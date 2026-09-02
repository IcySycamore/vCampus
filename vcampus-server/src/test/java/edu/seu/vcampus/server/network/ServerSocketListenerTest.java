package edu.seu.vcampus.server.network;

import edu.seu.vcampus.common.message.Message;
import org.junit.jupiter.api.Test;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ServerSocketListener 监听与接客测试：验证端口绑定、accept 连接并返回可用的 MessageStream。
 */
class ServerSocketListenerTest {

    /**
     * start 后能接受连接，返回的 MessageStream 可正常收发消息。
     *
     * @throws Exception 网络或序列化异常
     */
    @Test
    void acceptReturnsWorkingStream() throws Exception {
        ServerSocketListener listener = new ServerSocketListener();
        listener.start(0);
        final int port = listener.getPort();
        assertTrue(port > 0);

        final Message[] fromServer = new Message[1];
        final Exception[] clientError = new Exception[1];

        Thread client = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Socket sock = new Socket("127.0.0.1", port);
                    // 对端按客户端顺序初始化：先输入流，再输出流
                    ObjectInputStream cin = new ObjectInputStream(sock.getInputStream());
                    ObjectOutputStream cout = new ObjectOutputStream(sock.getOutputStream());
                    cout.flush();

                    fromServer[0] = (Message) cin.readObject();

                    cout.writeObject(new Message(202, "world"));
                    cout.flush();
                    sock.close();
                } catch (Exception e) {
                    clientError[0] = e;
                }
            }
        });
        client.start();

        MessageStream stream = listener.accept();
        stream.writeMessage(new Message(201, "hello"));

        Message reply = stream.recvMessage();

        stream.close();
        listener.stop();
        client.join();

        if (clientError[0] != null) {
            throw clientError[0];
        }

        assertEquals(201, fromServer[0].getCommand());
        assertEquals("hello", fromServer[0].getData());
        assertEquals(202, reply.getCommand());
        assertEquals("world", reply.getData());
    }
}
