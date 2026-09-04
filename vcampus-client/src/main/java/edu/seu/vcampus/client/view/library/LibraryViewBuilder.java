package edu.seu.vcampus.client.view.library;

import edu.seu.vcampus.client.view.shell.RoundedPanel;
import edu.seu.vcampus.client.view.shell.ModernTabbedPaneUI;
import edu.seu.vcampus.client.view.shell.UiFactory;
import edu.seu.vcampus.client.view.shell.UiIcons;
import edu.seu.vcampus.client.view.shell.UiTheme;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.plaf.basic.BasicComboBoxUI;

/**
 * 负责构建图书馆筛选区、标签页和表格表面。
 */
final class LibraryViewBuilder {

    private final JTextField keyword;
    private final JComboBox<String> field;
    private final JTable books;
    private final JTable borrows;

    LibraryViewBuilder(JTextField keyword, JComboBox<String> field,
            JTable books, JTable borrows) {
        this.keyword = keyword;
        this.field = field;
        this.books = books;
        this.borrows = borrows;
        styleInputs();
        UiFactory.styleTable(books);
        UiFactory.styleTable(borrows);
    }

    JTabbedPane createTabs(ActionListener search, ActionListener borrow,
            ActionListener refresh, ActionListener returnBook) {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setUI(new ModernTabbedPaneUI());
        tabs.setFont(UiTheme.font(Font.BOLD, 14F));
        tabs.setForeground(UiTheme.NAVY);
        tabs.setBorder(BorderFactory.createEmptyBorder());
        tabs.addTab("检索图书", UiIcons.load("search", 18),
                createSearch(search, borrow));
        tabs.addTab("我的借阅", UiIcons.load("borrow", 18),
                createBorrows(refresh, returnBook));
        return tabs;
    }

    private JPanel createSearch(ActionListener search, ActionListener borrow) {
        RoundedPanel card = card();
        JPanel filters = toolbar();
        filters.add(label("关键词"));
        filters.add(keyword);
        filters.add(label("检索范围"));
        filters.add(field);
        JButton searchButton = UiFactory.primaryButton("搜索", "search");
        searchButton.addActionListener(search);
        filters.add(searchButton);
        JButton borrowButton = UiFactory.secondaryButton("借阅所选", "borrow");
        borrowButton.addActionListener(borrow);
        filters.add(borrowButton);
        card.add(filters, BorderLayout.NORTH);
        card.add(scroll(books), BorderLayout.CENTER);
        return card;
    }

    private JPanel createBorrows(ActionListener refresh, ActionListener returnBook) {
        RoundedPanel card = card();
        JPanel actions = toolbar();
        JButton refreshButton = UiFactory.secondaryButton("刷新记录", "refresh");
        refreshButton.addActionListener(refresh);
        actions.add(refreshButton);
        JButton returnButton = UiFactory.primaryButton("归还所选", "return");
        returnButton.addActionListener(returnBook);
        actions.add(returnButton);
        card.add(actions, BorderLayout.NORTH);
        card.add(scroll(borrows), BorderLayout.CENTER);
        return card;
    }

    private RoundedPanel card() {
        RoundedPanel panel = new RoundedPanel(new BorderLayout(0, 14), 20, UiTheme.SURFACE);
        panel.setBorder(BorderFactory.createEmptyBorder(18, 20, 20, 20));
        return panel;
    }

    private JPanel toolbar() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        panel.setOpaque(false);
        return panel;
    }

    private JScrollPane scroll(JTable table) {
        JScrollPane pane = new JScrollPane(table);
        pane.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER));
        pane.getViewport().setBackground(UiTheme.SURFACE);
        return pane;
    }

    private JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(UiTheme.MUTED);
        label.setFont(UiTheme.font(Font.BOLD, 13F));
        return label;
    }

    private void styleInputs() {
        keyword.setPreferredSize(new Dimension(230, 38));
        keyword.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiTheme.BORDER),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        field.setPreferredSize(new Dimension(115, 38));
        field.setUI(new BasicComboBoxUI());
        field.setBackground(UiTheme.SURFACE);
        field.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER));
    }
}
