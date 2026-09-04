package edu.seu.vcampus.client.network;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** 客户端网络参数测试。 */
class ClientNetworkConfigTest {

    @Test
    void exposesDefaultsAndRejectsInvalidValues() {
        ClientNetworkConfig config = ClientNetworkConfig.defaults();

        assertEquals(5000, config.getConnectTimeoutMillis());
        assertEquals(30000, config.getReadTimeoutMillis());
        assertEquals(3, config.getMaxRetries());
        assertEquals(250L, config.getInitialBackoffMillis());
        assertEquals(4000L, config.getMaxBackoffMillis());
        assertEquals(1000L, config.getShutdownGraceMillis());
        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() {
                new ClientNetworkConfig(0, 1, 0, 0L, 0L, 0L);
            }
        });
    }
}
