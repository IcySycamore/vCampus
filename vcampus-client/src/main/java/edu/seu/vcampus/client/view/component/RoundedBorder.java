package edu.seu.vcampus.client.view.component;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;

import javax.swing.border.AbstractBorder;

/**
 * 圆角边框：用于输入框等控件，营造更柔和的现代观感。
 */
public class RoundedBorder extends AbstractBorder {

    private static final long serialVersionUID = 1L;

    private final Color color;
    private final int radius;

    /**
     * 构造圆角边框。
     *
     * @param color 边框颜色
     * @param radius 圆角半径
     */
    public RoundedBorder(Color color, int radius) {
        this.color = color;
        this.radius = radius;
    }

    @Override
    public void paintBorder(Component component, Graphics graphics, int x, int y,
            int width, int height) {
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(color);
        g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
        g2.dispose();
    }

    @Override
    public Insets getBorderInsets(Component component) {
        return new Insets(radius / 2 + 3, radius + 4, radius / 2 + 3, radius + 4);
    }
}
