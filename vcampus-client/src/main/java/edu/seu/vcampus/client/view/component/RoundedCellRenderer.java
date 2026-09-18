package edu.seu.vcampus.client.view.component;

import edu.seu.vcampus.client.view.theme.UiTheme;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * 圆角表格单元格渲染器：把 Swing 默认的直角底色块换成圆角块。
 *
 * <p>
 * 课表与时间槽格子原来直接用 {@code setBackground} 填整格，整个界面看下来全是直角的方块； 这里把底色改成「留白 4px
 * 的圆角矩形」再画文字，格与格之间自然分开，视觉效果接近卡片。
 *
 * <p>
 * 渲染器本身不透明会被表格的直角底色盖住，所以构造时置 {@code opaque = false}，只由本类画底色； 周围露出的即是表格背景（已由 {@link UiTheme}
 * 统一成页面色）。
 */
public class RoundedCellRenderer extends DefaultTableCellRenderer {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 圆角半径。 */
    private static final int ARC = 14;

    /** 块与格子边缘之间留的空白。 */
    private static final int INSET = 4;

    /** 构造一个圆角渲染器。 */
    public RoundedCellRenderer() {
        setOpaque(false);
    }

    /** {@inheritDoc} */
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {
        Component component = super.getTableCellRendererComponent(table, value, isSelected,
                hasFocus, row, column);
        component.setBackground(isSelected && table.getSelectionBackground() != null
                ? table.getSelectionBackground()
                : table.getBackground());
        return component;
    }

    /** {@inheritDoc} */
    @Override
    protected void paintComponent(Graphics graphics) {
        Color fill = getBackground();
        if (fill != null) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fill);
            g2.fillRoundRect(INSET, INSET, Math.max(0, getWidth() - 2 * INSET),
                    Math.max(0, getHeight() - 2 * INSET), ARC, ARC);
            g2.dispose();
        }
        super.paintComponent(graphics);
    }
}
