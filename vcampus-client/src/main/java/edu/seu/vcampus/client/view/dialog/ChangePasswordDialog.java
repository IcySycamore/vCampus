package edu.seu.vcampus.client.view.dialog;

import edu.seu.vcampus.client.view.component.RoundedPanel;
import edu.seu.vcampus.client.view.component.FormFieldPanel;
import edu.seu.vcampus.client.view.theme.UiFactory;
import edu.seu.vcampus.client.view.theme.UiTheme;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

/**
 * 用户修改密码表单。
 */
public class ChangePasswordDialog extends JDialog {

    private static final long serialVersionUID = 1L;
    private final JTextField userId = new JTextField(18);
    private final JPasswordField oldPassword = new JPasswordField(18);
    private final JPasswordField newPassword = new JPasswordField(18);
    private final JPasswordField confirmation = new JPasswordField(18);

    /**
     * 创建修改密码窗口。
     *
     * @param owner 父窗口
     */
    public ChangePasswordDialog(Window owner) {
        super(owner, "修改密码", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(createContent());
        pack();
        setMinimumSize(new Dimension(450, 420));
        setLocationRelativeTo(owner);
    }

    private JPanel createContent() {
        JPanel root = new JPanel(new BorderLayout(0, 22));
        root.setBackground(UiTheme.BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(28, 34, 28, 34));
        JLabel heading = new JLabel("修改登录密码");
        heading.setForeground(UiTheme.TEXT);
        heading.setFont(UiTheme.font(Font.BOLD, 24F));
        root.add(heading, BorderLayout.NORTH);
        RoundedPanel form = new RoundedPanel(new GridBagLayout(), 20, UiTheme.SURFACE);
        form.setBorder(BorderFactory.createEmptyBorder(22, 24, 22, 24));
        new FormFieldPanel("用户 ID", userId, 235).addTo(form, 0);
        new FormFieldPanel("原密码", oldPassword, 235).addTo(form, 1);
        new FormFieldPanel("新密码", newPassword, 235).addTo(form, 2);
        new FormFieldPanel("确认新密码", confirmation, 235).addTo(form, 3);
        root.add(form, BorderLayout.CENTER);
        JButton submit = UiFactory.primaryButton("确认修改", "user");
        submit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                submit();
            }
        });
        root.add(submit, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(submit);
        return root;
    }

    private void submit() {
        String oldValue = new String(oldPassword.getPassword());
        String newValue = new String(newPassword.getPassword());
        String confirmationValue = new String(confirmation.getPassword());
        if (userId.getText().trim().length() == 0 || oldValue.length() == 0
                || newValue.length() == 0) {
            showError("请完整填写密码信息");
            return;
        }
        if (oldValue.equals(newValue)) {
            showError("新密码不能与原密码相同");
            return;
        }
        if (!newValue.equals(confirmationValue)) {
            showError("两次输入的新密码不一致");
            return;
        }
        JOptionPane.showMessageDialog(this, "密码修改请求已提交，请重新登录",
                "修改成功", JOptionPane.INFORMATION_MESSAGE);
        dispose();
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "无法修改", JOptionPane.WARNING_MESSAGE);
    }
}
