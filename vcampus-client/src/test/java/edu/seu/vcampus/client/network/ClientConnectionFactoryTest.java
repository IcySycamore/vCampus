package edu.seu.vcampus.client.network;

import org.junit.jupiter.api.Test;

/** 连接工厂重试策略测试。 */
class ClientConnectionFactoryTest {

    @Test
    void retriesFailedObjectStreamHandshake() throws Exception {
        new ClientSocketResilienceTest().retriesHandshakeWithExponentialBackoff();
    }
}
