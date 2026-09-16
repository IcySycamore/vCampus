package edu.seu.vcampus.client.view.component;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * 现代标签页外观测试。
 */
class ModernTabbedPaneUITest {

    @Test
    void installsTransparentTabbedPaneUi() {
        JTabbedPane tabs = new JTabbedPane();
        ModernTabbedPaneUI ui = new ModernTabbedPaneUI();
        tabs.addTab("首页", new JPanel());

        tabs.setUI(ui);

        assertSame(ui, tabs.getUI());
        assertFalse(tabs.isOpaque());
    }
}
