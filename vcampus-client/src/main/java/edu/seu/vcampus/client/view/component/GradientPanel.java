package edu.seu.vcampus.client.view.component;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JPanel;

/**
 * 带有轻量装饰圆形的青蓝渐变面板。
 */
public class GradientPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private final Color start;
    private final Color end;

    /**
     * 创建渐变面板。
     *
     * @param start 左上颜色
     * @param end 右下颜色
     */
    public GradientPanel(Color start, Color end) {
        this.start = start;
        this.end = end;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D copy = (Graphics2D) graphics.create();
        copy.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        copy.setPaint(new GradientPaint(0, 0, start, getWidth(), getHeight(), end));
        copy.fillRect(0, 0, getWidth(), getHeight());
        copy.setColor(new Color(255, 255, 255, 18));
        int size = Math.max(180, getWidth() / 2);
        copy.fillOval(-size / 3, getHeight() - size / 2, size, size);
        copy.fillOval(getWidth() - size / 2, -size / 3, size, size);
        copy.dispose();
        super.paintComponent(graphics);
    }
}
