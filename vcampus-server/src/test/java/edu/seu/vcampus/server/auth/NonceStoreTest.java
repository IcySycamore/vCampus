package edu.seu.vcampus.server.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * NonceStore 测试。
 */
class NonceStoreTest {

    /**
     * 生成的 nonce 可用一次，再用失败（一次性）。
     */
    @Test
    void oneTimeUse() {
        NonceStore store = new NonceStore();
        String nonce = store.issue("001");
        assertNotEquals("", nonce);
        assertTrue(store.verifyAndConsume(nonce, "001"));
        assertFalse(store.verifyAndConsume(nonce, "001"));
    }

    /**
     * nonce 与用户名绑定：错误用户验证失败。
     */
    @Test
    void bindsUsername() {
        NonceStore store = new NonceStore();
        String nonce = store.issue("001");
        assertFalse(store.verifyAndConsume(nonce, "002"));
    }
}