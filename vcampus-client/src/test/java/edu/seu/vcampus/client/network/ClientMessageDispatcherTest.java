package edu.seu.vcampus.client.network;

import edu.seu.vcampus.client.handler.ClientMessageHandler;
import edu.seu.vcampus.client.handler.ConnectionListener;
import edu.seu.vcampus.client.handler.UiCallback;
import edu.seu.vcampus.client.network.ClientMessageDispatcher;
import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.message.MessageSender;
import edu.seu.vcampus.common.message.Message;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

/**
 * ClientMessageDispatcher 测试：随机序列号、按命令码等待响应、处理器路由与 UI 回调。
 *
 * <p>
 * 分发器不持有连接，用例只需注入假的发送通道（{@link MessageSender}）即可覆盖全部路径。
 */
class ClientMessageDispatcherTest {

    /**
     * 新建已绑定发送通道的分发器。
     *
     * @param sender 发送通道
     * @return 分发器
     */
    private static ClientMessageDispatcher dispatcherWith(MessageSender sender) {
        ClientMessageDispatcher dispatcher = new ClientMessageDispatcher();
        dispatcher.bindSender(sender);
        return dispatcher;
    }

    /** 发送前自动分配随机序列号；已有序列号时保留。 */
    @Test
    void assignsRandomUidOnSend() {
        ClientMessageDispatcher dispatcher = dispatcherWith(mock(MessageSender.class));

        Message first = new Message(Command.USER_LOGOUT, null);
        Message second = new Message(Command.USER_LOGOUT, null);
        dispatcher.send(first);
        dispatcher.send(second);

        assertNotNull(first.getUid());
        assertNotNull(second.getUid());
        assertFalse(first.getUid().equals(second.getUid()), "随机序列号不应重复");

        first.setUid(Long.valueOf(42L));
        dispatcher.send(first);
        assertEquals(Long.valueOf(42L), first.getUid(), "已有序列号时不应改写");
    }

