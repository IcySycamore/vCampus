package edu.seu.vcampus.common.random;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * UUID 生成工具测试。
 */
class RandomGenTest {

    /**
     * 生成的 UUID 非空且两次调用不同。
     */
    @Test
    void uuidGenerated() {
        RandomGen gen = new RandomGen();
        UUID first = gen.getUuid();
        UUID second = gen.getUuid();
        assertNotNull(first);
        assertNotNull(second);
        assertNotEquals(first, second);
    }

    /**
     * randomHex 生成指定长度且两次不同。
     */
    @Test
    void randomHexLength() {
        RandomGen gen = new RandomGen();
        String first = gen.randomHex(16);
        String second = gen.randomHex(16);
        assertEquals(32, first.length());
        assertEquals(32, second.length());
        assertNotEquals(first, second);
    }
}