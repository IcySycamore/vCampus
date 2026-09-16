package edu.seu.vcampus.client.course;

import edu.seu.vcampus.client.view.theme.UiFactory;
import edu.seu.vcampus.client.view.theme.UiTheme;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;

/**
 * 选课界面各视图区域的构建工具。
 */
final class CourseViewBuilder {

    private CourseViewBuilder() {
    }

    /**
     * 构建选课页标题区。
     *
     * @return 标题面板
     */
    static JPanel heading() {
        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        JPanel text = new JPanel(new BorderLayout(0, 5));
        text.setOpaque(false);
        JLabel title = new JLabel("选课与退课");
        title.setForeground(UiTheme.TEXT);
        title.setFont(UiTheme.font(Font.BOLD, 28F));
        JLabel subtitle = new JLabel("浏览可选课程，管理你的课程安排");
        subtitle.setForeground(UiTheme.MUTED);
        subtitle.setFont(UiTheme.font(Font.PLAIN, 15F));
        text.add(title, BorderLayout.NORTH);
        text.add(subtitle, BorderLayout.SOUTH);
        heading.add(text, BorderLayout.WEST);
        return heading;
    }

    /**
     * 构建选课页内容区：工具栏 + 表格。
     *
     * @param table 课程表格
     * @param toolbar 工具栏
     * @return 内容面板
     */
    static JPanel content(JTable table, JPanel toolbar) {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setOpaque(false);
        panel.add(toolbar, BorderLayout.NORTH);
        UiFactory.styleTable(table);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER));
        scroll.getViewport().setBackground(UiTheme.SURFACE);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    /**
     * 构建选课页工具栏。
     *
     * @param keywordField 搜索框
     * @param controller 面板控制器
     * @return 工具栏
     */
    static JPanel toolbar(JTextField keywordField, final CourseController controller) {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        toolbar.setOpaque(false);
        toolbar.add(label("关键词"));
        keywordField.setPreferredSize(new Dimension(220, 38));
        keywordField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiTheme.BORDER),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        toolbar.add(keywordField);
        JButton searchButton = UiFactory.primaryButton("搜索", "search");
        searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                controller.applyFilter();
            }
        });
        toolbar.add(searchButton);
        JButton refreshButton = UiFactory.secondaryButton("刷新课程", "refresh");
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                controller.panel.refreshCourses();
            }
        });
        toolbar.add(refreshButton);
        JButton selectButton = UiFactory.primaryButton("选课", "user");
        selectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                controller.selectSelected();
            }
        });
        toolbar.add(selectButton);
        JButton dropButton = UiFactory.secondaryButton("退课", "return");
        dropButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                controller.dropSelected();
            }
        });
        toolbar.add(dropButton);
        return toolbar;
    }

    /**
     * 设置状态标签的统一样式。
     *
     * @param statusLabel 状态标签
     */
    static void styleStatus(JLabel statusLabel) {
        statusLabel.setOpaque(true);
        statusLabel.setForeground(UiTheme.MUTED);
        statusLabel.setBackground(new Color(234, 241, 245));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(9, 10, 9, 10));
    }

    private static JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(UiTheme.MUTED);
        label.setFont(UiTheme.font(Font.BOLD, 13F));
        return label;
    }
}
