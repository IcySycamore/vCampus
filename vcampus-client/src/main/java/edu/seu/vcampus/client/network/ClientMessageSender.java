package edu.seu.vcampus.client.network;

import edu.seu.vcampus.common.message.MessageSender;
import edu.seu.vcampus.common.message.Message;

import java.io.IOException;

/**
 * 基于 {@link ClientSocketListener} 的发送通道：与 server 的 {@code ServerMessageSender}
 * 对称， 把「发送能力」交给分发器与业务处理器使用。
 *
 * <p>
 * {@link MessageSender} 契约约定不抛受检异常，因此底层 IO 异常（连接已断开等）在此消化； 连接生命周期（重连、关闭）由
 * {@link ClientSocketListener} 自行处理。
 */
public class ClientMessageSender implements MessageSender {

    /** 底层连接。 */
    private final ClientSocketListener m_socket;

    /**
     * 构造发送通道。
     *
     * @param socket 客户端连接
     * @throws IllegalArgumentException 连接为 null
     */
    public ClientMessageSender(ClientSocketListener socket) {
        if (socket == null) {
            throw new IllegalArgumentException("socket must not be null");
        }
        m_socket = socket;
    }

    /**
     * 发送一条消息；底层 IO 异常（连接断开等）在此捕获。
     *
     * @param message 待发消息
     * @throws IllegalArgumentException 消息为 null
     */
    @Override
    public void send(Message message) {
        if (message == null) {
            throw new IllegalArgumentException("message must not be null");
        }
        try {
            m_socket.send(message);
        } catch (IOException e) {
            System.err.println("发送消息失败: " + e.getMessage());
        }
    }
}
