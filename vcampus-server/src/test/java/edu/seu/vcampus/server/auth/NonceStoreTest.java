package edu.seu.vcampus.server.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * NonceManager 测试。
 */
class NonceStoreTest {

    /**
     * 生成的 nonce 可用一次，消费后再取失败（一次性）。
     */
    @Test
    void oneTimeUse() {
        NonceManager store = new NonceManager();
        String nonce = store.issue("001");
        assertNotEquals("", nonce);
        assertEquals(nonce, store.consume("001"));
        assertNull(store.consume("001"));
    }

    /**
     * nonce 与用户名绑定：他人无法消费该用户的 nonce。
     */
    @Test
    void bindsUsername() {
        NonceManager store = new NonceManager();
        String nonce = store.issue("001");
        assertNull(store.consume("002"));// 002 无 nonce，消费不到 001 的
        assertEquals(nonce, store.consume("001"));
    }
}