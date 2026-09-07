package edu.seu.vcampus.client.network;

import edu.seu.vcampus.client.handler.UIUpdateHandler;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.network.MessageStream;
import java.io.EOFException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 后台消息接收任务测试。
 */
class MessageReceiverTest {

    @Test
    void forwardsMessagesAndReportsUnexpectedEnd() throws Exception {
        MessageStream stream = mock(MessageStream.class);
        when(stream.recvMessage())
                .thenReturn(new Message(401, "first"))
                .thenReturn(new Message(402, "second"))
                .thenThrow(new EOFException());
        RecordingHandler handler = new RecordingHandler();

        new MessageReceiver(stream, handler).run();

        assertEquals(2, handler.count);
        assertEquals(402, handler.lastMessage.getCommand());
        assertInstanceOf(EOFException.class, handler.cause);
    }

    private static final class RecordingHandler implements UIUpdateHandler {

        private int count;
        private Message lastMessage;
        private Exception cause;

        @Override
        public void handleMessage(Message message) {
            count++;
            lastMessage = message;
        }

        @Override
        public void connectionClosed(Exception value) {
            cause = value;
        }
    }
}
