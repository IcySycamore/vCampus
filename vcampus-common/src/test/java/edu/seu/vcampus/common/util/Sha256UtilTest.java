package edu.seu.vcampus.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * SHA-256 工具测试。
 */
class Sha256UtilTest {

    /**
     * 已知明文应得到已知哈希（标准测试向量）。
     */
    @Test
    void knownVector() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                Sha256Util.sha256Hex("abc"));
    }

    /**
     * 加盐哈希应随盐变化。
     */
    @Test
    void saltedHashDiffers() {
        assertNotEquals(Sha256Util.sha256Hex("salt1", "pwd"),
                Sha256Util.sha256Hex("salt2", "pwd"));
    }
}