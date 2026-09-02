package edu.seu.vcampus.server.thread;

import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.server.handler.RequestDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

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

    @Test
    void testClientThreadLifecycle() throws Exception {
        // 模拟 RequestDispatcher 返回固定响应
        RequestDispatcher mockDispatcher = new RequestDispatcher() {
            @Override
            public Message dispatch(Message request) {
                Message response = new Message();
                response.setStatusCode("200");
                return response;
            }
        };

        // 启动 ClientThread 任务
        ClientThread clientThread = new ClientThread(serverSideSocket, mockDispatcher);
        ThreadPoolManager.getInstance().execute(clientThread);

        // 客户端端按协议初始化流：先写 out 并 flush，再建 in
        ObjectOutputStream clientOut = new ObjectOutputStream(clientSocket.getOutputStream());
        clientOut.flush();
        ObjectInputStream clientIn = new ObjectInputStream(clientSocket.getInputStream());

        // 发送测试消息
        Message request = new Message();
        clientOut.writeObject(request);
        clientOut.flush();

        // 接收响应并断言
        Message response = (Message) clientIn.readObject();
        assertNotNull(response, "服务端返回的响应不应为 null");
        assertEquals("200", response.getStatusCode(), "响应状态码应为 200");

        // 清理资源
        clientSocket.close();
        serverSocket.close();
    }
}