package edu.seu.vcampus.client.network;

import org.junit.jupiter.api.Test;

/** 后台重连任务测试。 */
class ClientReconnectTaskTest {

    @Test
    void reconnectsAfterReadTimeout() throws Exception {
        new ClientSocketResilienceTest().readTimeoutReportsFailureAndReconnects();
    }
}