    /** 请求按命令码等待响应：回显同一命令码的消息即本次请求的响应。 */
    @Test
    void requestWaitsForSameCommandResponse() throws Exception {
        final MessageSender sender = mock(MessageSender.class);
        final ClientMessageDispatcher dispatcher = dispatcherWith(sender);
        final Message response = new Message(Command.USER_LOGIN, "challenge");
        response.setStatusCode("200");
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                Message sent = (Message) invocation.getArguments()[0];
                assertEquals(Command.USER_LOGIN, sent.getCommand());
                assertNotNull(sent.getUid(), "发送前必须已分配序列号");
                dispatcher.dispatch(response);// 模拟服务器回传
                return null;
            }
        }).when(sender).send(any(Message.class));

        Message got = dispatcher.request(new Message(Command.USER_LOGIN, null), 500L);

        assertSame(response, got);
    }

    /** 无响应时等待超时返回 null。 */
    @Test
    void requestTimesOutWithoutResponse() throws Exception {
        ClientMessageDispatcher dispatcher = dispatcherWith(mock(MessageSender.class));

        assertNull(dispatcher.request(new Message(Command.USER_LOGIN, null), 50L));
    }

    /** 连接断开时立即唤醒等待方（返回 null），不必等到超时。 */
    @Test
    void connectionClosedReleasesAwaiter() throws Exception {
        final MessageSender sender = mock(MessageSender.class);
        final ClientMessageDispatcher dispatcher = dispatcherWith(sender);
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                dispatcher.connectionClosed(new IOException("peer reset"));
                return null;
            }
        }).when(sender).send(any(Message.class));

        assertNull(dispatcher.request(new Message(Command.USER_LOGIN, null), 5000L));
    }

    /** 连接层收到的消息经 handleMessage 进入同一条路由。 */
    @Test
    void handleMessageRoutesLikeDispatch() {
        ClientMessageDispatcher dispatcher = dispatcherWith(mock(MessageSender.class));
        RecordingHandler handler = new RecordingHandler();
        dispatcher.register(Command.STUDENT_QUERY, handler);

        Message push = new Message(Command.STUDENT_QUERY, "profile");
        dispatcher.handleMessage(push);

        assertSame(push, handler.m_last);
    }

    /** 连接关闭时通知登记在分发器上的监听者。 */
    @Test
    void notifiesConnectionListeners() {
        ClientMessageDispatcher dispatcher = dispatcherWith(mock(MessageSender.class));
        final Exception[] captured = new Exception[1];
        dispatcher.addConnectionListener(new ConnectionListener() {
            @Override
            public void connectionClosed(Exception cause) {
                captured[0] = cause;
            }
        });

        IOException cause = new IOException("peer reset");
        dispatcher.connectionClosed(cause);

        assertSame(cause, captured[0]);
    }

    /** 未配对的消息按命令码交给登记的处理器，并带上注入的发送通道与 UI 回调。 */
    @Test
    void routesUnmatchedMessageToHandler() {
        MessageSender sender = mock(MessageSender.class);
        ClientMessageDispatcher dispatcher = dispatcherWith(sender);
        RecordingHandler handler = new RecordingHandler();
        dispatcher.register(Command.STUDENT_QUERY, handler);
        RecordingUi ui = new RecordingUi();
        dispatcher.setUiCallback(ui);

        Message push = new Message(Command.STUDENT_QUERY, "profile");
        dispatcher.dispatch(push);

        assertSame(push, handler.m_last);
        assertSame(sender, handler.m_sender);
        assertSame(ui, handler.m_ui);
    }

    /** 未登记命令码时走兜底处理器。 */
    @Test
    void unregisteredCommandUsesFallback() {
        ClientMessageDispatcher dispatcher = dispatcherWith(mock(MessageSender.class));
        RecordingHandler fallback = new RecordingHandler();
        dispatcher.registerFallback(fallback);

        Message unknown = new Message(9999, "x");
        dispatcher.dispatch(unknown);

        assertSame(unknown, fallback.m_last);
    }

    /** 既未配对也未登记：消息被忽略，不报错。 */
    @Test
    void dropsUnroutableMessage() {
        ClientMessageDispatcher dispatcher = dispatcherWith(mock(MessageSender.class));

        dispatcher.dispatch(new Message(9999, "x"));
        dispatcher.dispatch(null);
    }

    /** 处理器抛异常被隔离，不会穿透到接收线程。 */
    @Test
    void isolatesHandlerFailure() {
        ClientMessageDispatcher dispatcher = dispatcherWith(mock(MessageSender.class));
        dispatcher.register(Command.USER_LOGIN, new ClientMessageHandler() {
            @Override
            public void handle(Message message, MessageSender sender, UiCallback ui) {
                throw new IllegalStateException("boom");
            }
        });

        dispatcher.dispatch(new Message(Command.USER_LOGIN, null));
    }

    /** 未注入 UI 回调时，处理器的界面任务在当前线程直接执行。 */
    @Test
    void uiCallbackDefaultsToInline() {
        ClientMessageDispatcher dispatcher = dispatcherWith(mock(MessageSender.class));
        final boolean[] ran = new boolean[1];
        dispatcher.register(Command.USER_LOGIN, new MessageHandlerRunsTask(ran));
        dispatcher.setUiCallback(null);// 恢复默认

        dispatcher.dispatch(new Message(Command.USER_LOGIN, null));

        assertTrue(ran[0], "默认回调应在当前线程执行");
    }

    /** 注入 UI 回调后，界面任务交给注入方。 */
    @Test
    void uiCallbackIsUsedWhenInjected() {
        ClientMessageDispatcher dispatcher = dispatcherWith(mock(MessageSender.class));
        final boolean[] ran = new boolean[1];
        RecordingUi ui = new RecordingUi();
        dispatcher.setUiCallback(ui);
        dispatcher.register(Command.USER_LOGIN, new MessageHandlerRunsTask(ran));

        dispatcher.dispatch(new Message(Command.USER_LOGIN, null));

        assertFalse(ran[0], "任务应交给注入的回调，而非直接执行");
        assertNotNull(ui.m_last, "注入的回调应收到任务");
    }

    /** 未绑定发送通道时发送/请求快速失败，不留等待槽。 */
    @Test
    void requiresSenderBound() {
        final ClientMessageDispatcher dispatcher = new ClientMessageDispatcher();

        assertThrows(IllegalStateException.class, new Executable() {
            @Override
            public void execute() {
                dispatcher.send(new Message(Command.USER_LOGOUT, null));
            }
        });
        assertThrows(IllegalStateException.class, new Executable() {
            @Override
            public void execute() throws Throwable {
                dispatcher.request(new Message(Command.USER_LOGIN, null), 50L);
            }
        });
    }

    /** 空处理器 / 空发送通道 / 空消息快速失败。 */
    @Test
    void rejectsNullArguments() {
        final ClientMessageDispatcher dispatcher = dispatcherWith(mock(MessageSender.class));

        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() {
                dispatcher.register(Command.USER_LOGIN, null);
            }
        });
        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() {
                dispatcher.bindSender(null);
            }
        });
        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() {
                dispatcher.addConnectionListener(null);
            }
        });
        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() {
                dispatcher.send(null);
            }
        });
    }

    /** 记录最后一条消息、回发通道与 UI 回调的处理器。 */
    private static final class RecordingHandler implements ClientMessageHandler {
        private Message m_last;
        private MessageSender m_sender;
        private UiCallback m_ui;

        @Override
        public void handle(Message message, MessageSender sender, UiCallback ui) {
            m_last = message;
            m_sender = sender;
            m_ui = ui;
        }
    }

    /** 记录被派发的界面任务的 UI 回调。 */
    private static final class RecordingUi implements UiCallback {
        private Runnable m_last;

        @Override
        public void run(Runnable task) {
            m_last = task;
        }
    }

    /** 用 UI 回调执行一个界面任务并打点，用于验证回调确实被调用。 */
    private static final class MessageHandlerRunsTask implements ClientMessageHandler {
        private final boolean[] m_marks;

        MessageHandlerRunsTask(boolean[] marks) {
            this.m_marks = marks;
        }

        @Override
        public void handle(Message message, MessageSender sender, UiCallback ui) {
            ui.run(new Runnable() {
                @Override
                public void run() {
                    m_marks[0] = true;
                }
            });
        }
    }
}
