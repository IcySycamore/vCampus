package edu.seu.vcampus.common.handler;

import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.message.Message;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * MessageHandler 接口契约测试：验证接口可被实现、handle 能正常处理消息并返回响应。
 */
class MessageHandlerTest {

    /**
     * 匿名实现应能按契约处理消息并返回响应。
     */
    @Test
    void handleIsInvokedOnImplementation() {
        MessageHandler handler = new MessageHandler() {
            @Override
            public Message handle(Message request) {
                Message response = new Message(request.getCommand(), "echo");
                response.setStatusCode(StatusCode.SUCCESS);
                return response;
            }
        };

        Message request = new Message(201, "data");
        Message response = handler.handle(request);

        assertEquals(StatusCode.SUCCESS, response.getStatusCode());
        assertEquals("echo", response.getData());
    }
}
