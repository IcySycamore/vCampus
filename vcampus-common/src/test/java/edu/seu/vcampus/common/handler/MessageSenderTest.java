package edu.seu.vcampus.common.handler;

import edu.seu.vcampus.common.message.Message;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * MessageSender 接口契约测试：验证接口可被实现、send 能正常发送消息。
 */
class MessageSenderTest {

    /**
     * 匿名实现应能接收并保存被发送的响应。
     */
    @Test
    void sendIsInvokedOnImplementation() {
        final Message[] sent = new Message[1];
        MessageSender sender = new MessageSender() {
            @Override
            public void send(Message response) {
                sent[0] = response;
            }
        };

        Message response = new Message(201, "data");
        sender.send(response);

        assertEquals(response, sent[0]);
    }
}
