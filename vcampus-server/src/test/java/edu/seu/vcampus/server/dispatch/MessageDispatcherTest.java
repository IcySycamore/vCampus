package edu.seu.vcampus.server.dispatch;

import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.handler.MessageHandler;
import edu.seu.vcampus.common.message.Message;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * MessageDispatcher 分发测试：验证命令路由、未登记命令回 400、空处理器被拒绝。
 */
class MessageDispatcherTest {

    /**
     * 登记过的命令码应路由到对应处理器并返回其响应。
     */
    @Test
    void dispatchRoutesToRegisteredHandler() {
        MessageDispatcher dispatcher = new MessageDispatcher();
        dispatcher.register(201, new MessageHandler() {
            @Override
            public Message handle(Message request) {
                Message response = new Message(request.getCommand(), "handled");
                response.setStatusCode(StatusCode.SUCCESS);
                return response;
            }
        });

        Message request = new Message(201, "data");
        Message response = dispatcher.dispatch(request);

        assertEquals(StatusCode.SUCCESS, response.getStatusCode());
        assertEquals("handled", response.getData());
    }

    /**
     * 未登记的命令码应返回 400 响应。
     */
    @Test
    void dispatchReturnsBadRequestForUnknownCommand() {
        MessageDispatcher dispatcher = new MessageDispatcher();

        Message request = new Message(999, "data");
        Message response = dispatcher.dispatch(request);

        assertEquals(StatusCode.BAD_REQUEST, response.getStatusCode());
    }

    /**
     * 注册空处理器应被拒绝。
     */
    @Test
    void registerRejectsNullHandler() {
        final MessageDispatcher dispatcher = new MessageDispatcher();

        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() throws Throwable {
                dispatcher.register(201, null);
            }
        });
    }
}
