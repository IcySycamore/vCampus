package edu.seu.vcampus.client.network;

import edu.seu.vcampus.client.handler.UIUpdateHandler;
import edu.seu.vcampus.common.message.Message;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 客户端心跳与服务端 ACK、断线重连的集成测试。 */
class ClientHeartbeatIntegrationTest {

    /** 首次连接与重连都应发心跳，并在网络层忽略 ACK。 */
    @Test
    void restartsHeartbeatAfterReconnectAndIgnoresAck() throws Exception {
        ServerSocket server = new ServerSocket(0);
        CountDownLatch disconnected = new CountDownLatch(1);
        CountDownLatch businessReceived = new CountDownLatch(1);
        CountDownLatch releasePeer = new CountDownLatch(1);
        AtomicInteger handled = new AtomicInteger();
        AtomicReference<Throwable> peerFailure = new AtomicReference<Throwable>();
        Thread peer = startPeer(server, releasePeer, peerFailure);
        ClientNetworkConfig config = new ClientNetworkConfig(
                500, 1000, 3, 20L, 80L, 200L, 30L);
        ClientSocket client = new ClientSocket("127.0.0.1", server.getLocalPort(),
                new RecordingHandler(disconnected, businessReceived, handled), config);

        try {
            client.connect();
            assertTrue(disconnected.await(2, TimeUnit.SECONDS));
            assertTrue(businessReceived.await(3, TimeUnit.SECONDS));
            assertTrue(client.isConnected());
        } finally {
            client.close();
            releasePeer.countDown();
            server.close();
            peer.join(3000L);
        }
        assertFalse(peer.isAlive());
        assertNull(peerFailure.get());
        assertEquals(1, handled.get());
    }

    private Thread startPeer(final ServerSocket server, final CountDownLatch release,
            final AtomicReference<Throwable> failure) {
        Thread peer = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Socket first = server.accept();
                    assertHeartbeat(openAndRead(first));
                    first.close();

                    Socket second = server.accept();
                    ObjectOutputStream output =
                            new ObjectOutputStream(second.getOutputStream());
                    output.flush();
                    ObjectInputStream input =
                            new ObjectInputStream(second.getInputStream());
                    assertHeartbeat((Message) input.readObject());
                    Message ack = new Message(1, "HEARTBEAT_ACK");
                    ack.setStatusCode("200");
                    output.writeObject(ack);
                    output.writeObject(new Message(400, "business"));
                    output.flush();
                    release.await(3, TimeUnit.SECONDS);
                    second.close();
                } catch (Throwable throwable) {
                    failure.set(throwable);
                }
            }
        }, "heartbeat-integration-peer");
        peer.start();
        return peer;
    }

    private Message openAndRead(Socket socket) throws Exception {
        ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
        output.flush();
        ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
        return (Message) input.readObject();
    }

    private void assertHeartbeat(Message heartbeat) {
        assertEquals(1, heartbeat.getCommand());
        assertNull(heartbeat.getData());
        assertNull(heartbeat.getToken());
    }

    private static final class RecordingHandler implements UIUpdateHandler {
        private final CountDownLatch disconnected;
        private final CountDownLatch received;
        private final AtomicInteger handled;

        RecordingHandler(CountDownLatch disconnected, CountDownLatch received,
                AtomicInteger handled) {
            this.disconnected = disconnected;
            this.received = received;
            this.handled = handled;
        }

        @Override
        public void handleMessage(Message message) {
            handled.incrementAndGet();
            received.countDown();
        }

        @Override
        public void connectionClosed(Exception cause) {
            disconnected.countDown();
        }
    }
}
