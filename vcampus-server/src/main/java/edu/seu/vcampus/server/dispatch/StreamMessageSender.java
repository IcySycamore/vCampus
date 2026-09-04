package edu.seu.vcampus.server.dispatch;

import edu.seu.vcampus.common.handler.MessageSender;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.server.network.MessageStream;

import java.io.IOException;

/**
 * 基于 {@link MessageStream} 的响应发送器：把「发送能力」交给业务模块。
 *
 * <p>工作线程为每条连接创建一个本类实例，随请求一起传给 handler；
 * handler 调用 {@link #send} 时，底层通过 MessageStream 把响应写回客户端。
 */
public class StreamMessageSender implements MessageSender {

    /** 底层消息流（负责实际写回）。 */
    private final MessageStream stream;

    /**
     * 构造发送器。
     *
     * @param stream 消息流（不可为 null）
     */
    public StreamMessageSender(MessageStream stream) {
        if (stream == null) {
            throw new IllegalArgumentException("stream must not be null");
        }
        this.stream = stream;
    }

    /**
     * 发送响应消息；底层 IO 异常（连接已断开等）在此捕获，线程池接入后触发连接清理。
     *
     * @param response 响应消息
     */
    @Override
    public void send(Message response) {
        try {
            stream.writeMessage(response);
        } catch (IOException e) {
            // 连接已断开导致发送失败；线程池接入后在此触发连接清理
            System.err.println("发送响应失败: " + e.getMessage());
        }
    }
}
