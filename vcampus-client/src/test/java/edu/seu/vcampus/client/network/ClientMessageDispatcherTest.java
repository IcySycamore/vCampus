package edu.seu.vcampus.client.network;

import edu.seu.vcampus.client.handler.ConnectionListener;
import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.message.MessageSender;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

/** Tests request/reply coordination and connection lifecycle handling. */
class ClientMessageDispatcherTest {
    private static ClientMessageDispatcher dispatcherWith(MessageSender sender) {
        ClientMessageDispatcher dispatcher = new ClientMessageDispatcher();
        dispatcher.bindSender(sender);
        return dispatcher;
    }

    @Test
    void assignsRandomUidOnSend() {
        ClientMessageDispatcher dispatcher = dispatcherWith(mock(MessageSender.class));
        Message first = new Message(Command.USER_LOGOUT, null);
        Message second = new Message(Command.USER_LOGOUT, null);
        dispatcher.send(first);
        dispatcher.send(second);
        assertNotNull(first.getUid());
        assertNotNull(second.getUid());
        assertFalse(first.getUid().equals(second.getUid()));
        first.setUid(Long.valueOf(42L));
        dispatcher.send(first);
        assertEquals(Long.valueOf(42L), first.getUid());
    }

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
                assertNotNull(sent.getUid());
                dispatcher.dispatch(response);
                return null;
            }
        }).when(sender).send(any(Message.class));
        Message got = dispatcher.request(new Message(Command.USER_LOGIN, null), 500L);
        assertSame(response, got);
    }

    @Test
    void requestTimesOutWithoutResponse() throws Exception {
        ClientMessageDispatcher dispatcher = dispatcherWith(mock(MessageSender.class));
        assertNull(dispatcher.request(new Message(Command.USER_LOGIN, null), 50L));
    }

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

    @Test
    void rejectsNullArguments() {
        final ClientMessageDispatcher dispatcher =
                dispatcherWith(mock(MessageSender.class));
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
}
