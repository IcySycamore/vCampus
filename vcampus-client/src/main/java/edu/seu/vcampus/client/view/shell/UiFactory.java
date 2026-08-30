package edu.seu.vcampus.client.view.shell;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.JTableHeader;
import javax.swing.plaf.basic.BasicButtonUI;

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
        JButton button = baseButton(text, icon + "-light");
        button.setForeground(Color.WHITE);
        button.setBackground(UiTheme.ACCENT);
        return button;
    }

    /**
     * 创建次要操作按钮。
     *
     * @param text 文本
     * @param icon 图标名
     * @return 按钮
     */
    public static JButton secondaryButton(String text, String icon) {
        JButton button = baseButton(text, icon);
        button.setForeground(UiTheme.NAVY);
        button.setBackground(new Color(235, 244, 248));
        return button;
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

    private static JButton baseButton(String text, String icon) {
        JButton button = new JButton(text, UiIcons.load(icon, 18));
        button.setUI(new BasicButtonUI());
        button.setOpaque(true);
        button.setFont(UiTheme.font(Font.BOLD, 14F));
        button.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setFocusPainted(false);
        button.setIconTextGap(8);
        return button;
    }
}
