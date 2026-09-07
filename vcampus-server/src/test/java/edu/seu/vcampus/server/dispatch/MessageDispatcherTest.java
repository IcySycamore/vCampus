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

    /**
     * 范围注册后，范围内的任意命令码（含起止边界）都应路由到该处理器。
     */
    @Test
    void rangeRegisterRoutesWholeSegment() {
        MessageDispatcher dispatcher = new MessageDispatcher();
        dispatcher.register(200, 299, new MessageHandler() {
            @Override
            public void handle(Message request, MessageSender sender) {
                Message response = new Message(request.getCommand(), "segment");
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

        dispatcher.dispatch(new Message(200, "start"), sender);
        assertEquals(StatusCode.SUCCESS, sent[0].getStatusCode());

        dispatcher.dispatch(new Message(250, "middle"), sender);
        assertEquals(StatusCode.SUCCESS, sent[0].getStatusCode());

        dispatcher.dispatch(new Message(299, "end"), sender);
        assertEquals(StatusCode.SUCCESS, sent[0].getStatusCode());
    }

    /**
     * 范围之外、范围之前的命令码应回 400。
     */
    @Test
    void rangeRegisterSendsBadRequestOutsideSegment() {
        MessageDispatcher dispatcher = new MessageDispatcher();
        dispatcher.register(200, 299, new MessageHandler() {
            @Override
            public void handle(Message request, MessageSender sender) {
            }
        });

        final Message[] sent = new Message[1];
        MessageSender sender = new MessageSender() {
            @Override
            public void send(Message response) {
                sent[0] = response;
            }
        };

        dispatcher.dispatch(new Message(199, "below"), sender);
        assertEquals(StatusCode.BAD_REQUEST, sent[0].getStatusCode());

        dispatcher.dispatch(new Message(300, "above"), sender);
        assertEquals(StatusCode.BAD_REQUEST, sent[0].getStatusCode());
    }

    /**
     * 与已登记范围重叠的注册应被拒绝。
     */
    @Test
    void registerRejectsOverlappingRange() {
        final MessageDispatcher dispatcher = new MessageDispatcher();
        dispatcher.register(200, 299, new MessageHandler() {
            @Override
            public void handle(Message request, MessageSender sender) {
            }
        });

        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() throws Throwable {
                dispatcher.register(250, 350, new MessageHandler() {
                    @Override
                    public void handle(Message request, MessageSender sender) {
                    }
                });
            }
        });
    }

    /**
     * 起始命令码大于终止命令码应被拒绝。
     */
    @Test
    void registerRejectsInvertedRange() {
        final MessageDispatcher dispatcher = new MessageDispatcher();

        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() throws Throwable {
                dispatcher.register(299, 200, new MessageHandler() {
                    @Override
                    public void handle(Message request, MessageSender sender) {
                    }
                });
            }
        });
    }
}
