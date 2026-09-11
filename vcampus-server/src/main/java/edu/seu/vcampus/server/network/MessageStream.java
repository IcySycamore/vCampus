package edu.seu.vcampus.server.network;


import edu.seu.vcampus.common.message.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * 服务端消息流：封装对象流的创建与 {@link Message} 的收发（见 docs/应用层协议规定.md §6.2）。
 *
 * <p>服务端先创建输出流并立即 flush、再创建输入流，与客户端一致；两端都先建输出流，
 * 避免两端都先建输入流导致的死锁。
 */
public class MessageStream {

    /** 输出对象流（先创建）。 */
    private ObjectOutputStream out;

    /** 输入对象流（后创建）。 */
    private ObjectInputStream in;

    /** 底层 socket。 */
    private Socket socket;

    /**
     * 基于已建立的 socket 创建消息流，按协议 §6.2 的顺序（先输出流后输入流）初始化对象流。
     *
     * <p>先建输出流后立即 flush，把序列化流头发送出去，再建输入流；否则对端建输入流时
     * 会因读不到流头而阻塞，造成死锁。
     *
     * @param socket 已建立的连接
     * @throws IOException 创建对象流失败
     */
    public MessageStream(Socket socket) throws IOException {
        this.socket = socket;
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.out.flush();
        this.in = new ObjectInputStream(socket.getInputStream());
    }

    /**
     * 从输入流读取一条消息。
     *
     * @return 收到的 Message
     * @throws IOException            读取失败或对端断开
     * @throws ClassNotFoundException 反序列化失败
     */
    public Message recvMessage() throws IOException, ClassNotFoundException {
        return (Message) in.readObject();
    }

    /**
     * 向输出流写入一条消息并立即发送。
     *
     * <p>写入前先 {@code reset()} 清空对象引用缓存：否则同一条连接上第二次发送
     * <b>同一个可变对象</b>（如 DAO 中的学籍记录被修改后再次查询返回）时，序列化
     * 只写引用标记（back-reference）而不重写字段，对端会看到第一次的旧值。
     * 每条消息独立完整序列化，语义上也更符合「一请求一响应」。
     *
     * @param msg 要发送的 Message
     * @throws IOException 写入失败
     */
    public void writeMessage(Message msg) throws IOException {
        out.reset();
        out.writeObject(msg);
        out.flush();
    }

    /**
     * 返回底层 socket，供上层设置收发超时等连接参数。
     *
     * <p>流头已在构造时读走，因此持有 Socket 的一方不应再新建 MessageStream。
     *
     * @return 底层 socket
     */
    public Socket getSocket() {
        return socket;
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
