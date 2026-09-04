package edu.seu.vcampus.client.view.shell;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * 主窗口面包屑、全局搜索、设置和用户信息顶栏。
 */
public class MainHeaderPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private final JTextField searchField = new JTextField(20);

    /**
     * 创建校园工作台顶栏。
     *
     * @param userId 用户 ID
     * @param role 登录身份
     * @param search 搜索回调
     * @param settings 设置回调
     */
    public MainHeaderPanel(String userId, String role,
            final StringAction search, final Runnable settings) {
        setLayout(new BorderLayout(22, 0));
        setBackground(UiTheme.BACKGROUND);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(225, 218, 204)),
                BorderFactory.createEmptyBorder(14, 24, 14, 24)));
        add(createBreadcrumb(), BorderLayout.WEST);
        add(createSearch(search), BorderLayout.CENTER);
        add(createActions(userId, role, settings), BorderLayout.EAST);
    }

    private JPanel createBreadcrumb() {
        JPanel panel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 8));
        panel.setOpaque(false);
        JLabel home = new JLabel("首页");
        home.setForeground(UiTheme.TEXT);
        home.setFont(UiTheme.font(Font.BOLD, 14F));
        JLabel separator = new JLabel("/");
        separator.setForeground(UiTheme.MUTED);
        JLabel page = new JLabel("工作台");
        page.setForeground(UiTheme.MUTED);
        panel.add(home);
        panel.add(separator);
        panel.add(page);
        return panel;
    }

    private JPanel createSearch(final StringAction search) {
        JPanel panel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 0, 2));
        panel.setOpaque(false);
        searchField.setPreferredSize(new Dimension(270, 36));
        searchField.setToolTipText("搜索学籍、课程、图书馆等校园功能");
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 216, 208)),
                BorderFactory.createEmptyBorder(6, 11, 6, 11)));
        ActionListener searchAction = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                search.accept(searchField.getText());
            }
        };
        searchField.addActionListener(searchAction);
        panel.add(searchField);
        JButton button = UiFactory.primaryButton("搜索", "search");
        button.addActionListener(searchAction);
        panel.add(button);
        return panel;
    }

    private JPanel createActions(String userId, String role, final Runnable settings) {
        JPanel actions = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        JButton settingButton = UiFactory.secondaryButton("设置", "settings");
        settingButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                settings.run();
            }
        });
        actions.add(settingButton);
        RoundedPanel avatar = new RoundedPanel(new BorderLayout(), 24, UiTheme.NAVY);
        avatar.setBorder(BorderFactory.createEmptyBorder(8, 9, 8, 9));
        avatar.add(new JLabel(UiIcons.load("user-light", 18)));
        actions.add(avatar);
        JPanel text = new JPanel(new java.awt.GridLayout(2, 1, 0, 1));
        text.setOpaque(false);
        JLabel user = new JLabel(role + " · " + userId);
        user.setForeground(UiTheme.TEXT);
        user.setFont(UiTheme.font(Font.BOLD, 13F));
        JLabel identity = new JLabel(role);
        identity.setForeground(UiTheme.MUTED);
        identity.setFont(UiTheme.font(Font.PLAIN, 11F));
        text.add(user);
        text.add(identity);
        actions.add(text);
        return actions;
    }
}
