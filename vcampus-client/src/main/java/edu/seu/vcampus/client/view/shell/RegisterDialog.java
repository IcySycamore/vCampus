package edu.seu.vcampus.client.view.shell;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.plaf.basic.BasicComboBoxUI;

/**
 * 新用户注册表单。
 */
public class RegisterDialog extends JDialog {

    private static final long serialVersionUID = 1L;
    private final JTextField userId = new JTextField(18);
    private final JTextField name = new JTextField(18);
    private final JComboBox<String> role = new JComboBox<String>(
            new String[] {"学生", "教师", "管理员"});
    private final JPasswordField password = new JPasswordField(18);
    private final JPasswordField confirmation = new JPasswordField(18);

    /**
     * 创建注册窗口。
     *
     * @param owner 父窗口
     */
    public RegisterDialog(Window owner) {
        super(owner, "注册 vCampus 用户", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(createContent());
        pack();
        setMinimumSize(new Dimension(460, 470));
        setLocationRelativeTo(owner);
    }

    private JPanel createContent() {
        JPanel root = new JPanel(new BorderLayout(0, 22));
        root.setBackground(UiTheme.BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(28, 34, 28, 34));
        JLabel heading = new JLabel("创建新用户");
        heading.setForeground(UiTheme.TEXT);
        heading.setFont(UiTheme.font(Font.BOLD, 24F));
        root.add(heading, BorderLayout.NORTH);
        RoundedPanel form = new RoundedPanel(new GridBagLayout(), 20, UiTheme.SURFACE);
        form.setBorder(BorderFactory.createEmptyBorder(22, 24, 22, 24));
        role.setUI(new BasicComboBoxUI());
        role.setBackground(UiTheme.SURFACE);
        role.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER));
        addRow(form, 0, "用户 ID", userId);
        addRow(form, 1, "姓名", name);
        addRow(form, 2, "身份", role);
        addRow(form, 3, "密码", password);
        addRow(form, 4, "确认密码", confirmation);
        root.add(form, BorderLayout.CENTER);
        JPanel actions = new JPanel(new BorderLayout(10, 0));
        actions.setOpaque(false);
        JButton cancel = UiFactory.secondaryButton("取消", "return");
        cancel.addActionListener(event -> dispose());
        JButton submit = UiFactory.primaryButton("完成注册", "user");
        submit.addActionListener(event -> submit());
        actions.add(cancel, BorderLayout.WEST);
        actions.add(submit, BorderLayout.EAST);
        root.add(actions, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(submit);
        return root;
    }

    private void addRow(JPanel panel, int row, String text, java.awt.Component component) {
        GridBagConstraints grid = new GridBagConstraints();
        grid.gridy = row;
        grid.insets = new Insets(7, 5, 7, 5);
        grid.anchor = GridBagConstraints.WEST;
        JLabel label = new JLabel(text);
        label.setForeground(UiTheme.NAVY);
        label.setFont(UiTheme.font(Font.BOLD, 13F));
        panel.add(label, grid);
        grid.gridx = 1;
        grid.weightx = 1;
        grid.fill = GridBagConstraints.HORIZONTAL;
        if (component instanceof JTextField) {
            styleField((JTextField) component);
        }
        panel.add(component, grid);
    }

    private void styleField(JTextField field) {
        field.setPreferredSize(new Dimension(240, 38));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiTheme.BORDER),
                BorderFactory.createEmptyBorder(7, 10, 7, 10)));
    }

    private void submit() {
        String first = new String(password.getPassword());
        String second = new String(confirmation.getPassword());
        if (userId.getText().trim().length() == 0 || name.getText().trim().length() == 0
                || first.length() == 0) {
            showError("请完整填写注册信息");
            return;
        }
        if (!first.equals(second)) {
            showError("两次输入的密码不一致");
            return;
        }
        JOptionPane.showMessageDialog(this, "注册信息已提交，请使用新用户登录",
                "注册成功", JOptionPane.INFORMATION_MESSAGE);
        dispose();
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "无法注册", JOptionPane.WARNING_MESSAGE);
    }
}
