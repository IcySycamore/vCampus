package edu.seu.vcampus.server.dispatch;

import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.handler.MessageHandler;
import edu.seu.vcampus.common.handler.MessageSender;
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
     * 登记过的命令码应路由到对应处理器，处理器通过 sender 发送响应。
     */
    @Test
    void dispatchRoutesToRegisteredHandler() {
        MessageDispatcher dispatcher = new MessageDispatcher();
        dispatcher.register(201, new MessageHandler() {
            @Override
            public void handle(Message request, MessageSender sender) {
                Message response = new Message(request.getCommand(), "handled");
                response.setStatusCode(StatusCode.SUCCESS);
                sender.send(response);
            }
        });

        final Message[] sent = new Message[1];
        MessageSender sender = new MessageSender() {
            @Override
            public void send(Message response) {
                sent[0] = response;
            }
        };

        dispatcher.dispatch(new Message(201, "data"), sender);

        assertEquals(StatusCode.SUCCESS, sent[0].getStatusCode());
        assertEquals("handled", sent[0].getData());
    }

    /**
     * 未登记的命令码应通过 sender 发送 400 响应。
     */
    @Test
    void dispatchSendsBadRequestForUnknownCommand() {
        MessageDispatcher dispatcher = new MessageDispatcher();

        final Message[] sent = new Message[1];
        MessageSender sender = new MessageSender() {
            @Override
            public void send(Message response) {
                sent[0] = response;
            }
        };

        dispatcher.dispatch(new Message(999, "data"), sender);

        assertEquals(StatusCode.BAD_REQUEST, sent[0].getStatusCode());
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
