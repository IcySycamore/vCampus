package edu.seu.vcampus.client.network;

import edu.seu.vcampus.client.handler.UIUpdateHandler;
import edu.seu.vcampus.common.message.Message;

import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 客户端 Socket 连接，负责对象流建立、消息发送和后台接收。
 */
public class ClientSocket implements Closeable {

    private static final AtomicLong MESSAGE_IDS = new AtomicLong();
    private final String host;
    private final int port;
    private final UIUpdateHandler handler;
    private Socket socket;
    private ObjectOutputStream output;
    private MessageReceiver receiver;
    private Thread receiverThread;

    /**
     * 创建尚未连接的客户端。
     *
     * @param host 服务器端地址
     * @param port 服务器端端口
     * @param handler 网络事件处理器
     */
    public ClientSocket(String host, int port, UIUpdateHandler handler) {
        if (host == null || host.trim().length() == 0 || handler == null) {
            throw new IllegalArgumentException("host and handler must not be empty");
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("port is out of range");
        }
        this.host = host;
        this.port = port;
        this.handler = handler;
    }

    /**
     * 建立连接并启动后台消息接收线程。
     *
     * @throws IOException 建立连接或对象流失败
     */
    public synchronized void connect() throws IOException {
        if (isConnected()) {
            return;
        }
        Socket newSocket = new Socket(host, port);
        try {
            ObjectOutputStream newOutput = new ObjectOutputStream(newSocket.getOutputStream());
            newOutput.flush();
            ObjectInputStream input = new ObjectInputStream(newSocket.getInputStream());
            socket = newSocket;
            output = newOutput;
            receiver = new MessageReceiver(input, handler);
            receiverThread = new Thread(receiver, "vcampus-message-receiver");
            receiverThread.setDaemon(true);
            receiverThread.start();
        } catch (IOException exception) {
            newSocket.close();
            throw exception;
        }
    }

    /**
     * 向服务器端发送消息。未设置 uid 时自动生成。
     *
     * @param message 待发送消息
     * @throws IOException 连接不可用或写入失败
     */
    public synchronized void send(Message message) throws IOException {
        if (!isConnected()) {
            throw new IOException("client is not connected");
        }
        if (message == null) {
            throw new IllegalArgumentException("message must not be null");
        }
        if (message.getUid() == null) {
            message.setUid(MESSAGE_IDS.incrementAndGet());
        }
        output.writeObject(message);
        output.flush();
        output.reset();
    }

    /** @return 当前是否保持连接 */
    public synchronized boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    @Override
    public synchronized void close() throws IOException {
        if (receiver != null) {
            receiver.stop();
        }
        if (socket != null) {
            socket.close();
        }
        socket = null;
        output = null;
    }
}
