package edu.seu.vcampus.client.view.component;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import javax.swing.border.AbstractBorder;

/** 绘制抗锯齿圆角描边的轻量边框。 */
public final class RoundedOutlineBorder extends AbstractBorder {
    private static final long serialVersionUID = 1L;
    private final Color color;
    private final int radius;

    /**
     * 创建圆角描边。
     *
     * @param color  描边颜色
     * @param radius 圆角直径
     */
    public RoundedOutlineBorder(Color color, int radius) {
        this.color = color;
        this.radius = radius;
    }

    @Override
    public void paintBorder(Component component, Graphics graphics, int x, int y,
            int width, int height) {
        Graphics2D copy = (Graphics2D) graphics.create();
        copy.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        copy.setColor(color);
        copy.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
        copy.dispose();
    }

    @Override
    public Insets getBorderInsets(Component component) {
        return new Insets(1, 1, 1, 1);
    }
}
