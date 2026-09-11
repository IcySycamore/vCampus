package edu.seu.vcampus.client.network;

import org.junit.jupiter.api.Test;

/** 客户端优雅关闭测试。 */
class ClientShutdownTest {

    @Test
    void deliversInflightMessageBeforeClosing() throws Exception {
        new ClientSocketResilienceTest().gracefulCloseDeliversInflightMessage();
    }
}
