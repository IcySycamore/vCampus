package edu.seu.vcampus.client.network;

import edu.seu.vcampus.client.handler.UIUpdateHandler;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.network.MessageStream;

import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

/** 客户端 Socket 连接，负责限时连接、指数退避重连、消息收发和优雅关闭。 */
public class ClientSocketListener implements Closeable {
    private final UIUpdateHandler handler;
    private final ClientNetworkConfig config;
    private final ClientConnectionFactory connectionFactory;
    private Socket socket;
    private MessageStream stream;
    private ClientMessageReceiverThread receiver;
    private Thread receiverThread;
    private Thread reconnectThread;
    private ClientHeartbeat heartbeat;
    private volatile boolean connected;
    private volatile boolean shutdownRequested;
    private long connectionGeneration;

    /**
     * 使用默认超时和重试策略创建客户端。
     * 
     * @param host 服务器端地址
     * @param port 服务器端端口
     * @param handler 网络事件处理器
     */
    public ClientSocketListener(String host, int port, UIUpdateHandler handler) {
        this(host, port, handler, ClientNetworkConfig.defaults());
    }

    /**
     * 使用指定网络参数创建客户端。
     * 
     * @param host 服务器端地址
     * @param port 服务器端端口
     * @param handler 网络事件处理器
     * @param config 超时、重试和关闭参数
     */
    public ClientSocketListener(String host, int port, UIUpdateHandler handler,
            ClientNetworkConfig config) {
        if (host == null || host.trim().length() == 0 || handler == null || config == null) {
            throw new IllegalArgumentException("host, handler and config must not be empty");
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("port is out of range");
        }
        this.handler = handler;
        this.config = config;
        this.connectionFactory = new ClientConnectionFactory(host, port, config);
    }

    /**
     * 建立连接；失败时按照配置执行有限次数的指数退避重试。
     * 
     * @throws IOException 重试耗尽或客户端已经关闭
     */
    public void connect() throws IOException {
        synchronized (this) {
            if (connected) {
                return;
            }
            if (shutdownRequested) {
                throw new SocketException("client is closed");
            }
        }
        install(connectionFactory.openWithRetry());
    }

    /**
     * 向服务器端发送消息。调用方必须已分配序列号（uid 由分发器统一发号）。
     *
     * @param message 待发送消息
     * @throws IOException 连接不可用或写入失败
     * @throws IllegalArgumentException 消息或其 uid 为 null
     */
    public synchronized void send(Message message) throws IOException {
        if (!connected) {
            throw new IOException("client is not connected");
        }
        if (message == null) {
            throw new IllegalArgumentException("message must not be null");
        }
        if (message.getUid() == null) {
            throw new IllegalArgumentException("message uid must be assigned before send");
        }
        stream.writeMessage(message);
    }

    /** @return 当前是否保持连接 */
    public synchronized boolean isConnected() {
        return connected;
    }

    private synchronized void install(ClientConnectionFactory.Connection connection)
            throws IOException {
        if (shutdownRequested || connected) {
            ClientConnectionFactory.closeQuietly(connection.socket);
            if (shutdownRequested) {
                throw new SocketException("client is closed");
            }
            return;
        }
        socket = connection.socket;
        stream = connection.stream;
        connected = true;
        final long generation = ++connectionGeneration;
        receiver = new ClientMessageReceiverThread(connection.stream, this, generation);
        receiverThread = new Thread(receiver, "vcampus-message-receiver");
        receiverThread.setDaemon(true);
        heartbeat = ClientHeartbeat.start(this, connection.stream, generation,
                config.getHeartbeatIntervalMillis());
        receiverThread.start();
    }

    void handleReceived(long generation, Message message) {
        synchronized (this) {
            if (generation != connectionGeneration) {
                return;
            }
        }
        handler.handleMessage(message);
    }

    void handleConnectionClosed(long generation, Exception cause) {
        Socket closedSocket;
        ClientHeartbeat stoppedHeartbeat;
        synchronized (this) {
            if (generation != connectionGeneration || !connected) {
                return;
            }
            connected = false;
            closedSocket = socket;
            stoppedHeartbeat = heartbeat;
            socket = null;
            stream = null;
            receiver = null;
            receiverThread = null;
            heartbeat = null;
        }
        ClientHeartbeat.stop(stoppedHeartbeat);
        ClientConnectionFactory.closeQuietly(closedSocket);
        if (!shutdownRequested) {
            try {
                handler.connectionClosed(cause);
            } finally {
                startReconnect(generation);
            }
        }
    }

    private synchronized void startReconnect(final long generation) {
        if (shutdownRequested || reconnectThread != null) {
            return;
        }
        reconnectThread = ClientReconnectTask.create(this, connectionFactory, generation);
        reconnectThread.start();
    }

    synchronized void installReconnect(ClientConnectionFactory.Connection connection,
            long generation) throws IOException {
        if (generation != connectionGeneration || shutdownRequested) {
            ClientConnectionFactory.closeQuietly(connection.socket);
            return;
        }
        install(connection);
    }

    synchronized void reconnectFinished(Thread completedThread) {
        if (reconnectThread == completedThread) {
            reconnectThread = null;
        }
    }

    /** 停止发送并等待在途响应；宽限期结束后强制关闭，未读取消息会被丢弃。 */
    @Override
    public void close() throws IOException {
        Socket closingSocket;
        ClientMessageReceiverThread closingReceiver;
        Thread closingReceiverThread;
        Thread closingReconnectThread;
        ClientHeartbeat closingHeartbeat;
        synchronized (this) {
            shutdownRequested = true;
            connectionFactory.stop();
            connected = false;
            closingSocket = socket;
            closingReceiver = receiver;
            closingReceiverThread = receiverThread;
            closingReconnectThread = reconnectThread;
            closingHeartbeat = heartbeat;
            heartbeat = null;
        }
        ClientHeartbeat.stop(closingHeartbeat);
        try {
            ClientShutdown.close(closingSocket, closingReceiver, closingReceiverThread,
                    closingReconnectThread, config.getShutdownGraceMillis());
        } finally {
            clearConnection();
        }
    }

    private synchronized void clearConnection() {
        connectionGeneration++;
        socket = null;
        stream = null;
        receiver = null;
        receiverThread = null;
        reconnectThread = null;
        heartbeat = null;
    }
}
