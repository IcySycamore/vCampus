package edu.seu.vcampus.client.view.shell;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * 校园工作台布局测试。
 */
class OaDashboardPanelTest {

    @Test
    void buildsGreetingAndDashboardSections() {
        OaDashboardPanel panel = new OaDashboardPanel("10001", "学生", null);

        assertInstanceOf(ProportionalLayout.class, panel.getLayout());
        assertEquals(2, panel.getComponentCount());
    }
}
