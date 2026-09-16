package edu.seu.vcampus.client.network;

import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.network.MessageStream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.EOFException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 消息接收任务测试：消息与断连事件带上连接代次交回连接对象，心跳不下发。
 */
class MessageReceiverTest {

    /** 连接代次。 */
    private static final long GENERATION = 7L;

    /** 非心跳消息逐条交回连接对象，读失败时上报断连（携带同一代次）。 */
    @Test
    void forwardsMessagesAndReportsUnexpectedEnd() throws Exception {
        MessageStream stream = mock(MessageStream.class);
        when(stream.recvMessage()).thenReturn(new Message(Command.HEARTBEAT, "HEARTBEAT_ACK"))
                .thenReturn(new Message(401, "first")).thenReturn(new Message(402, "second"))
                .thenThrow(new EOFException());
        ClientSocketListener client = mock(ClientSocketListener.class);

        new ClientMessageReceiverThread(stream, client, GENERATION).run();

        ArgumentCaptor<Message> messages = ArgumentCaptor.forClass(Message.class);
        verify(client, times(2)).handleReceived(eq(GENERATION), messages.capture());
        assertEquals(402, messages.getValue().getCommand());
        ArgumentCaptor<Exception> cause = ArgumentCaptor.forClass(Exception.class);
        verify(client).handleConnectionClosed(eq(GENERATION), cause.capture());
        assertInstanceOf(EOFException.class, cause.getValue());
    }

    /** 心跳确认不下发：只把真实消息交回连接对象。 */
    @Test
    void dropsHeartbeat() throws Exception {
        MessageStream stream = mock(MessageStream.class);
        when(stream.recvMessage()).thenReturn(new Message(Command.HEARTBEAT, "HEARTBEAT_ACK"))
                .thenReturn(new Message(401, "first")).thenThrow(new EOFException());
        ClientSocketListener client = mock(ClientSocketListener.class);

        new ClientMessageReceiverThread(stream, client, GENERATION).run();

        ArgumentCaptor<Message> messages = ArgumentCaptor.forClass(Message.class);
        verify(client, times(1)).handleReceived(eq(GENERATION), messages.capture());
        assertEquals(401, messages.getValue().getCommand());
    }
}
