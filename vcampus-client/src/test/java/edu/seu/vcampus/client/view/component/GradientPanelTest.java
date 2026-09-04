package edu.seu.vcampus.client.view.component;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * 渐变背景面板绘制测试。
 */
class GradientPanelTest {

    @Test
    void paintsNonTransparentGradient() {
        GradientPanel panel = new GradientPanel(Color.RED, Color.BLUE);
        panel.setSize(120, 80);
        BufferedImage image = new BufferedImage(120, 80, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();

        panel.paint(graphics);
        graphics.dispose();

        assertFalse(panel.isOpaque());
        assertNotEquals(0, image.getRGB(60, 40));
    }
}
