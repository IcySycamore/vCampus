package edu.seu.vcampus.client.view.bank;

import edu.seu.vcampus.client.view.shell.MainContentPanel;
import edu.seu.vcampus.client.view.shell.PageNames;
import edu.seu.vcampus.common.user.entity.Role;

import java.awt.Component;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 银行路由按角色分流：管理员进管理页，学生与教师进个人页，用户管理权限不变。 */
class BankRoutingTest {

    /** 同一侧栏条目在三种角色下挂载不同页面。 */
    @Test
    void bankRouteFollowsRoleWithoutChangingAdminAccess() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                for (Role role : Role.values()) {
                    MainContentPanel shell = new MainContentPanel("bank-test",
                            role.getDisplayName());
                    assertEquals(role == Role.ADMIN, shell.isRegistered(PageNames.USER_ADMIN));
                    shell.showPage(PageNames.BANK);
                    assertEquals(PageNames.BANK, shell.getCurrentPage());

                    Component visible = null;
                    for (Component component : shell.getComponents()) {
                        if (component.isVisible()) {
                            visible = component;
                        }
                    }
                    if (role == Role.ADMIN) {
                        assertTrue(visible instanceof BankAdminPanel);
                    } else {
                        assertTrue(visible instanceof JScrollPane);
                        assertTrue(((JScrollPane) visible).getViewport().getView()
                                instanceof BankPanel);
                        assertFalse(visible instanceof BankAdminPanel);
                    }

                    shell.showPage(PageNames.USER_ADMIN);
                    assertEquals(role == Role.ADMIN ? PageNames.USER_ADMIN : PageNames.BANK,
                            shell.getCurrentPage());
                }
            }
        });
    }
}
