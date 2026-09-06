package edu.seu.vcampus.common.handler;

import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.message.Message;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * MessageHandler 接口契约测试：验证接口可被实现，处理完成后通过 sender 发送响应。
 */
class MessageHandlerTest {

    /**
     * 匿名实现应能处理消息，并通过 sender 发送响应。
     */
    @Test
    void handleSendsResponseThroughSender() {
        MessageHandler handler = new MessageHandler() {
            @Override
            public void handle(Message request, MessageSender sender) {
                Message response = new Message(request.getCommand(), "echo");
                response.setStatusCode(StatusCode.SUCCESS);
                sender.send(response);
            }
        };

        final Message[] sent = new Message[1];
        MessageSender sender = new MessageSender() {
            @Override
            public void send(Message response) {
                sent[0] = response;
            }
        };

        handler.handle(new Message(201, "data"), sender);

        assertEquals(StatusCode.SUCCESS, sent[0].getStatusCode());
        assertEquals("echo", sent[0].getData());
    }
}
