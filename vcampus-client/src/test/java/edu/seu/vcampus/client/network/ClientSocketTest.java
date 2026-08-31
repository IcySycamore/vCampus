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
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 客户端对象流网络测试。
 */
class ClientSocketTest {

    @Test
    void sendsAndReceivesMessage() throws Exception {
        ServerSocket server = new ServerSocket(0);
        CountDownLatch received = new CountDownLatch(1);
        CountDownLatch releasePeer = new CountDownLatch(1);
        RecordingHandler handler = new RecordingHandler(received);
        AtomicReference<Exception> peerFailure = new AtomicReference<Exception>();
        Thread peer = startEchoPeer(server, peerFailure, releasePeer);
        ClientSocket client = new ClientSocket("127.0.0.1", server.getLocalPort(), handler);

        boolean handled;
        try {
            client.connect();
            client.send(new Message(400, "Java"));
            handled = received.await(10, TimeUnit.SECONDS);
        } finally {
            try {
                client.close();
            } finally {
                releasePeer.countDown();
                server.close();
                peer.join(10000L);
            }
        }
        assertNull(peerFailure.get(), "echo peer failed");
        assertFalse(peer.isAlive(), "echo peer did not stop");
        assertTrue(handled, "client did not receive the echoed message");
        assertEquals(400, handler.message.getCommand());
        assertNotNull(handler.message.getUid());
    }

    private Thread startEchoPeer(final ServerSocket server,
            final AtomicReference<Exception> peerFailure,
            final CountDownLatch releasePeer) {
        Thread peer = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Socket socket = server.accept();
                    try {
                        ObjectOutputStream output =
                                new ObjectOutputStream(socket.getOutputStream());
                        output.flush();
                        ObjectInputStream input =
                                new ObjectInputStream(socket.getInputStream());
                        output.writeObject(input.readObject());
                        output.flush();
                        releasePeer.await(10, TimeUnit.SECONDS);
                    } finally {
                        socket.close();
                    }
                } catch (Exception exception) {
                    peerFailure.set(exception);
                }
            }
        });
        peer.start();
        return peer;
    }

    private static final class RecordingHandler implements UIUpdateHandler {

        private final CountDownLatch received;
        private volatile Message message;

        private RecordingHandler(CountDownLatch received) {
            this.received = received;
        }

        @Override
        public void handleMessage(Message value) {
            message = value;
            received.countDown();
        }

        @Override
        public void connectionClosed(Exception cause) {
        }
    }
}
