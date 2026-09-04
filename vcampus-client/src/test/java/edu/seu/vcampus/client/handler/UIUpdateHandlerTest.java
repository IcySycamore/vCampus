package edu.seu.vcampus.client.handler;

import edu.seu.vcampus.common.message.Message;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * UI 网络回调接口测试。
 */
class UIUpdateHandlerTest {

    @Test
    void dispatchesMessageAndCloseCause() {
        final Message[] message = new Message[1];
        final Exception[] cause = new Exception[1];
        UIUpdateHandler handler = new UIUpdateHandler() {
            @Override
            public void handleMessage(Message value) {
                message[0] = value;
            }

            @Override
            public void connectionClosed(Exception value) {
                cause[0] = value;
            }
        };
        Message expectedMessage = new Message(400, "test");
        Exception expectedCause = new Exception("closed");

        handler.handleMessage(expectedMessage);
        handler.connectionClosed(expectedCause);

        assertSame(expectedMessage, message[0]);
        assertSame(expectedCause, cause[0]);
    }
}
