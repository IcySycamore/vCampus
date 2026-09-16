package edu.seu.vcampus.client.view.bank;

import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.client.bank.BankService;
import edu.seu.vcampus.client.view.UiTasks;
import edu.seu.vcampus.client.view.theme.UiFactory;
import edu.seu.vcampus.client.view.theme.UiTheme;
import edu.seu.vcampus.common.bank.dto.BankAdminAccountView;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

/** 管理员重置他人银行密码的弹窗；新密码明文只留在本机。 */
final class ResetBankPasswordDialog extends JDialog {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 新密码最短长度。 */
    private static final int MIN_LENGTH = 8;

    /** 新密码最长长度。 */
    private static final int MAX_LENGTH = 64;

    private final BankService api;
    private final String username;
    private final ResetBankPasswordForm form = new ResetBankPasswordForm();
    private final JLabel message = new JLabel(" ");
    private final JButton submit = UiFactory.primaryButton("确认重置", "bank");
    private final JButton cancel = UiFactory.secondaryButton("取消", "bank");
    private final Runnable onDone;

    /**
     * 构造弹窗。
     *
     * @param owner 父窗口
     * @param api 银行 API
     * @param account 目标账户
     * @param onDone 重置成功后的回调
     */
    ResetBankPasswordDialog(Window owner, BankService api, BankAdminAccountView account,
            Runnable onDone) {
        super(owner, "重置银行密码", ModalityType.APPLICATION_MODAL);
        if (api == null || account == null) {
            throw new IllegalArgumentException("api and account are required");
        }
        this.api = api;
        this.username = account.getUsername();
        this.onDone = onDone;
        setContentPane(content());
        setMinimumSize(new Dimension(480, 320));
        getRootPane().setDefaultButton(submit);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent event) {
                form.requestNewPasswordFocus();
            }
        });
        pack();
        setResizable(false);
        BankDialogs.centerOnScreen(this);
    }

    private JPanel content() {
        JPanel page = new JPanel(new BorderLayout(0, 16));
        page.setBackground(UiTheme.BACKGROUND);
        page.setBorder(BorderFactory.createEmptyBorder(22, 26, 20, 26));
        JLabel title = new JLabel("为 " + username + "设置新的银行密码");
        title.setFont(UiTheme.font(Font.BOLD, 16F));
        title.setForeground(UiTheme.TEXT);
        page.add(title, BorderLayout.NORTH);

        page.add(form, BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout(0, 10));
        south.setOpaque(false);
        message.setForeground(UiTheme.MUTED);
        message.setFont(UiTheme.font(Font.PLAIN, 12F));
        south.add(message, BorderLayout.NORTH);
        JPanel buttons = new JPanel();
        buttons.setOpaque(false);
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
        cancel.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                dispose();
            }
        });
        submit.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                submit();
            }
        });
        buttons.add(submit);
        buttons.add(javax.swing.Box.createHorizontalStrut(10));
        buttons.add(cancel);
        south.add(buttons, BorderLayout.CENTER);
        page.add(south, BorderLayout.SOUTH);
        return page;
    }

    private void submit() {
        final char[] value = form.newPassword();
        char[] again = form.confirmation();
        String error = validate(value, again);
        Arrays.fill(again, '\0');
        if (error != null) {
            Arrays.fill(value, '\0');
            showError(error);
            return;
        }
        setBusy(true);
        UiTasks.run(createResetTask(api, username, value),
                new UiTasks.Success<BankAdminAccountView>() {
                    @Override
                    public void accept(BankAdminAccountView result) {
                        dispose();
                        if (onDone != null) {
                            onDone.run();
                        }
                    }
                }, new UiTasks.Failure() {
                    @Override
                    public void accept(ApiException failure) {
                        setBusy(false);
                        showError(failure.getMessage());
                    }
                }
        );
    }

    /** 后台请求结束后再清除密码，避免任务启动前清空同一个数组。 */
    static UiTasks.Task<BankAdminAccountView> createResetTask(final BankService api,
            final String username, final char[] password) {
        return new UiTasks.Task<BankAdminAccountView>() {
            @Override
            public BankAdminAccountView run() {
                try {
                    return api.resetPassword(username, password);
                } finally {
                    Arrays.fill(password, '\0');
                }
            }
        };
    }

    private String validate(char[] value, char[] again) {
        if (value.length < MIN_LENGTH || value.length > MAX_LENGTH) {
            return "新银行密码长度需为" + MIN_LENGTH + "至" + MAX_LENGTH + "个字符";
        }
        if (!Arrays.equals(value, again)) {
            return "两次输入的银行密码不一致";
        }
        return null;
    }

    private void setBusy(boolean busy) {
        submit.setEnabled(!busy);
        cancel.setEnabled(!busy);
        form.setInputsEnabled(!busy);
        if (busy) {
            message.setForeground(UiTheme.MUTED);
            message.setText("正在重置银行密码…");
        }
        setDefaultCloseOperation(busy
                ? WindowConstants.DO_NOTHING_ON_CLOSE : WindowConstants.DISPOSE_ON_CLOSE);
    }

    private void showError(String text) {
        message.setForeground(UiTheme.ACCENT);
        message.setText(text);
    }

    @Override
    public void dispose() {
        form.clearPasswords();
        super.dispose();
    }
}
