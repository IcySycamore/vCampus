package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.common.user.entity.SessionEntry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 账户弹窗内容测试：字段呈现、姓名回退与两个动作回调。
 */
class AccountPopupPanelTest {

    @Test
    void showsNameRoleUserNameAndUuid() {
        SessionEntry session = new SessionEntry("uuid-1", "2025001", "张三", "学生", 0L);

        AccountPopupPanel panel = new AccountPopupPanel(session, null, null);

        assertEquals("张三", panel.getDisplayNameText());
        assertEquals("登录名：2025001", panel.getUserNameText());
        assertEquals("身份：学生", panel.getRoleText());
        assertEquals("标识：uuid-1", panel.getUuidText());
    }

    @Test
    void fallsBackToUserNameWhenNameMissing() {
        SessionEntry session = new SessionEntry("uuid-2", "admin", null, "管理员", 0L);

        AccountPopupPanel panel = new AccountPopupPanel(session, null, null);

        assertEquals("admin", panel.getDisplayNameText());
    }

    @Test
    void invokesChangePasswordAndLogoutCallbacks() {
        final int[] counts = new int[2];
        SessionEntry session = new SessionEntry("uuid-3", "2025002", "李四", "教师", 0L);
        AccountPopupPanel panel = new AccountPopupPanel(session, new Runnable() {
            @Override
            public void run() {
                counts[0]++;
            }
        }, new Runnable() {
            @Override
            public void run() {
                counts[1]++;
            }
        });

        panel.getChangePasswordButton().doClick();
        panel.getLogoutButton().doClick();

        assertEquals(1, counts[0]);
        assertEquals(1, counts[1]);
    }

    @Test
    void toleratesMissingSessionAndActions() {
        AccountPopupPanel panel = new AccountPopupPanel(null, null, null);

        assertEquals("-", panel.getDisplayNameText());
        assertTrue(panel.getRoleText().startsWith("身份："));
        panel.getChangePasswordButton().doClick();
        panel.getLogoutButton().doClick();
    }
}
