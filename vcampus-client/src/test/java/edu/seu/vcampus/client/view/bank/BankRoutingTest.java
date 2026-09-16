package edu.seu.vcampus.client.view.bank;

import edu.seu.vcampus.client.view.shell.MainContentPanel;
import edu.seu.vcampus.client.view.shell.PageNames;
import edu.seu.vcampus.common.user.entity.Role;
import java.awt.Component;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 银行接入新版路由后，各身份可到达真实银行页且保留用户管理权限。 */
class BankRoutingTest {
    @Test
    void bankRouteShowsBankPanelWithoutChangingAdminAccess() throws Exception {
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
                        if (component.isVisible()) { visible = component; }
                    }
                    assertTrue(visible instanceof JScrollPane);
                    assertTrue(((JScrollPane) visible).getViewport().getView()
                            instanceof BankPanel);
                    shell.showPage(PageNames.USER_ADMIN);
                    assertEquals(role == Role.ADMIN ? PageNames.USER_ADMIN : PageNames.BANK,
                            shell.getCurrentPage());
                }
            }
        });
    }
}
