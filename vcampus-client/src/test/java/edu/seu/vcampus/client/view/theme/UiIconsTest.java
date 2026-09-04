package edu.seu.vcampus.client.view.theme;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 图标资源加载测试。
 */
class UiIconsTest {

    @Test
    void loadsAndScalesKnownIcon() {
        Icon icon = UiIcons.load("student", 16);

        assertNotNull(icon);
        assertEquals(16, icon.getIconWidth());
        assertEquals(16, icon.getIconHeight());
        assertNull(UiIcons.load("missing-icon", 16));
    }

    @Test
    void marksResponsiveIconLabel() {
        JLabel label = UiIcons.responsiveLabel("student", 20, SwingConstants.CENTER);

        assertTrue(Boolean.TRUE.equals(label.getClientProperty("responsiveIcon")));
        assertEquals(SwingConstants.CENTER, label.getHorizontalAlignment());
    }
}
