package edu.seu.vcampus.common.network;

import edu.seu.vcampus.common.message.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * 封装单条 Socket 连接的对象输入流、对象输出流及消息收发。
 *
 * <p>客户端和服务端都先创建输出流并立即 flush，再创建输入流，避免双方同时
 * 等待对端序列化流头而死锁。
 */
public class MessageStream {

    /** 输出对象流（先创建）。 */
    private ObjectOutputStream out;

    /** 输入对象流（后创建）。 */
    private ObjectInputStream in;

    /** 底层 socket。 */
    private Socket socket;

    /**
     * 基于已建立的 socket 创建消息流。
     *
     * @param socket 已建立的连接
     * @throws IOException 创建对象流失败
     */
    public MessageStream(Socket socket) throws IOException {
        if (socket == null) {
            throw new IllegalArgumentException("socket must not be null");
        }
        this.socket = socket;
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.out.flush();
        this.in = new ObjectInputStream(socket.getInputStream());
    }

    /**
     * 从输入流读取一条消息。
     *
     * @return 收到的消息
     * @throws IOException 读取失败或对端断开
     * @throws ClassNotFoundException 反序列化失败
     */
    public Message recvMessage() throws IOException, ClassNotFoundException {
        return (Message) in.readObject();
    }

    /**
     * 向输出流写入一条消息并立即发送。
     *
     * @param message 要发送的消息
     * @throws IOException 写入失败
     */
    public synchronized void writeMessage(Message message) throws IOException {
        out.reset();
        out.writeObject(message);
        out.flush();
    }

    /**
     * 关闭消息流与底层 socket。
     *
     * @throws IOException 关闭失败
     */
    public void close() throws IOException {
        in.close();
        out.close();
        socket.close();
    }
}
