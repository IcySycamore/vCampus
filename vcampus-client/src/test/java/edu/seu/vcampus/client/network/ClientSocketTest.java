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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 客户端对象流网络测试。
 */
class ClientSocketTest {

    @Test
    void sendsAndReceivesMessage() throws Exception {
        ServerSocket server = new ServerSocket(0);
        CountDownLatch received = new CountDownLatch(1);
        RecordingHandler handler = new RecordingHandler(received);
        Thread peer = startEchoPeer(server);
        ClientSocket client = new ClientSocket("127.0.0.1", server.getLocalPort(), handler);

        client.connect();
        client.send(new Message(400, "Java"));

        assertTrue(received.await(3, TimeUnit.SECONDS));
        assertEquals(400, handler.message.getCommand());
        assertNotNull(handler.message.getUid());
        client.close();
        server.close();
        peer.join(3000L);
    }

    private Thread startEchoPeer(final ServerSocket server) {
        Thread peer = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Socket socket = server.accept();
                    ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
                    output.flush();
                    ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
                    output.writeObject(input.readObject());
                    output.flush();
                    socket.close();
                } catch (Exception exception) {
                    throw new IllegalStateException(exception);
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
