package edu.seu.vcampus.client.view.shell;

import javax.swing.JPanel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 一级页面路由测试。
 */
class AppRouterTest {

    @Test
    void registersAndNavigatesPages() {
        JPanel container = new JPanel();
        AppRouter router = new AppRouter(container, PageNames.HOME);
        router.register(PageNames.HOME, new JPanel());
        router.register(PageNames.LIBRARY, new JPanel());

        router.navigate(PageNames.LIBRARY);

        assertEquals(PageNames.LIBRARY, router.getCurrentPage());
        assertEquals(2, container.getComponentCount());
    }
}
