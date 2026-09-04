package edu.seu.vcampus.server.dispatch;

import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.server.network.MessageStream;
import org.junit.jupiter.api.Test;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * StreamMessageSender 测试：验证 send 通过 MessageStream 把响应发送给对端。
 */
class StreamMessageSenderTest {

    /**
     * send 应把响应消息写入底层 MessageStream，对端能收到完整内容。
     *
     * @throws Exception 网络或序列化异常
     */
    @Test
    void sendWritesResponseThroughStream() throws Exception {
        ServerSocket server = new ServerSocket(0);
        final int port = server.getLocalPort();

        final Message[] received = new Message[1];
        final Exception[] clientError = new Exception[1];

        Thread client = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Socket sock = new Socket("127.0.0.1", port);
                    // 按协议 §6.2：先建输出流并 flush，再建输入流（与服务端一致）
                    ObjectOutputStream cout = new ObjectOutputStream(sock.getOutputStream());
                    cout.flush();
                    ObjectInputStream cin = new ObjectInputStream(sock.getInputStream());
                    received[0] = (Message) cin.readObject();
                    sock.close();
                } catch (Exception e) {
                    clientError[0] = e;
                }
            }
        });
        client.start();

        Socket serverSocket = server.accept();
        MessageStream stream = new MessageStream(serverSocket);
        StreamMessageSender sender = new StreamMessageSender(stream);

        Message response = new Message(201, "handled");
        response.setStatusCode("200");
        sender.send(response);

        stream.close();
        server.close();
        client.join();

        if (clientError[0] != null) {
            throw clientError[0];
        }

        assertEquals(201, received[0].getCommand());
        assertEquals("handled", received[0].getData());
    }
}
