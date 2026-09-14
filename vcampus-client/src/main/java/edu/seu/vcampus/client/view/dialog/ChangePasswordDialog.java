package edu.seu.vcampus.client.view.dialog;

import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.client.user.UserService;
import edu.seu.vcampus.client.view.UiTasks;
import edu.seu.vcampus.client.view.theme.UiTheme;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
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

/**
 * 修改密码对话框（本人改密走挑战-应答校验旧密码，明文不上线）。
 *
 * <p>
 * 原先这段逻辑长在「用户中心」页里；用户中心页下线后它随账户弹窗走，因此抽成独立对话框。
 * 输入校验是纯函数 {@link #validate(String, String, String)}，可单测。
 */
public class ChangePasswordDialog extends JDialog {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 用户管理 API。 */
    private final UserService m_api;

    /** 原密码输入框。 */
    private final JPasswordField m_old = new JPasswordField(12);

    /** 新密码输入框。 */
    private final JPasswordField m_new = new JPasswordField(12);

    /** 确认新密码输入框。 */
    private final JPasswordField m_confirm = new JPasswordField(12);

    /**
     * 构造修改密码对话框。
     *
     * @param owner 宿主窗口
     * @param api   用户管理 API
     * @throws IllegalArgumentException api 为 null
     */
    public ChangePasswordDialog(Window owner, UserService api) {
        super(owner, "修改密码", ModalityType.APPLICATION_MODAL);
        if (api == null) {
            throw new IllegalArgumentException("api must not be null");
        }
        this.m_api = api;
        setLayout(new BorderLayout(0, 12));
        getContentPane().setBackground(UiTheme.BACKGROUND);
        add(createForm(), BorderLayout.CENTER);
        add(createActions(), BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(owner);
    }

    private JPanel createForm() {
        JPanel form = new JPanel(new GridLayout(3, 2, 10, 10));
        form.setOpaque(false);
        form.setBorder(BorderFactory.createEmptyBorder(18, 20, 4, 20));
        form.add(new JLabel("原密码"));
        form.add(m_old);
        form.add(new JLabel("新密码"));
        form.add(m_new);
        form.add(new JLabel("确认新密码"));
        form.add(m_confirm);
        return form;
    }

    private JPanel createActions() {
        JPanel actions = new JPanel(new BorderLayout());
        actions.setOpaque(false);
        actions.setBorder(BorderFactory.createEmptyBorder(0, 20, 16, 20));
        JLabel hint = new JLabel("修改成功后需使用新密码重新登录");
        hint.setForeground(UiTheme.MUTED);
        hint.setFont(UiTheme.font(Font.PLAIN, 11F));
        actions.add(hint, BorderLayout.WEST);
        JPanel buttons = new JPanel(new GridLayout(1, 2, 8, 0));
        buttons.setOpaque(false);
        JButton cancel = new JButton("取消");
        cancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                dispose();
            }
        });
        JButton submit = new JButton("确定");
        submit.setPreferredSize(new Dimension(96, 32));
        submit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                submit();
            }
        });
        buttons.add(cancel);
        buttons.add(submit);
        actions.add(buttons, BorderLayout.EAST);
        return actions;
    }

    /**
     * 校验两次输入（纯函数，便于单测）。
     *
     * @param oldPassword     原密码
     * @param newPassword     新密码
     * @param confirmation    确认新密码
     * @return 错误文案；全部通过返回 null
     */
    public static String validate(String oldPassword, String newPassword, String confirmation) {
        if (oldPassword == null || oldPassword.length() == 0 || newPassword == null
                || newPassword.length() == 0) {
            return "请完整填写密码";
        }
        if (!newPassword.equals(confirmation)) {
            return "两次输入的新密码不一致";
        }
        if (newPassword.equals(oldPassword)) {
            return "新密码不能与原密码相同";
        }
        return null;
    }

    /** 提交修改：先本地校验，再后台调用 API。 */
    private void submit() {
        final String oldValue = new String(m_old.getPassword());
        final String newValue = new String(m_new.getPassword());
        String error = validate(oldValue, newValue, new String(m_confirm.getPassword()));
        if (error != null) {
            JOptionPane.showMessageDialog(this, error, "无法修改", JOptionPane.WARNING_MESSAGE);
            return;
        }
        UiTasks.run(new UiTasks.Task<Void>() {
            @Override
            public Void run() {
                m_api.changePassword(oldValue, newValue);
                return null;
            }
        }, new UiTasks.Success<Void>() {
            @Override
            public void accept(Void ignored) {
                JOptionPane.showMessageDialog(ChangePasswordDialog.this, "密码已修改，下次请使用新密码登录",
                        "修改成功", JOptionPane.INFORMATION_MESSAGE);
                dispose();
            }
        }, new UiTasks.Failure() {
            @Override
            public void accept(ApiException error) {
                JOptionPane.showMessageDialog(ChangePasswordDialog.this, error.getMessage(), "修改失败",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
