package edu.seu.vcampus.client.network;

import edu.seu.vcampus.client.handler.UIUpdateHandler;
import edu.seu.vcampus.common.message.Message;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * 后台消息接收任务测试。
 */
class MessageReceiverTest {

    @Test
    void forwardsMessagesAndReportsUnexpectedEnd() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ObjectOutputStream output = new ObjectOutputStream(bytes);
        output.writeObject(new Message(401, "first"));
        output.writeObject("ignored");
        output.writeObject(new Message(402, "second"));
        output.close();
        RecordingHandler handler = new RecordingHandler();
        ObjectInputStream input = new ObjectInputStream(
                new ByteArrayInputStream(bytes.toByteArray()));

        new MessageReceiver(input, handler).run();

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
