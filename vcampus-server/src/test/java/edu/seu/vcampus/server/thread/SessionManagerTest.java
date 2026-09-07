package edu.seu.vcampus.server.thread;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SessionManager 会话创建、校验、续期、过期和注销测试。
 */
class SessionManagerTest {

    @Test
    void createsAndValidatesSession() {
        SessionManager manager = new SessionManager(1000L);

        String token = manager.createSession("user-1", "学生");

        assertNotNull(token);
        assertEquals(64, token.length());
        assertTrue(manager.validateToken(token));
        assertEquals("user-1", manager.getSession(token).getUserId());
        assertEquals("学生", manager.getSession(token).getRole());
        assertEquals(1, manager.size());
    }

    @Test
    void rejectsMissingAndUnknownTokens() {
        SessionManager manager = new SessionManager(1000L);

        assertFalse(manager.validateToken(null));
        assertFalse(manager.validateToken(""));
        assertFalse(manager.validateToken("unknown-token"));
        assertFalse(manager.revoke("unknown-token"));
    }

    @Test
    void refreshesSessionOnValidAccess() throws InterruptedException {
        SessionManager manager = new SessionManager(80L);
        String token = manager.createSession("user-1", "学生");

        Thread.sleep(50L);
        assertTrue(manager.isValid(token));
        Thread.sleep(50L);

        assertTrue(manager.isValid(token),
                "有效访问应刷新会话最后访问时间");
    }

    @Test
    void expiresAndCleansUpSession() throws InterruptedException {
        SessionManager manager = new SessionManager(20L);
        String token = manager.createSession("user-1", "学生");

        Thread.sleep(40L);

        assertFalse(manager.validateToken(token));
        assertEquals(0, manager.cleanupExpiredSessions());
        assertEquals(0, manager.size());
    }

    @Test
    void revokesSession() {
        SessionManager manager = new SessionManager(1000L);
        String token = manager.createSession("user-1", "学生");

        assertTrue(manager.revoke(token));
        assertFalse(manager.validateToken(token));
        assertFalse(manager.revoke(token));
    }

    @Test
    void requiresUserIdAndRole() {
        final SessionManager manager = new SessionManager(1000L);

        assertThrows(IllegalArgumentException.class,
                new Executable() {
                    @Override
                    public void execute() {
                        manager.createSession(null, "学生");
                    }
                });
        assertThrows(IllegalArgumentException.class,
                new Executable() {
                    @Override
                    public void execute() {
                        manager.createSession("user-1", "");
                    }
                });
    }

    @Test
    void createsDifferentTokensForDifferentSessions() {
        SessionManager manager = new SessionManager(1000L);

        String first = manager.createSession("user-1", "学生");
        String second = manager.createSession("user-1", "学生");

        assertNotEquals(first, second);
    }
}