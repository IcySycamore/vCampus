package edu.seu.vcampus.server.thread;

import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.handler.MessageSender;
import edu.seu.vcampus.server.auth.SessionManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

class ClientThreadTest {

    private ServerSocket serverSocket;
    private Socket clientSocket;
    private Socket serverSideSocket;

    @BeforeEach
    void setUp() throws Exception {
        // 创建本地随机端口 Socket 连接模拟客户端和服务端通信
        serverSocket = new ServerSocket(0);
        int port = serverSocket.getLocalPort();

        clientSocket = new Socket("localhost", port);
        serverSideSocket = serverSocket.accept();
    }

    @AfterEach
    void tearDown() {
        // 兜底清理 Socket 资源，防止测试失败导致端口泄漏
        try {
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
            if (serverSideSocket != null && !serverSideSocket.isClosed()) {
                serverSideSocket.close();
            }
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void testClientThreadLifecycle() throws Exception {
        SessionManager sessionManager = new SessionManager();
        ClientThread.getDispatcher().register(Command.USER_LOGIN,
                new edu.seu.vcampus.common.handler.MessageHandler() {
                    @Override
                    public void handle(Message request, MessageSender sender) {
                        Message response = new Message(
                                request.getCommand(), null);
                        response.setStatusCode("200");
                        sender.send(response);
                    }
                });

        // 启动 ClientThread 任务
        ClientThread clientThread = new ClientThread(
                serverSideSocket, sessionManager);
        ThreadPoolManager.getInstance().execute(clientThread);

        // 客户端按协议初始化流：先写 out 并 flush，再建 in
        ObjectOutputStream clientOut = new ObjectOutputStream(clientSocket.getOutputStream());
        clientOut.flush();
        ObjectInputStream clientIn = new ObjectInputStream(clientSocket.getInputStream());

        // 发送测试消息
        Message request = new Message(Command.USER_LOGIN, null);
        clientOut.writeObject(request);
        clientOut.flush();

        // 接收响应并断言
        Message response = (Message) clientIn.readObject();
        assertNotNull(response, "服务端返回的响应不应为 null");
        assertEquals("200", response.getStatusCode(), "响应状态码应为 200");
    }
}