package edu.seu.vcampus.client.network;

import edu.seu.vcampus.client.handler.ClientMessageHandler;
import edu.seu.vcampus.client.handler.UiCallback;
import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.message.MessageSender;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/** Tests routing received messages to registered handlers and UI callbacks. */
class ClientMessageDispatcherRoutingTest {
    private ClientMessageDispatcher dispatcherWith(MessageSender sender) {
        ClientMessageDispatcher dispatcher = new ClientMessageDispatcher();
        dispatcher.bindSender(sender);
        return dispatcher;
    }

    @Test
    void handleMessageRoutesLikeDispatch() {
        ClientMessageDispatcher dispatcher = dispatcherWith(mock(MessageSender.class));
        RecordingHandler handler = new RecordingHandler();
        dispatcher.register(Command.STUDENT_QUERY, handler);
        Message push = new Message(Command.STUDENT_QUERY, "profile");
        dispatcher.handleMessage(push);
        assertSame(push, handler.last);
    }

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
        assertSame(push, handler.last);
        assertSame(sender, handler.sender);
        assertSame(ui, handler.ui);
    }

    @Test
    void unregisteredCommandUsesFallback() {
        ClientMessageDispatcher dispatcher = dispatcherWith(mock(MessageSender.class));
        RecordingHandler fallback = new RecordingHandler();
        dispatcher.registerFallback(fallback);
        Message unknown = new Message(9999, "x");
        dispatcher.dispatch(unknown);
        assertSame(unknown, fallback.last);
    }

    @Test
    void dropsUnroutableMessage() {
        ClientMessageDispatcher dispatcher = dispatcherWith(mock(MessageSender.class));
        dispatcher.dispatch(new Message(9999, "x"));
        dispatcher.dispatch(null);
    }

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

    @Test
    void uiCallbackDefaultsToInline() {
        ClientMessageDispatcher dispatcher = dispatcherWith(mock(MessageSender.class));
        final boolean[] ran = new boolean[1];
        dispatcher.register(Command.USER_LOGIN, new MessageHandlerRunsTask(ran));
        dispatcher.setUiCallback(null);
        dispatcher.dispatch(new Message(Command.USER_LOGIN, null));
        assertTrue(ran[0]);
    }

    @Test
    void uiCallbackIsUsedWhenInjected() {
        ClientMessageDispatcher dispatcher = dispatcherWith(mock(MessageSender.class));
        final boolean[] ran = new boolean[1];
        RecordingUi ui = new RecordingUi();
        dispatcher.setUiCallback(ui);
        dispatcher.register(Command.USER_LOGIN, new MessageHandlerRunsTask(ran));
        dispatcher.dispatch(new Message(Command.USER_LOGIN, null));
        assertFalse(ran[0]);
        assertNotNull(ui.last);
    }

    private static final class RecordingHandler implements ClientMessageHandler {
        private Message last;
        private MessageSender sender;
        private UiCallback ui;

        @Override
        public void handle(Message message, MessageSender replySender, UiCallback callback) {
            last = message;
            sender = replySender;
            ui = callback;
        }
    }

    private static final class RecordingUi implements UiCallback {
        private Runnable last;

        @Override
        public void run(Runnable task) {
            last = task;
        }
    }

    private static final class MessageHandlerRunsTask implements ClientMessageHandler {
        private final boolean[] marks;

        MessageHandlerRunsTask(boolean[] marks) {
            this.marks = marks;
        }

        @Override
        public void handle(Message message, MessageSender sender, UiCallback ui) {
            ui.run(new Runnable() {
                @Override
                public void run() {
                    marks[0] = true;
                }
            });
        }
    }
}
