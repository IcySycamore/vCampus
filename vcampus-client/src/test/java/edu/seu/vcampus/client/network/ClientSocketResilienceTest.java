package edu.seu.vcampus.client.network;
import edu.seu.vcampus.client.handler.UIUpdateHandler;
import edu.seu.vcampus.common.message.Message;
import org.junit.jupiter.api.Test;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
/** 客户端重试、超时与优雅关闭测试。 */
class ClientSocketResilienceTest {
    private static final ClientNetworkConfig FAST_CONFIG =
            new ClientNetworkConfig(500, 300, 3, 20L, 80L, 500L);

    @Test
    void retriesHandshakeWithExponentialBackoff() throws Exception {
        ServerSocket server = new ServerSocket(0);
        AtomicInteger accepted = new AtomicInteger();
        AtomicReference<Exception> failure = new AtomicReference<Exception>();
        Thread peer = startRetryPeer(server, accepted, failure);
        ClientSocket client = new ClientSocket("127.0.0.1", server.getLocalPort(),
                new NoOpHandler(), FAST_CONFIG);
        try {
            client.connect();
            assertTrue(client.isConnected());
            assertEquals(3, accepted.get());
        } finally {
            client.close();
            server.close();
            peer.join(2000L);
        }
        assertNull(failure.get());
    }

    @Test
    void readTimeoutReportsFailureAndReconnects() throws Exception {
        ServerSocket server = new ServerSocket(0);
        CountDownLatch disconnected = new CountDownLatch(1);
        CountDownLatch reconnected = new CountDownLatch(1);
        AtomicReference<Exception> failure = new AtomicReference<Exception>();
        Thread peer = startTimeoutPeer(server, reconnected, failure);
        ClientSocket client = new ClientSocket("127.0.0.1", server.getLocalPort(),
                new LatchHandler(disconnected, new CountDownLatch(1)), FAST_CONFIG);
        try {
            client.connect();
            assertTrue(disconnected.await(2, TimeUnit.SECONDS));
            assertTrue(reconnected.await(2, TimeUnit.SECONDS));
            assertTrue(waitUntilConnected(client));
        } finally {
            client.close();
            server.close();
            peer.join(2000L);
        }
        assertNull(failure.get());
    }

    @Test
    void gracefulCloseDeliversInflightMessage() throws Exception {
        ServerSocket server = new ServerSocket(0);
        CountDownLatch received = new CountDownLatch(1);
        AtomicReference<Exception> failure = new AtomicReference<Exception>();
        Thread peer = startFinalMessagePeer(server, failure);
        ClientSocket client = new ClientSocket("127.0.0.1", server.getLocalPort(),
                new LatchHandler(new CountDownLatch(1), received), FAST_CONFIG);
        client.connect();
        client.close();
        server.close();
        peer.join(2000L);

        assertTrue(received.await(1, TimeUnit.SECONDS));
        assertFalse(client.isConnected());
        assertNull(failure.get());
    }

    private Thread startRetryPeer(final ServerSocket server, final AtomicInteger accepted,
            final AtomicReference<Exception> failure) {
        return startPeer(new PeerAction() {
            @Override
            public void run() throws Exception {
                for (int index = 0; index < 3; index++) {
                    Socket socket = server.accept();
                    accepted.incrementAndGet();
                    if (index < 2) {
                        socket.close();
                    } else {
                        openStreamsAndWait(socket, 1000L);
                    }
                }
            }
        }, failure);
    }

    private Thread startTimeoutPeer(final ServerSocket server,
            final CountDownLatch reconnected, final AtomicReference<Exception> failure) {
        return startPeer(new PeerAction() {
            @Override
            public void run() throws Exception {
                Socket first = server.accept();
                openStreamsAndWait(first, 500L);
                Socket second = server.accept();
                reconnected.countDown();
                openStreamsAndWait(second, 1000L);
            }
        }, failure);
    }

    private Thread startFinalMessagePeer(final ServerSocket server,
            final AtomicReference<Exception> failure) {
        return startPeer(new PeerAction() {
            @Override
            public void run() throws Exception {
                Socket socket = server.accept();
                ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
                output.flush();
                new ObjectInputStream(socket.getInputStream());
                output.writeObject(new Message(499, "final"));
                output.flush();
                socket.close();
            }
        }, failure);
    }

    private Thread startPeer(final PeerAction action,
            final AtomicReference<Exception> failure) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    action.run();
                } catch (Exception exception) {
                    failure.set(exception);
                }
            }
        });
        thread.start();
        return thread;
    }

    private void openStreamsAndWait(Socket socket, long waitMillis) throws Exception {
        ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
        output.flush();
        new ObjectInputStream(socket.getInputStream());
        Thread.sleep(waitMillis);
        socket.close();
    }

    private boolean waitUntilConnected(ClientSocket client) throws InterruptedException {
        for (int index = 0; index < 20; index++) {
            if (client.isConnected()) {
                return true;
            }
            Thread.sleep(25L);
        }
        return false;
    }

    private interface PeerAction {
        void run() throws Exception;
    }

    private static final class NoOpHandler implements UIUpdateHandler {
        @Override
        public void handleMessage(Message message) {
        }

        @Override
        public void connectionClosed(Exception cause) {
        }
    }

    private static final class LatchHandler implements UIUpdateHandler {
        private final CountDownLatch disconnected;
        private final CountDownLatch received;

        LatchHandler(CountDownLatch disconnected, CountDownLatch received) {
            this.disconnected = disconnected;
            this.received = received;
        }

        @Override
        public void handleMessage(Message message) {
            received.countDown();
        }

        @Override
        public void connectionClosed(Exception cause) {
            disconnected.countDown();
        }
    }
}
