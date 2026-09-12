package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.client.user.ClientSession;
import edu.seu.vcampus.common.user.entity.SessionEntry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 个人信息页的构造测试。
 *
 * <p>
 * 只验证「页面能建起来」这一条——渲染细节由 Swing 负责，为它写断言收益很低。但这一条必须守住：
 * 未连接时（单测直接构造面板、或用户还没登录就点到这页）会话为空，页面不能抛异常。
 */
class ProfilePanelTest {

    /**
     * 没有会话时也要能构造（显示「未登录」而已）。
     */
    @Test
    void createsWithoutSession() {
        ProfilePanel panel = new ProfilePanel(null);

        assertNotNull(panel);
        assertTrue(panel.getComponentCount() > 0);
    }

    /**
     * 带学生会话时能构造。
     */
    @Test
    void createsWithStudentSession() {
        ProfilePanel panel = new ProfilePanel(sessionOf("uuid-1", "001", "张三", "学生"));

        assertNotNull(panel);
        assertTrue(panel.getComponentCount() > 0);
    }

    /**
     * 带教师会话时能构造（教师也有信息查看需求）。
     */
    @Test
    void createsWithTeacherSession() {
        ProfilePanel panel = new ProfilePanel(sessionOf("uuid-t", "t01", "李老师", "教师"));

        assertNotNull(panel);
        assertTrue(panel.getComponentCount() > 0);
    }

    /**
     * 构造一个已登录的会话。
     *
     * @param uuid 账户 uuid
     * @param username 登录名
     * @param realName 姓名
     * @param role 角色显示名
     * @return 会话
     */
    private static ClientSession sessionOf(String uuid, String username, String realName,
            String role) {
        ClientSession session = new ClientSession();
        session.cache("token", new SessionEntry(uuid, username, realName, role, 0L));
        return session;
    }
}
