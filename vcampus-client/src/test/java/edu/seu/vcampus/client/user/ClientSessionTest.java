package edu.seu.vcampus.client.user;

import edu.seu.vcampus.common.user.entity.SessionEntry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ClientSession 视图测试：缓存 SessionEntry 后按会话记录查询身份。
 */
class ClientSessionTest {

    /** 缓存会话记录后，用户名/角色/uuid 均取自该记录。 */
    @Test
    void queriesCachedEntry() {
        ClientSession session = new ClientSession();
        SessionEntry entry = new SessionEntry("uuid-1", "001", "学生", 0L);

        session.cache("token-abc", entry);

        assertTrue(session.isLoggedIn());
        assertEquals("token-abc", session.getToken());
        assertEquals("001", session.getEntry().getUsername());
        assertEquals("学生", session.getEntry().getRole());
        assertEquals("uuid-1", session.getEntry().getUuid());
    }

    /** 未登录时所有查询返回 null / false。 */
    @Test
    void emptyBeforeLogin() {
        ClientSession session = new ClientSession();

        assertFalse(session.isLoggedIn());
        assertNull(session.getToken());
        assertNull(session.getEntry());
    }

    /** 只有令牌没有会话记录时不算登录（避免出现半截身份）。 */
    @Test
    void tokenAloneIsNotLoggedIn() {
        ClientSession session = new ClientSession();
        session.cache("token-abc", null);

        assertFalse(session.isLoggedIn());
        assertNull(session.getEntry());
    }

    /** 清除后回到未登录状态。 */
    @Test
    void clearResetsSession() {
        ClientSession session = new ClientSession();
        session.cache("token-abc", new SessionEntry("uuid-1", "001", "学生", 0L));

        session.clear();

        assertFalse(session.isLoggedIn());
        assertNull(session.getToken());
        assertNull(session.getEntry());
    }

    /**
     * 有姓名时显示名用姓名——这就是「登录后显示姓名」的落点。
     */
    @Test
    void displayNameUsesRealName() {
        ClientSession session = new ClientSession();
        session.cache("token-abc", new SessionEntry("uuid-1", "001", "张三", "学生", 0L));

        assertEquals("张三", session.getDisplayName());
        assertEquals("张三", session.getRealName());
        assertEquals("学生", session.getRole());
    }

    /**
     * 未采集姓名（例如管理员账号）时显示名回退登录名，且不返回 null。
     */
    @Test
    void displayNameFallsBackToUsername() {
        ClientSession session = new ClientSession();
        session.cache("token-abc", new SessionEntry("uuid-1", "003", null, "管理员", 0L));

        assertEquals("003", session.getDisplayName());
        assertNull(session.getRealName());
        assertEquals("管理员", session.getRole());
    }

    /**
     * 未登录时显示名是空串而不是 null，界面可以直接贴上去。
     */
    @Test
    void displayNameEmptyBeforeLogin() {
        ClientSession session = new ClientSession();

        assertEquals("", session.getDisplayName());
        assertNull(session.getRealName());
        assertNull(session.getRole());
    }
}
