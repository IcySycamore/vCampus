package edu.seu.vcampus.client.view.theme;

import edu.seu.vcampus.client.view.component.RoundedButton;
import edu.seu.vcampus.client.view.theme.UiIcons;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.JTableHeader;

/**
 * 创建统一风格按钮和表格的界面工厂。
 */
public final class UiFactory {

    private UiFactory() {
    }

    /**
     * 创建主操作按钮。
     *
     * @param text 文本
     * @param icon 图标名
     * @return 按钮
     */
    public static JButton primaryButton(String text, String icon) {
        return new RoundedButton(text, UiIcons.load(icon + "-light", 18),
                UiTheme.ACCENT, Color.WHITE, 8, false);
    }

    /**
     * 创建次要操作按钮（线框/幽灵样式）。
     *
     * @param text 文本
     * @param icon 图标名
     * @return 按钮
     */
    public static JButton secondaryButton(String text, String icon) {
        return new RoundedButton(text, UiIcons.load(icon, 18),
                UiTheme.ACCENT, UiTheme.ACCENT, 8, true);
    }

    /**
     * 应用统一表格视觉。
     *
     * @param table 表格
     */
    public static void styleTable(JTable table) {
        table.setRowHeight(42);
        table.setShowVerticalLines(false);
        table.setGridColor(UiTheme.BORDER);
        table.setSelectionBackground(new Color(222, 239, 247));
        table.setSelectionForeground(UiTheme.TEXT);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JTableHeader header = table.getTableHeader();
        header.setFont(UiTheme.font(Font.BOLD, 13F));
        header.setForeground(UiTheme.MUTED);
        header.setBackground(new Color(247, 249, 251));
        header.setPreferredSize(new Dimension(0, 40));
    }
}
