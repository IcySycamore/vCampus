package edu.seu.vcampus.server.thread;

import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.server.handler.RequestDispatcher;
import edu.seu.vcampus.server.network.MessageStream;

import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 客户端处理线程：负责单个客户端 Socket 连接的生命周期管理与消息收发循环。
 *
 * <p>每个连接对应一个 {@link ClientThread} 任务，被提交至 {@link ThreadPoolManager} 执行。
 * 线程不断从 {@link MessageStream} 读取请求消息，交由 {@link RequestDispatcher} 处理，
 * 并将响应结果写回客户端。当客户端断开连接或发生不可逆 IO 异常时，会自动清理 Socket 资源并平滑退出。
 */
public class ClientThread implements Runnable {

    private static final Logger logger = Logger.getLogger(ClientThread.class.getName());

    /** 底层客户端 Socket 连接。 */
    private final Socket socket;

    /** 请求分发器，用于处理业务逻辑。 */
    private final RequestDispatcher dispatcher;

    /** 运行状态标识。 */
    private volatile boolean running = true;

    /**
     * 基于已建立的 socket 与分发器创建客户端处理任务。
     *
     * @param socket     已建立的客户端套接字连接
     * @param dispatcher 请求分发器实例
     */
    public ClientThread(Socket socket, RequestDispatcher dispatcher) {
        this.socket = socket;
        this.dispatcher = dispatcher;
    }

    /**
     * 线程执行主循环：初始化消息流、循环读取并分发消息、隔离业务层异常及保证资源释放。
     */
    @Override
    public void run() {
        MessageStream messageStream = null;
        try {
            // 初始化消息流（按协议先 out 后 in 初始化对象流）
            messageStream = new MessageStream(socket);
            logger.info("客户端连接成功: " + socket.getRemoteSocketAddress());

            while (running && !socket.isClosed()) {
                Message request;
                try {
                    request = messageStream.recvMessage();
                } catch (EOFException | SocketException e) {
                    logger.info("客户端已断开连接: " + socket.getRemoteSocketAddress());
                    break;
                } catch (ClassNotFoundException e) {
                    logger.log(Level.SEVERE, "收到未知类型的消息，反序列化失败", e);
                    continue;
                }

                if (request == null) {
                    break;
                }

                // 捕获业务层异常，避免单个请求错误导致线程终止或 Socket 意外关闭
                try {
                    Message response = dispatcher.dispatch(request);
                    if (response != null) {
                        messageStream.writeMessage(response);
                    }
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "处理业务请求时发生异常: " + request, e);
                }
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "客户端通信流建立或传输发生 IO 异常", e);
        } finally {
            closeResource(messageStream);
        }
    }

    /**
     * 显式停止当前线程的处理循环。
     */
    public void stop() {
        this.running = false;
    }

    /**
     * 安全关闭消息流与底层 Socket 资源。
     *
     * @param messageStream 待关闭的消息流
     */
    private void closeResource(MessageStream messageStream) {
        if (messageStream != null) {
            try {
                messageStream.close();
            } catch (IOException e) {
                logger.log(Level.WARNING, "关闭 MessageStream 失败", e);
            }
        } else if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                logger.log(Level.WARNING, "关闭 Socket 失败", e);
            }
        }
        logger.info("客户端连接资源已释放: " + socket.getRemoteSocketAddress());
    }
}