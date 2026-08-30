package edu.seu.vcampus.client.view.shell;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 主内容区导航测试。
 */
class MainContentPanelTest {

    /**
     * 页面切换后应记录当前页面。
     */
    @Test
    void switchesToLibraryPage() {
        MainContentPanel panel = new MainContentPanel();

        panel.showPage(MainContentPanel.LIBRARY);

        assertEquals(MainContentPanel.LIBRARY, panel.getCurrentPage());
    }
}
