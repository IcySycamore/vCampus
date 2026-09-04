package edu.seu.vcampus.client.network;

import org.junit.jupiter.api.Test;

/** 接收事件转发测试。 */
class ClientReceiverHandlerTest {

    @Test
    void forwardsEventsForCurrentConnectionGeneration() throws Exception {
        new ClientSocketTest().sendsAndReceivesMessage();
    }
}
