package edu.seu.vcampus.server.network;

import edu.seu.vcampus.common.message.Message;
import org.junit.jupiter.api.Test;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * MessageStream 收发往返测试：验证对象流初始化顺序正确（不死锁）、Message 序列化往返一致。
 */
class MessageStreamTest {

    /**
     * 服务端 MessageStream 与对端对象流之间收发消息，字段应一致。
     *
     * @throws Exception 网络或序列化异常
     */
    @Test
    void roundTrip() throws Exception {
        ServerSocket server = new ServerSocket(0);
        final int port = server.getLocalPort();

        final Message[] fromServer = new Message[1];
        final Exception[] clientError = new Exception[1];

        Thread client = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Socket sock = new Socket("127.0.0.1", port);
                    // 对端按客户端顺序初始化：先输入流，再输出流（与服务端相反）
                    ObjectInputStream cin = new ObjectInputStream(sock.getInputStream());
                    ObjectOutputStream cout = new ObjectOutputStream(sock.getOutputStream());
                    cout.flush();

                    fromServer[0] = (Message) cin.readObject();

                    cout.writeObject(new Message(102, "world"));
                    cout.flush();
                    sock.close();
                } catch (Exception e) {
                    clientError[0] = e;
                }
            }
        });
        client.start();

        Socket serverSocket = server.accept();
        MessageStream stream = new MessageStream(serverSocket);

        stream.writeMessage(new Message(101, "hello"));

        Message reply = stream.recvMessage();

        stream.close();
        server.close();
        client.join();

        if (clientError[0] != null) {
            throw clientError[0];
        }

        assertEquals(101, fromServer[0].getCommand());
        assertEquals("hello", fromServer[0].getData());
        assertEquals(102, reply.getCommand());
        assertEquals("world", reply.getData());
    }
}
