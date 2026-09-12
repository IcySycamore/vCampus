package edu.seu.vcampus.common.user.entity;

import org.junit.jupiter.api.Test;

import edu.seu.vcampus.common.user.entity.SessionEntry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SessionEntry 测试：身份查询、过期判定与滑动续期。
 */
class SessionEntryTest {

    /** 构造后身份字段可直接查询。 */
    @Test
    void exposesIdentity() {
        SessionEntry entry = new SessionEntry("uuid-1", "001", "学生", 1000L);

        assertEquals("uuid-1", entry.getUuid());
        assertEquals("001", entry.getUsername());
        assertEquals("学生", entry.getRole());
        assertEquals(1000L, entry.getExpiry());
    }

    /** 过期判定：到期时刻仍有效，超过才过期。 */
    @Test
    void expiryBoundary() {
        SessionEntry entry = new SessionEntry("uuid-1", "001", "学生", 1000L);

        assertFalse(entry.isExpired(1000L));
        assertTrue(entry.isExpired(1001L));
    }

    /** 续期后有效期被推后。 */
    @Test
    void renewExtendsExpiry() {
        SessionEntry entry = new SessionEntry("uuid-1", "001", "学生", 1000L);

        entry.renew(2000L);

        assertEquals(2000L, entry.getExpiry());
        assertFalse(entry.isExpired(1500L));
    }

    /** 带上姓名的构造：显示名优先用姓名。 */
    @Test
    void exposesRealName() {
        SessionEntry entry = new SessionEntry("uuid-1", "001", "张三", "学生", 1000L);

        assertEquals("张三", entry.getRealName());
        assertEquals("张三", entry.getDisplayName());
    }

    /** 未采集姓名（管理员账号）时显示名回退登录名，且不返回 null。 */
    @Test
    void displayNameFallsBackToUsername() {
        SessionEntry entry = new SessionEntry("uuid-1", "003", null, "管理员", 1000L);

        assertNull(entry.getRealName());
        assertEquals("003", entry.getDisplayName());
    }

    /** 纯空格的姓名不算数，仍回退登录名（避免界面显示一片空白）。 */
    @Test
    void blankRealNameFallsBackToUsername() {
        SessionEntry entry = new SessionEntry("uuid-1", "001", "   ", "学生", 1000L);

        assertEquals("001", entry.getDisplayName());
    }

    /** 旧的四参构造仍可用，姓名视为未采集（保证既有调用方不用改）。 */
    @Test
    void legacyConstructorHasNoRealName() {
        SessionEntry entry = new SessionEntry("uuid-1", "001", "学生", 1000L);

        assertNull(entry.getRealName());
        assertEquals("001", entry.getDisplayName());
    }
}
