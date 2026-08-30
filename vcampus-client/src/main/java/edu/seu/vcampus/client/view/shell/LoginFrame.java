package edu.seu.vcampus.client.view.shell;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;

/**
 * 深色品牌区与白色登录卡组成的 vCampus 登录窗口。
 */
public class LoginFrame extends JFrame {

    private static final long serialVersionUID = 1L;
    private final JTextField userIdField = new JTextField(20);
    private final JPasswordField passwordField = new JPasswordField(20);
    private final JLabel messageLabel = new JLabel(" ");
    private final JToggleButton[] roleButtons = new JToggleButton[3];
    private String selectedRole = "学生";

    /** 创建登录窗口。 */
    public LoginFrame() {
        super("vCampus 虚拟校园");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(900, 650));
        setSize(1080, 760);
        setLocationRelativeTo(null);
        setContentPane(createContent());
        ResponsiveTypography.install(this, 1080, 1.2F);
    }

    private JPanel createContent() {
        GradientPanel root = new GradientPanel(new Color(13, 24, 45),
                new Color(32, 28, 48));
        root.setLayout(new GridBagLayout());
        JPanel shell = new JPanel(new GridLayout(1, 2));
        shell.setOpaque(false);
        shell.setPreferredSize(new Dimension(880, 590));
        shell.add(new LoginBrandPanel());
        shell.add(createLoginCard());
        root.add(shell);
        ResponsiveTypography.installScaledSize(this, shell,
                880, 590, 1080, 1.2F);
        return root;
    }

    private JPanel createLoginCard() {
        RoundedPanel card = new RoundedPanel(new GridBagLayout(), 24, UiTheme.SURFACE);
        card.setBorder(BorderFactory.createEmptyBorder(30, 44, 28, 44));
        GridBagConstraints grid = new GridBagConstraints();
        grid.gridx = 0;
        grid.fill = GridBagConstraints.HORIZONTAL;
        grid.weightx = 1;
        grid.insets = new Insets(4, 2, 4, 2);
        RoundedPanel iconBadge = new RoundedPanel(new BorderLayout(), 18,
                new Color(251, 235, 236));
        iconBadge.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        iconBadge.add(new JLabel(UiIcons.load("user", 25)));
        JPanel iconRow = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 0, 0));
        iconRow.setOpaque(false);
        iconRow.add(iconBadge);
        card.add(iconRow, grid);
        grid.gridy = 1;
        JLabel heading = new JLabel("用户登录", SwingConstants.CENTER);
        heading.setForeground(UiTheme.TEXT);
        heading.setFont(UiTheme.font(Font.BOLD, 25F));
        card.add(heading, grid);
        grid.gridy = 2;
        JLabel subtitle = new JLabel("欢迎使用 vCampus 虚拟校园系统", SwingConstants.CENTER);
        subtitle.setForeground(UiTheme.MUTED);
        card.add(subtitle, grid);
        grid.gridy = 3;
        grid.insets = new Insets(18, 2, 8, 2);
        card.add(createRoleSelector(), grid);
        grid.gridy = 4;
        grid.insets = new Insets(12, 2, 5, 2);
        card.add(inputRow("user", userIdField), grid);
        grid.gridy = 5;
        card.add(inputRow("lock", passwordField), grid);
        grid.gridy = 6;
        messageLabel.setForeground(UiTheme.ACCENT);
        card.add(messageLabel, grid);
        grid.gridy = 7;
        grid.insets = new Insets(9, 2, 5, 2);
        JButton loginButton = UiFactory.primaryButton("登  录", "user");
        loginButton.addActionListener(new LoginAction());
        card.add(loginButton, grid);
        grid.gridy = 8;
        grid.insets = new Insets(12, 2, 0, 2);
        card.add(createAccountActions(), grid);
        getRootPane().setDefaultButton(loginButton);
        return card;
    }

    private JPanel createRoleSelector() {
        JPanel panel = new JPanel(new GridLayout(1, 3, 4, 0));
        panel.setBackground(new Color(246, 247, 250));
        panel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        ButtonGroup group = new ButtonGroup();
        String[] roles = {"学生", "教师", "管理员"};
        for (int index = 0; index < roles.length; index++) {
            final String role = roles[index];
            JToggleButton button = new JToggleButton(role);
            button.setFocusPainted(false);
            button.setFont(UiTheme.font(Font.BOLD, 13F));
            button.addActionListener(event -> selectRole(role));
            roleButtons[index] = button;
            group.add(button);
            panel.add(button);
        }
        roleButtons[0].setSelected(true);
        updateRoleColors();
        return panel;
    }

    private JPanel inputRow(String icon, JTextField field) {
        JPanel row = new JPanel(new BorderLayout(9, 0));
        row.setBackground(new Color(250, 250, 252));
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(222, 210, 214)),
                BorderFactory.createEmptyBorder(7, 11, 7, 11)));
        row.add(new JLabel(UiIcons.load(icon, 18)), BorderLayout.WEST);
        field.setBorder(BorderFactory.createEmptyBorder());
        field.setOpaque(false);
        field.setPreferredSize(new Dimension(280, 28));
        row.add(field, BorderLayout.CENTER);
        return row;
    }

    private JPanel createAccountActions() {
        JPanel panel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 14, 0));
        panel.setOpaque(false);
        JButton register = linkButton("注册新用户");
        register.addActionListener(event -> new RegisterDialog(this).setVisible(true));
        JButton password = linkButton("修改密码");
        password.addActionListener(event -> new ChangePasswordDialog(this).setVisible(true));
        panel.add(register);
        panel.add(new JLabel("·"));
        panel.add(password);
        return panel;
    }

    private JButton linkButton(String text) {
        JButton button = new JButton(text);
        button.setForeground(UiTheme.ACCENT_DARK);
        button.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        return button;
    }

    private void selectRole(String role) {
        selectedRole = role;
        updateRoleColors();
    }

    private void updateRoleColors() {
        for (JToggleButton button : roleButtons) {
            boolean selected = button != null && button.isSelected();
            if (button != null) {
                button.setForeground(selected ? UiTheme.ACCENT : UiTheme.MUTED);
                button.setBackground(selected ? Color.WHITE : new Color(246, 247, 250));
            }
        }
    }

    private final class LoginAction implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent event) {
            String userId = userIdField.getText().trim();
            if (userId.length() == 0 || passwordField.getPassword().length == 0) {
                messageLabel.setText("请输入用户 ID 和密码");
                return;
            }
            MainFrame main = new MainFrame(userId, selectedRole);
            if ((getExtendedState() & JFrame.MAXIMIZED_BOTH) == JFrame.MAXIMIZED_BOTH) {
                main.setExtendedState(JFrame.MAXIMIZED_BOTH);
            }
            main.setVisible(true);
            dispose();
        }
    }
}
