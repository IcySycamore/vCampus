package edu.seu.vcampus.client.view.component;

import edu.seu.vcampus.client.view.theme.UiTheme;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;

/**
 * 圆角按钮：现代 SaaS 风格的扁平按钮，支持实心（主操作）与线框/幽灵（次操作）两种形态，
 * 并带鼠标悬停微交互。
 */
public class RoundedButton extends JButton {

    private static final long serialVersionUID = 1L;

    /** 常态背景；线框模式下背景透明，仅用作边框色。 */
    private final Color accent;

    /** 常态前景。 */
    private final Color foreground;

    /** 圆角半径。 */
    private final int radius;

    /** 是否为线框（幽灵）样式。 */
    private final boolean outlined;

    /** 当前是否悬停。 */
    private boolean hovered;

    /**
     * 构造圆角按钮。
     *
     * @param text 文本
     * @param icon 图标（可为 null）
     * @param accent 主色（实心时为背景色，线框时为边框/文字色）
     * @param foreground 前景色（实心时通常为白色）
     * @param radius 圆角半径
     * @param outlined 是否线框样式
     */
    public RoundedButton(String text, Icon icon, Color accent, Color foreground,
            int radius, boolean outlined) {
        super(text, icon);
        this.accent = accent;
        this.foreground = foreground;
        this.radius = radius;
        this.outlined = outlined;
        setFont(UiTheme.font(Font.BOLD, 14F));
        setForeground(outlined ? accent : foreground);
        if (!outlined) {
            setBackground(accent);
        }
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setIconTextGap(8);
        setBorder(BorderFactory.createEmptyBorder(9, 16, 9, 16));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                hovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent event) {
                hovered = false;
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (outlined) {
            if (hovered) {
                g2.setColor(withAlpha(accent, 24));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            }
            g2.setColor(accent);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
        } else {
            g2.setColor(hovered ? darken(accent) : accent);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
        }
        g2.dispose();
        super.paintComponent(graphics);
    }

    private static Color darken(Color color) {
        return new Color(Math.max(0, (int) (color.getRed() * 0.88f)),
                Math.max(0, (int) (color.getGreen() * 0.88f)),
                Math.max(0, (int) (color.getBlue() * 0.88f)));
    }

    private static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }
}
