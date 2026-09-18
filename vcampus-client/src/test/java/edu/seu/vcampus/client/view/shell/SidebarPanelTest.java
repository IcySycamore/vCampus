package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.common.user.entity.Role;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 主窗口侧栏状态与按角色过滤测试。
 */
class SidebarPanelTest {

    @Test
    void followsContentPageChanges() {
        SidebarPanel sidebar = new SidebarPanel(null);

        sidebar.handle(PageNames.LIBRARY);

        assertEquals(PageNames.LIBRARY, sidebar.getSelectedPage());
    }

    @Test
    void hidesUserAdminEntryFromStudents() {
        SidebarPanel sidebar = new SidebarPanel(null, Role.STUDENT);

        assertFalse(sidebar.isVisible(PageNames.USER_ADMIN));
        assertTrue(sidebar.isVisible(PageNames.LIBRARY));
        assertTrue(sidebar.isVisible(PageNames.HOME));
    }

    @Test
    void hidesUserAdminEntryFromTeachers() {
        SidebarPanel sidebar = new SidebarPanel(null, Role.TEACHER);

        assertFalse(sidebar.isVisible(PageNames.USER_ADMIN));
    }

    @Test
    void showsUserAdminEntryForAdministrators() {
        SidebarPanel sidebar = new SidebarPanel(null, Role.ADMIN);

        assertTrue(sidebar.isVisible(PageNames.USER_ADMIN));
        assertNotNull(sidebar.buttonFor(PageNames.USER_ADMIN).getIcon());
    }

    @Test
    void showsNoUserCenterEntryAtAll() {
        SidebarPanel sidebar = new SidebarPanel(null, Role.ADMIN);

        assertFalse(sidebar.isVisible("user"));
    }
}
