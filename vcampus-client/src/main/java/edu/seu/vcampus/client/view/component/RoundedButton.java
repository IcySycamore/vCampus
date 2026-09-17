package edu.seu.vcampus.client.view.component;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.plaf.basic.BasicButtonUI;

/** 带圆角填充与描边的轻量按钮。 */
public final class RoundedButton extends JButton {
    private static final long serialVersionUID = 1L;
    private final Color borderColor;
    private final int radius;

    /**
     * 创建圆角按钮。
     *
     * @param text       按钮文字
     * @param background 填充颜色
     * @param foreground 文字颜色
     * @param border     描边颜色
     * @param radius     圆角直径
     */
    public RoundedButton(String text, Color background, Color foreground,
            Color border, int radius) {
        super(text);
        borderColor = border;
        this.radius = radius;
        setUI(new BasicButtonUI());
        setBackground(background);
        setForeground(foreground);
        setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        setBorderPainted(false);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D copy = (Graphics2D) graphics.create();
        copy.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        copy.setColor(fillColor());
        copy.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
        copy.setColor(borderColor);
        copy.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
        copy.dispose();
        super.paintComponent(graphics);
    }

    private Color fillColor() {
        if (!isEnabled()) {
            return new Color(getBackground().getRed(), getBackground().getGreen(),
                    getBackground().getBlue(), 110);
        }
        return getModel().isPressed() ? getBackground().darker() : getBackground();
    }
}
