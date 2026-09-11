package edu.seu.vcampus.client.network;

import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.network.MessageStream;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/** 客户端定时心跳任务测试。 */
class ClientHeartbeatTest {

    /** 心跳应使用公共命令码 1，且不携带 token。 */
    @Test
    void periodicallySendsProtocolHeartbeat() throws Exception {
        ClientSocket client = mock(ClientSocket.class);
        MessageStream stream = mock(MessageStream.class);
        ClientHeartbeat heartbeat = ClientHeartbeat.start(client, stream, 7L, 20L);
        ArgumentCaptor<Message> message = ArgumentCaptor.forClass(Message.class);
        try {
            verify(stream, timeout(1000L).atLeastOnce()).writeMessage(message.capture());
        } finally {
            heartbeat.stop();
        }
        assertEquals(1, message.getValue().getCommand());
        assertNull(message.getValue().getData());
        assertNull(message.getValue().getToken());
    }

    /** 心跳写入失败应关闭对应代次的连接，以触发现有重连流程。 */
    @Test
    void reportsWriteFailureToConnectionLifecycle() throws Exception {
        ClientSocket client = mock(ClientSocket.class);
        MessageStream stream = mock(MessageStream.class);
        IOException failure = new IOException("heartbeat failed");
        doThrow(failure).when(stream).writeMessage(any(Message.class));
        ClientHeartbeat heartbeat = ClientHeartbeat.start(client, stream, 9L, 20L);
        try {
            verify(client, timeout(1000L).atLeastOnce())
                    .handleConnectionClosed(9L, failure);
        } finally {
            heartbeat.stop();
        }
    }
}
