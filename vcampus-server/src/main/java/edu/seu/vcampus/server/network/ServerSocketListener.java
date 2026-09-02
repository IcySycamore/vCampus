package edu.seu.vcampus.server.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * 服务端 Socket 监听器：绑定端口、接受连接，并为每个连接创建 {@link MessageStream}
 * （见 docs/应用层协议规定.md §6.1）。
 *
 * <p>本类只负责「监听 + 接客」。accept 到连接后创建消息流，交由上层（线程池）驱动收发循环。
 */
public class ServerSocketListener {

    /** 协议 v1 约定的默认监听端口。 */
    public static final int DEFAULT_PORT = 8888;

    /** 底层监听 socket。 */
    private ServerSocket serverSocket;

    /** 是否正在运行。 */
    private volatile boolean running;

    /**
     * 绑定端口并启动监听。
     *
     * @param port 监听端口（协议 v1 约定 8888）
     * @throws IOException 绑定端口失败
     */
    public void start(int port) throws IOException {
        this.serverSocket = new ServerSocket(port);
        this.running = true;
    }

    /**
     * 阻塞接受一个客户端连接，并为其创建消息流。
     *
     * @return 该连接对应的 MessageStream
     * @throws IOException 接受连接或创建消息流失败
     */
    public MessageStream accept() throws IOException {
        Socket socket = serverSocket.accept();
        return new MessageStream(socket);
    }

    /**
     * 关闭监听。阻塞中的 accept 会因 socket 关闭而抛异常退出。
     *
     * @throws IOException 关闭失败
     */
    public void stop() throws IOException {
        this.running = false;
        if (serverSocket != null) {
            serverSocket.close();
        }
    }

    /**
     * 返回实际绑定的端口（端口传 0 时可拿到系统分配的随机端口，便于测试）。
     *
     * @return 监听端口
     */
    public int getPort() {
        return serverSocket.getLocalPort();
    }

    /**
     * 返回是否正在运行。
     *
     * @return 是否运行中
     */
    public boolean isRunning() {
        return running;
    }
}
