package edu.seu.vcampus.server.network;

import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.message.MessageHandler;
import edu.seu.vcampus.common.message.MessageSender;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.server.network.ServerMessageDispatcher;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * MessageDispatcher 分发测试：验证命令路由、未登记命令回 400、空处理器被拒绝。
 */
class ServerMessageDispatcherTest {

    /**
     * 登记过的命令码应路由到对应处理器，处理器通过 sender 发送响应。
     */
    @Test
    void dispatchRoutesToRegisteredHandler() {
        ServerMessageDispatcher dispatcher = new ServerMessageDispatcher();
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
        ServerMessageDispatcher dispatcher = new ServerMessageDispatcher();

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
        final ServerMessageDispatcher dispatcher = new ServerMessageDispatcher();

        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() throws Throwable {
                dispatcher.register(201, null);
            }
        });
    }

    /**
     * 范围登记：段内命令（含起止边界）路由到该处理器，段外命令回 400。
     */
    @Test
    void rangeRegisterCoversSegmentOnly() {
        ServerMessageDispatcher dispatcher = new ServerMessageDispatcher();
        dispatcher.register(200, 299, echoHandler("segment"));

        final Message[] sent = new Message[1];
        MessageSender sender = recordingSender(sent);

        int[] inside = {200, 250, 299};
        int index = 0;
        while (index < inside.length) {
            dispatcher.dispatch(new Message(inside[index], "data"), sender);
            assertEquals(StatusCode.SUCCESS, sent[0].getStatusCode(),
                    "段内命令 " + inside[index] + " 应被路由");
            index = index + 1;
        }

        int[] outside = {199, 300};
        index = 0;
        while (index < outside.length) {
            dispatcher.dispatch(new Message(outside[index], "data"), sender);
            assertEquals(StatusCode.BAD_REQUEST, sent[0].getStatusCode(),
                    "段外命令 " + outside[index] + " 应回 400");
            index = index + 1;
        }
    }

    /**
     * 同一范围重复登记应覆盖旧处理器，而不是报冲突。
     */
    @Test
    void duplicateSameRangeOverwrites() {
        ServerMessageDispatcher dispatcher = new ServerMessageDispatcher();
        dispatcher.register(200, 299, echoHandler("old"));
        dispatcher.register(200, 299, echoHandler("new"));

        final Message[] sent = new Message[1];
        dispatcher.dispatch(new Message(250, "data"), recordingSender(sent));

        assertEquals("new", sent[0].getData(), "同范围重复登记应以后者为准");
    }

    /**
     * 与已登记范围部分重叠的注册应被拒绝（属号段冲突，需尽早暴露）。
     */
    @Test
    void registerRejectsOverlappingRange() {
        final ServerMessageDispatcher dispatcher = new ServerMessageDispatcher();
        dispatcher.register(200, 299, echoHandler("segment"));

        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() throws Throwable {
                dispatcher.register(250, 350, echoHandler("overlap"));
            }
        });
    }

    /**
     * 起始命令码大于终止命令码应被拒绝。
     */
    @Test
    void registerRejectsInvertedRange() {
        final ServerMessageDispatcher dispatcher = new ServerMessageDispatcher();

        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() throws Throwable {
                dispatcher.register(299, 200, echoHandler("inverted"));
            }
        });
    }

    /**
     * 构造固定回 SUCCESS 的处理器。
     *
     * @param payload 响应数据
     * @return 处理器
     */
    private static MessageHandler echoHandler(final String payload) {
        return new MessageHandler() {
            @Override
            public void handle(Message request, MessageSender sender) {
                Message response = new Message(request.getCommand(), payload);
                response.setStatusCode(StatusCode.SUCCESS);
                sender.send(response);
            }
        };
    }

    /**
     * 构造把响应记录到数组的发送器。
     *
     * @param sent 长度为 1 的数组，用于回传响应
     * @return 发送器
     */
    private static MessageSender recordingSender(final Message[] sent) {
        return new MessageSender() {
            @Override
            public void send(Message response) {
                sent[0] = response;
            }
        };
    }
}
