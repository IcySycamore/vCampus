package edu.seu.vcampus.client.view.shell;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * 圆角容器绘制测试。
 */
class RoundedPanelTest {

    @Test
    void paintsConfiguredFillColor() {
        RoundedPanel panel = new RoundedPanel(new FlowLayout(), 16, Color.GREEN);
        panel.setSize(80, 60);
        BufferedImage image = new BufferedImage(80, 60, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();

        panel.paint(graphics);
        graphics.dispose();

        assertFalse(panel.isOpaque());
        assertEquals(Color.GREEN.getRGB(), image.getRGB(40, 30));
    }
}
