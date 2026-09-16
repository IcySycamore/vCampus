package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.common.user.dto.UserQuery;
import edu.seu.vcampus.common.user.entity.Role;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 筛选条「控件 → 查询条件」映射测试。
 */
class UserQueryBarTest {

    @Test
    void translatesDefaultsToUnfilteredQuery() {
        UserQueryBar bar = new UserQueryBar();

        UserQuery query = bar.toQuery(2, 20);

        assertEquals("", query.getKeyword());
        assertNull(query.getRole());
        assertNull(query.getEnabled());
        assertEquals(2, query.getPageNumber());
        assertEquals(20, query.getPageSize());
    }

    @Test
    void translatesSelectedRoleAndState() {
        UserQueryBar bar = new UserQueryBar();
        bar.setKeyword("2025");
        bar.selectRole("教师");
        bar.selectState("禁用");

        UserQuery query = bar.toQuery(1, 10);

        assertEquals("2025", query.getKeyword());
        assertEquals(Role.TEACHER, query.getRole());
        assertEquals(Boolean.FALSE, query.getEnabled());
    }

    @Test
    void queryButtonFiresCallback() {
        UserQueryBar bar = new UserQueryBar();
        final boolean[] fired = new boolean[1];
        bar.setOnQuery(new Runnable() {
            @Override
            public void run() {
                fired[0] = true;
            }
        });

        bar.selectState("启用");
        findSearchButton(bar).doClick();

        assertTrue(fired[0]);
    }

    private javax.swing.JButton findSearchButton(UserQueryBar bar) {
        for (java.awt.Component component : bar.getComponents()) {
            if (component instanceof javax.swing.JButton) {
                return (javax.swing.JButton) component;
            }
        }
        throw new IllegalStateException("查询按钮不存在");
    }
}
