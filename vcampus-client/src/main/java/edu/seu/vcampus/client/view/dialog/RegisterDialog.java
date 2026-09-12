package edu.seu.vcampus.client.view.dialog;

import edu.seu.vcampus.client.user.AuthException;
import edu.seu.vcampus.client.user.UserService;
import edu.seu.vcampus.client.view.component.RoundedPanel;
import edu.seu.vcampus.client.view.component.FormFieldPanel;
import edu.seu.vcampus.client.view.theme.UiFactory;
import edu.seu.vcampus.client.view.theme.UiTheme;

import java.awt.BorderLayout;
import java.io.IOException;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
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

    /** 用户服务；为 null 时只做本地校验（登录前的入口拿不到会话）。 */
    private final UserService service;

    /**
     * 创建注册窗口（不接服务端，仅本地校验）。
     *
     * @param owner 父窗口
     */
    public RegisterDialog(Window owner) {
        this(owner, null);
    }

    /**
     * 创建注册窗口。
     *
     * <p>
     * 传入 {@code service} 时点「完成注册」会真的调服务端（需管理员会话），姓名随之入库；
     * 传 null 时保持原有的本地校验行为，避免登录前拿不到会话的入口因报错而不可用。
     *
     * @param owner 父窗口
     * @param service 用户服务；可为 null
     */
    public RegisterDialog(Window owner, UserService service) {
        super(owner, "注册 vCampus 用户", ModalityType.APPLICATION_MODAL);
        this.service = service;
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
        new FormFieldPanel("用户 ID", userId, 240).addTo(form, 0);
        new FormFieldPanel("姓名", name, 240).addTo(form, 1);
        new FormFieldPanel("身份", role, 240).addTo(form, 2);
        new FormFieldPanel("密码", password, 240).addTo(form, 3);
        new FormFieldPanel("确认密码", confirmation, 240).addTo(form, 4);
        root.add(form, BorderLayout.CENTER);
        JPanel actions = new JPanel(new BorderLayout(10, 0));
        actions.setOpaque(false);
        JButton cancel = UiFactory.secondaryButton("取消", "return");
        cancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                dispose();
            }
        });
        JButton submit = UiFactory.primaryButton("完成注册", "user");
        submit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                submit();
            }
        });
        actions.add(cancel, BorderLayout.WEST);
        actions.add(submit, BorderLayout.EAST);
        root.add(actions, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(submit);
        return root;
    }

    private void submit() {
        final String id = userId.getText().trim();
        final String realName = name.getText().trim();
        final String selectedRole = (String) role.getSelectedItem();
        String first = new String(password.getPassword());
        String second = new String(confirmation.getPassword());
        if (id.length() == 0 || realName.length() == 0 || first.length() == 0) {
            showError("请完整填写注册信息");
            return;
        }
        if (!first.equals(second)) {
            showError("两次输入的密码不一致");
            return;
        }
        if (service == null) {
            // 登录前的入口拿不到管理员会话：只做本地校验，不发请求
            JOptionPane.showMessageDialog(this, "注册信息已提交，请使用新用户登录",
                    "注册成功", JOptionPane.INFORMATION_MESSAGE);
            dispose();
            return;
        }
        submitToServer(id, selectedRole, first, realName);
    }

    /**
     * 后台线程提交注册：姓名一并送服务端入库。
     *
     * <p>
     * 网络调用不能放在 EDT 上，否则界面会在请求期间整块卡死；回调统一经
     * {@link SwingUtilities#invokeLater} 回到 EDT 再动控件。
     *
     * @param id 登录名
     * @param selectedRole 角色显示名
     * @param secret 明文密码
     * @param realName 真实姓名
     */
    private void submitToServer(final String id, final String selectedRole,
            final String secret, final String realName) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    service.register(id, selectedRole, secret, realName);
                    succeed();
                } catch (AuthException e) {
                    showErrorLater("注册失败：" + e.getStatusCode());
                } catch (IOException e) {
                    showErrorLater("无法连接服务器：" + e.getMessage());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    showErrorLater("注册被中断");
                }
            }
        }, "vcampus-register").start();
    }

    private void succeed() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JOptionPane.showMessageDialog(RegisterDialog.this,
                        "注册信息已提交，请使用新用户登录", "注册成功",
                        JOptionPane.INFORMATION_MESSAGE);
                dispose();
            }
        });
    }

    /** 后台线程专用的错误提示：先切回 EDT 再弹窗。 */
    private void showErrorLater(final String message) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                showError(message);
            }
        });
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "无法注册", JOptionPane.WARNING_MESSAGE);
    }
}
