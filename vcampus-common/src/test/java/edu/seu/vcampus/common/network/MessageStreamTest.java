package edu.seu.vcampus.common.network;

import edu.seu.vcampus.common.message.Message;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 通用消息流的 Socket 收发测试。
 */
class MessageStreamTest {

    /**
     * 双方按输出流优先的顺序初始化后，应能双向传输完整消息。
     *
     * @throws Exception 网络或序列化异常
     */
    @Test
    void sendsAndReceivesMessages() throws Exception {
        ServerSocket server = new ServerSocket(0);
        final int port = server.getLocalPort();
        final Message[] receivedByPeer = new Message[1];
        final Exception[] peerFailure = new Exception[1];

        Thread peer = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Socket socket = new Socket("127.0.0.1", port);
                    socket.setSoTimeout(3000);
                    ObjectOutputStream output =
                            new ObjectOutputStream(socket.getOutputStream());
                    output.flush();
                    ObjectInputStream input =
                            new ObjectInputStream(socket.getInputStream());
                    receivedByPeer[0] = (Message) input.readObject();
                    output.writeObject(new Message(402, "reply"));
                    output.flush();
                    socket.close();
                } catch (Exception exception) {
                    peerFailure[0] = exception;
                }
            }
        }, "message-stream-test-peer");
        peer.start();

        Socket accepted = server.accept();
        accepted.setSoTimeout(3000);
        MessageStream stream = new MessageStream(accepted);
        stream.writeMessage(new Message(401, "request"));
        Message reply = stream.recvMessage();
        stream.close();
        server.close();
        peer.join(3000);

        if (peerFailure[0] != null) {
            throw peerFailure[0];
        }
        assertEquals(401, receivedByPeer[0].getCommand());
        assertEquals("request", receivedByPeer[0].getData());
        assertEquals(402, reply.getCommand());
        assertEquals("reply", reply.getData());
    }
}
