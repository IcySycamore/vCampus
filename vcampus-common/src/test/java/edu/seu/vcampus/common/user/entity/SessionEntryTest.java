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

    /** 姓名原样带出，界面直接显示——不再需要额外的「显示名」概念。 */
    @Test
    void exposesRealName() {
        SessionEntry entry = new SessionEntry("uuid-1", "001", "张三", "学生", 1000L);

        assertEquals("张三", entry.getRealName());
    }

    /** 旧的四参构造仍可用（姓名未采集时为 null；服务端不会签发这种会话）。 */
    @Test
    void legacyConstructorHasNoRealName() {
        SessionEntry entry = new SessionEntry("uuid-1", "001", "学生", 1000L);

        assertNull(entry.getRealName());
        assertEquals("001", entry.getUsername());
    }
}
