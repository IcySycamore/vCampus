package edu.seu.vcampus.server.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * SessionManager 测试。
 */
class SessionManagerTest {

    /**
     * 签发后可校验出真实身份。
     */
    @Test
    void createAndValidate() {
        SessionManager manager = new SessionManager();
        String token = manager.create("uuid-001", "001", "学生");
        assertNotNull(token);
        SessionManager.SessionEntry entry = manager.validate(token);
        assertNotNull(entry);
        assertEquals("uuid-001", entry.getUuid());
        assertEquals("001", entry.getUsername());
        assertEquals("学生", entry.getRole());
    }

    /**
     * 无效 token 校验失败。
     */
    @Test
    void invalidToken() {
        SessionManager manager = new SessionManager();
        assertNull(manager.validate("not-a-token"));
    }

    /**
     * 登出后 token 失效。
     */
    @Test
    void invalidate() {
        SessionManager manager = new SessionManager();
        String token = manager.create("uuid-001", "001", "学生");
        manager.invalidate(token);
        assertNull(manager.validate(token));
    }
}