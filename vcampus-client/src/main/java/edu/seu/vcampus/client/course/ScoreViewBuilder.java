package edu.seu.vcampus.client.course;

import edu.seu.vcampus.client.view.theme.UiFactory;
import edu.seu.vcampus.client.view.theme.UiTheme;

import java.awt.BorderLayout;
import java.awt.Color;
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
 * 成绩界面各视图区域的构建工具。
 */
final class ScoreViewBuilder {

    private ScoreViewBuilder() {
    }

    /**
     * 构建成绩页标题区。
     *
     * @param studentView 是否学生视角
     * @return 标题面板
     */
    static JPanel heading(boolean studentView) {
        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        JPanel text = new JPanel(new BorderLayout(0, 5));
        text.setOpaque(false);
        JLabel title = new JLabel("成绩中心");
        title.setForeground(UiTheme.TEXT);
        title.setFont(UiTheme.font(Font.BOLD, 28F));
        JLabel subtitle = new JLabel(studentView
                ? "查看个人各科成绩与平均绩点" : "录入、更新与修改学生成绩");
        subtitle.setForeground(UiTheme.MUTED);
        subtitle.setFont(UiTheme.font(Font.PLAIN, 15F));
        text.add(title, BorderLayout.NORTH);
        text.add(subtitle, BorderLayout.SOUTH);
        heading.add(text, BorderLayout.WEST);
        return heading;
    }

    /**
     * 构建成绩页内容区：工具栏 + 表格。
     *
     * @param table 成绩表格
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
     * 构建成绩页工具栏，包含搜索、刷新与（教师/管理员）成绩录入表单。
     *
     * @param studentView 是否学生视角
     * @param keywordField 搜索框
     * @param studentIdField 学号输入框
     * @param courseCodeField 课程编号输入框
     * @param scoreField 成绩输入框
     * @param controller 面板控制器
     * @return 工具栏
     */
    static JPanel toolbar(boolean studentView, JTextField keywordField,
            JTextField studentIdField, JTextField courseCodeField,
            JTextField scoreField, final ScoreController controller) {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        toolbar.setOpaque(false);
        toolbar.add(label("关键词"));
        toolbar.add(keywordField);
        JButton searchButton = UiFactory.secondaryButton("搜索", "search");
        searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                controller.applyFilter();
            }
        });
        toolbar.add(searchButton);
        JButton refreshButton = UiFactory.secondaryButton("刷新成绩", "refresh");
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                controller.panel.refreshScores();
            }
        });
        toolbar.add(refreshButton);
        if (!studentView) {
            toolbar.add(form(studentIdField, courseCodeField, scoreField, controller));
        }
        return toolbar;
    }

    /**
     * 构建底部状态栏与 GPA 展示区。
     *
     * @param statusLabel 状态标签
     * @param gpaLabel GPA 标签
     * @return 底部面板
     */
    static JPanel footer(JLabel statusLabel, JLabel gpaLabel) {
        statusLabel.setOpaque(true);
        statusLabel.setForeground(UiTheme.MUTED);
        statusLabel.setBackground(new Color(234, 241, 245));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(9, 10, 9, 10));
        gpaLabel.setForeground(UiTheme.NAVY);
        gpaLabel.setFont(UiTheme.font(Font.BOLD, 15F));
        gpaLabel.setBorder(BorderFactory.createEmptyBorder(9, 10, 9, 10));
        JPanel footer = new JPanel(new BorderLayout(10, 0));
        footer.setOpaque(false);
        footer.add(statusLabel, BorderLayout.CENTER);
        JPanel gpa = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        gpa.setOpaque(false);
        gpa.add(gpaLabel);
        footer.add(gpa, BorderLayout.EAST);
        return footer;
    }

    private static JPanel form(JTextField studentIdField, JTextField courseCodeField,
            JTextField scoreField, final ScoreController controller) {
        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        form.setOpaque(false);
        form.add(label("学号"));
        form.add(studentIdField);
        form.add(label("课程编号"));
        form.add(courseCodeField);
        form.add(label("成绩"));
        form.add(scoreField);
        JButton saveButton = UiFactory.primaryButton("保存成绩", "user");
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                controller.saveSelected();
            }
        });
        form.add(saveButton);
        return form;
    }

    private static JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(UiTheme.MUTED);
        label.setFont(UiTheme.font(Font.BOLD, 13F));
        return label;
    }
}
