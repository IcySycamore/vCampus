package edu.seu.vcampus.client.view.shell;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 主窗口侧栏状态测试。
 */
class SidebarPanelTest {

    @Test
    void followsContentPageChanges() {
        SidebarPanel sidebar = new SidebarPanel(null);

        sidebar.handle(PageNames.LIBRARY);

        assertEquals(PageNames.LIBRARY, sidebar.getSelectedPage());
    }
}
