package edu.seu.vcampus.client.view.component;

import javax.swing.JTextField;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 图标输入行测试。
 */
class IconTextFieldPanelTest {

    @Test
    void containsIconAndField() {
        IconTextFieldPanel panel = new IconTextFieldPanel("user", new JTextField());

        assertEquals(2, panel.getComponentCount());
    }
}
