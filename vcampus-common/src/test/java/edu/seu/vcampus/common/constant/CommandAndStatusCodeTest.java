package edu.seu.vcampus.common.constant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 命令码与状态码常量测试（一个测试覆盖 Command 与 StatusCode 两个类）。
 */
class CommandAndStatusCodeTest {

    /**
     * 用户管理命令码段 100-199 自洽。
     */
    @Test
    void commandSegment() {
        assertEquals(100, Command.USER_LOGIN);
        assertEquals(101, Command.USER_LOGOUT);
        assertEquals(109, Command.USER_SALT_REQUEST);
    }

    /**
     * 状态码关键值正确。
     */
    @Test
    void statusCodes() {
        assertEquals("200", StatusCode.SUCCESS);
        assertEquals("P101", StatusCode.ROLE_MISMATCH);
        assertEquals("P102", StatusCode.USER_DISABLED);
    }
}