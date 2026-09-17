package edu.seu.vcampus.client.view.library;

import edu.seu.vcampus.client.view.component.ModernTabbedPaneUI;
import edu.seu.vcampus.client.view.component.RoundedPanel;
import edu.seu.vcampus.client.view.theme.UiFactory;
import edu.seu.vcampus.client.view.theme.UiIcons;
import edu.seu.vcampus.client.view.theme.UiTheme;
import java.awt.BorderLayout;
import java.awt.FontMetrics;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.regex.Pattern;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.RowFilter;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

/** 构建图书馆检索页和共享表格表面。 */
final class LibraryViewBuilder {
    LibraryViewBuilder() {
    }

    JTabbedPane createTabs(JPanel home, JPanel catalog) {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setName("libraryTabs");
        tabs.setUI(new ModernTabbedPaneUI());
        tabs.setBackground(UiTheme.BACKGROUND);
        tabs.setFont(UiTheme.font(Font.BOLD, 14F));
        tabs.setForeground(UiTheme.NAVY);
        tabs.setBorder(BorderFactory.createEmptyBorder());
        tabs.addTab("图书馆首页", UiIcons.load("home", 18), home);
        tabs.addTab("图书查询", UiIcons.load("search", 18), catalog);
        return tabs;
    }

    static JPanel cardWithToolbar(JTable table, JPanel actions) {
        RoundedPanel card = card();
        card.add(actions, BorderLayout.NORTH);
        card.add(scroll(table, null, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED),
                BorderLayout.CENTER);
        return card;
    }

    static JPanel toolbar() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        panel.setOpaque(false);
        return panel;
    }

    static JTextField addKeywordFilter(JPanel toolbar, JTable table, String name) {
        final JTextField keyword = new JTextField(12);
        final TableRowSorter<DefaultTableModel> sorter = createSorter(table);
        keyword.setName(name);
        keyword.setToolTipText("筛选当前列表中的任意字段");
        toolbar.add(new JLabel("关键词"));
        toolbar.add(keyword);
        keyword.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                filter(sorter, keyword.getText());
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                filter(sorter, keyword.getText());
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                filter(sorter, keyword.getText());
            }
        });
        return keyword;
    }

    static JScrollPane scroll(JTable table, String name, int horizontalPolicy) {
        UiFactory.styleTable(table);
        if (table.getRowSorter() == null) {
            createSorter(table);
        }
        table.setBackground(UiTheme.BACKGROUND);
        JScrollPane pane = new JScrollPane(table,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, horizontalPolicy);
        pane.setName(name);
        pane.setBackground(UiTheme.BACKGROUND);
        pane.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER));
        pane.getViewport().setBackground(UiTheme.BACKGROUND);
        return pane;
    }

    /** 按表格当前内容加宽文本列，内容过长时交由水平滚动条展示。 */
    static void fitTextColumn(JTable table, int columnIndex, int minimumWidth) {
        FontMetrics metrics = table.getFontMetrics(table.getFont());
        int width = Math.max(minimumWidth,
                metrics.stringWidth(table.getColumnName(columnIndex)) + 24);
        for (int row = 0; row < table.getModel().getRowCount(); row++) {
            Object value = table.getModel().getValueAt(row, columnIndex);
            width = Math.max(width, metrics.stringWidth(String.valueOf(value)) + 24);
        }
        table.getColumnModel().getColumn(columnIndex).setPreferredWidth(width);
    }

    /** 调整馆藏表中可能较长的 ISBN 与作者列。 */
    static void fitCatalogTextColumns(JTable table) {
        fitTextColumn(table, 0, 180);
        fitTextColumn(table, 2, 190);
    }

    static void runOnUi(Runnable action) {
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
        } else {
            SwingUtilities.invokeLater(action);
        }
    }

    static JPanel createHeading() {
        JPanel text = new JPanel(new BorderLayout(0, 5));
        text.setOpaque(false);
        JLabel title = new JLabel("智慧图书馆");
        title.setForeground(UiTheme.TEXT);
        title.setFont(UiTheme.font(Font.BOLD, 28F));
        JLabel subtitle = new JLabel("预约到馆保留 15 天 · 借期 30 天 · 最多续借 2 次");
        subtitle.setForeground(UiTheme.MUTED);
        subtitle.setFont(UiTheme.font(Font.PLAIN, 15F));
        text.add(title, BorderLayout.NORTH);
        text.add(subtitle, BorderLayout.SOUTH);
        return text;
    }

    static JPanel createFooter(JLabel status, JLabel quota) {
        JPanel footer = new JPanel(new BorderLayout(0, 8));
        footer.setOpaque(false);
        status.setOpaque(true);
        status.setForeground(UiTheme.MUTED);
        status.setBackground(new java.awt.Color(234, 241, 245));
        status.setBorder(BorderFactory.createEmptyBorder(9, 10, 9, 10));
        footer.add(quota, BorderLayout.NORTH);
        footer.add(status, BorderLayout.SOUTH);
        return footer;
    }

    private static RoundedPanel card() {
        RoundedPanel panel = new RoundedPanel(new BorderLayout(0, 14),
                20, UiTheme.BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(18, 20, 20, 20));
        return panel;
    }

    private static TableRowSorter<DefaultTableModel> createSorter(JTable table) {
        TableRowSorter<DefaultTableModel> sorter =
                new TableRowSorter<DefaultTableModel>((DefaultTableModel) table.getModel());
        table.setRowSorter(sorter);
        return sorter;
    }

    private static void filter(TableRowSorter<DefaultTableModel> sorter, String text) {
        String keyword = text.trim();
        sorter.setRowFilter(keyword.isEmpty() ? null
                : RowFilter.regexFilter("(?i)" + Pattern.quote(keyword)));
    }

}
