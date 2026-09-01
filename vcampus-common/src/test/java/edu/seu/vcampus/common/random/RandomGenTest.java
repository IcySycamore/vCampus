package edu.seu.vcampus.common.random;

import org.junit.jupiter.api.Test;

import java.util.UUID;

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
}