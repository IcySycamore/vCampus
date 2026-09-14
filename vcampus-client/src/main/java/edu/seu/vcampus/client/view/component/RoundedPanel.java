package edu.seu.vcampus.client.view.component;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import javax.swing.JPanel;

/**
 * 可复用的圆角表面容器。
 */
public class RoundedPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private final int radius;
    private final Color fill;

    /**
     * 创建圆角容器。
     *
     * @param layout 布局管理器
     * @param radius 圆角半径
     * @param fill 填充色
     */
    public RoundedPanel(LayoutManager layout, int radius, Color fill) {
        super(layout);
        this.radius = radius;
        this.fill = fill;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D copy = (Graphics2D) graphics.create();
        copy.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        copy.setColor(fill);
        copy.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
        copy.dispose();
        super.paintComponent(graphics);
    }
}
