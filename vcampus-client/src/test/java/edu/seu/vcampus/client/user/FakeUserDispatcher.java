package edu.seu.vcampus.client.user;

import edu.seu.vcampus.client.network.ClientMessageDispatcher;
import edu.seu.vcampus.common.message.Message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 假分发器：按命令码返回预置响应，并记录发送的消息（不建真实连接，见 ADR-0009 D10）。
 *
 * <p>
 * 从 {@code UserServiceTest} 的内部类提出来，供「我的轨」与「管理轨」两个测试类共用。
 */
final class FakeUserDispatcher extends ClientMessageDispatcher {

    /** 已发送消息（按发送顺序）。 */
    final List<Message> sent = new ArrayList<Message>();

    /** 预置响应（命令码 → 响应）。 */
    private final Map<Integer, Message> m_replies = new HashMap<Integer, Message>();

    /**
     * 预置某命令码的响应。
     *
     * @param command  命令码
     * @param response 响应
     */
    void reply(int command, Message response) {
        m_replies.put(Integer.valueOf(command), response);
    }

    @Override
    public void send(Message message) {
        sent.add(message);
    }

    @Override
    public Message request(Message request, long timeoutMillis) {
        sent.add(request);
        return m_replies.get(Integer.valueOf(request.getCommand()));
    }
}
