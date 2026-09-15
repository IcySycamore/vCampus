package edu.seu.vcampus.server.network;

import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.message.MessageSender;
import edu.seu.vcampus.server.thread.ThreadPoolManager;
import edu.seu.vcampus.server.user.SessionManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

class ServerMessageReceiverThreadTest {

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
        ServerMessageReceiverThread.getDispatcher().register(Command.USER_LOGIN,
                new edu.seu.vcampus.common.message.MessageHandler() {
                    @Override
                    public void handle(Message request, MessageSender sender) {
                        Message response = new Message(request.getCommand(), null);
                        response.setStatusCode("200");
                        sender.send(response);
                    }
                });

        // 启动每连接一个接收线程
        ServerMessageReceiverThread receiverThread = new ServerMessageReceiverThread(
                serverSideSocket, sessionManager);
        ThreadPoolManager.getInstance().execute(receiverThread);

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

    /**
     * 关键回归：慢处理器执行期间，心跳仍要立刻得到回应。
     *
     * <p>
     * 修复前的行为是：handler 在读循环里就地执行，一个 3 秒的请求会把读循环卡住，心跳堆在
     * TCP 缓冲区里没人处理，客户端迟迟等不到确认，误判为断连并重连。修复后业务跑在独立线程池，
     * 读循环立刻回到 recvMessage，心跳随到随回。
     *
     * <p>
     * 阈值取 1.5 秒：慢处理器要跑 3 秒，只要心跳没被它挡住，实际耗时必然是毫秒级；
     * 若回归成同步执行，耗时会在 3 秒左右，断言会立刻失败。
     *
     * @throws Exception 通信失败
     */
    @Test
    void heartbeatIsAnsweredWhileSlowHandlerRuns() throws Exception {
        ServerMessageReceiverThread.getDispatcher().register(Command.USER_LOGIN_VERIFY,
                new edu.seu.vcampus.common.message.MessageHandler() {
                    @Override
                    public void handle(Message request, MessageSender sender) {
                        try {
                            Thread.sleep(3000L);// 模拟耗时业务：全量查询 + 逐条联查
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        Message response = new Message(request.getCommand(), "慢请求完成");
                        response.setStatusCode(StatusCode.SUCCESS);
                        sender.send(response);
                    }
                });

        ServerMessageReceiverThread receiverThread = new ServerMessageReceiverThread(
                serverSideSocket, new SessionManager());
        new Thread(receiverThread, "test-receiver").start();

        ObjectOutputStream clientOut = new ObjectOutputStream(clientSocket.getOutputStream());
        clientOut.flush();
        ObjectInputStream clientIn = new ObjectInputStream(clientSocket.getInputStream());

        // 先发一个会跑 3 秒的请求，并留出时间让读线程把它丢进业务池
        clientOut.writeObject(new Message(Command.USER_LOGIN_VERIFY, null));
        clientOut.flush();
        Thread.sleep(300L);

        // 紧接着发心跳：它应当被立刻回应，而不是排在慢请求后面
        long started = System.currentTimeMillis();
        clientOut.writeObject(new Message(Command.HEARTBEAT, null));
        clientOut.flush();
        Message ack = (Message) clientIn.readObject();
        long elapsed = System.currentTimeMillis() - started;

        assertEquals(Command.HEARTBEAT, ack.getCommand(), "先收到的应当是心跳确认");
        assertEquals(StatusCode.SUCCESS, ack.getStatusCode());
        assertTrue(elapsed < 1500L,
                "心跳应在慢处理器执行期间就被回应，实际耗时 " + elapsed + "ms");

        // 慢请求最终也要正常完成，说明异步化没有把它的响应弄丢
        Message slow = (Message) clientIn.readObject();
        assertEquals("慢请求完成", slow.getData());
    }
}