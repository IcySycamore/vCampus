package edu.seu.vcampus.client.view.bank;

import edu.seu.vcampus.client.view.theme.UiFactory;
import edu.seu.vcampus.client.bank.BankService;
import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.client.view.UiTasks;
import edu.seu.vcampus.common.bank.dto.BankAccountResponse;
import edu.seu.vcampus.client.view.theme.UiTheme;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

/** 开户表单：校园账号、密码输入及格式反馈；通过银行 API 提交开户。 */
public final class OpenAccountDialog extends JDialog {
    private static final long serialVersionUID = 1L;
    private final BankService api;
    private final Runnable onOpened;
    private final JTextField name = new JTextField(22);
    private final JPasswordField login = new JPasswordField(22);
    private final JPasswordField password = new JPasswordField(22);
    private final JPasswordField confirmation = new JPasswordField(22);
    private final JLabel message = new JLabel("银行密码仅用于付款扣款，请妥善保管");
    private final JButton submit = UiFactory.primaryButton("确认开户", "bank");
    private final JButton cancel = new JButton("取消");

    /**
     * 创建开户弹窗。
     * @param owner 父窗口
     * @param api 银行 API
     * @param onOpened 开户后刷新页面
     */
    public OpenAccountDialog(Window owner, BankService api, Runnable onOpened) {
        super(owner, "开通校园银行账户", ModalityType.APPLICATION_MODAL);
        this.api = api;
        this.onOpened = onOpened;
        name.setText(api.currentUsername());
        setContentPane(content());
        setMinimumSize(new Dimension(520, 560));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getRootPane().setDefaultButton(submit);
        pack();
        setResizable(false);
        BankDialogs.centerOnScreen(this);
    }

    private JPanel content() {
        JPanel root = new JPanel(new BorderLayout(0, 20));
        root.setBackground(UiTheme.BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(26, 30, 24, 30));
        JPanel heading = new JPanel(new BorderLayout(0, 8));
        heading.setOpaque(false);
        JLabel title = new JLabel("开通你的校园银行账户");
        title.setFont(UiTheme.font(Font.BOLD, 23F));
        title.setForeground(UiTheme.NAVY);
        heading.add(title, BorderLayout.NORTH);
        JLabel hint = new JLabel("验证校园身份 · 设置独立银行密码");
        hint.setForeground(UiTheme.MUTED);
        heading.add(hint, BorderLayout.SOUTH);
        root.add(heading, BorderLayout.NORTH);
        JPanel fields = new JPanel(new GridLayout(4, 1, 0, 16));
        fields.setOpaque(false);
        fields.add(field("校园账号（当前登录账号）", name));
        fields.add(field("校园登录密码", login));
        fields.add(field("设置银行密码（8–64 个字符）", password));
        fields.add(field("确认银行密码", confirmation));
        root.add(fields, BorderLayout.CENTER);
        JPanel footer = new JPanel(new BorderLayout(0, 16));
        footer.setOpaque(false);
        message.setForeground(UiTheme.MUTED);
        footer.add(message, BorderLayout.NORTH);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        cancel.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent event) { dispose(); }
        });
        submit.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent event) { submit(); }
        });
        actions.add(cancel);
        actions.add(submit);
        footer.add(actions, BorderLayout.SOUTH);
        root.add(footer, BorderLayout.SOUTH);
        return root;
    }

    private JPanel field(String label, JComponent input) {
        JPanel row = new JPanel(new BorderLayout(0, 6));
        row.setOpaque(false);
        JLabel title = new JLabel(label);
        title.setLabelFor(input);
        row.add(title, BorderLayout.NORTH);
        input.setPreferredSize(new Dimension(400, 36));
        input.getAccessibleContext().setAccessibleName(label);
        row.add(input, BorderLayout.CENTER);
        return row;
    }

    private void submit() {
        final String username = name.getText().trim();
        final char[] loginValue = login.getPassword();
        final char[] bankValue = password.getPassword();
        char[] confirmValue = confirmation.getPassword();
        String error = BankOpeningInput.validationMessage(username,
                loginValue, bankValue, confirmValue);
        Arrays.fill(confirmValue, '\0');
        if (error != null) {
            Arrays.fill(loginValue, '\0');
            Arrays.fill(bankValue, '\0');
            message.setText(error);
            return;
        }
        setBusy(true);
        UiTasks.run(new UiTasks.Task<BankAccountResponse>() {
            @Override public BankAccountResponse run() {
                try { return api.openAccount(username, loginValue, bankValue); }
                finally {
                    Arrays.fill(loginValue, '\0');
                    Arrays.fill(bankValue, '\0');
                }
            }
        }, new UiTasks.Success<BankAccountResponse>() {
            @Override public void accept(BankAccountResponse result) {
                dispose();
                onOpened.run();
            }
        }, new UiTasks.Failure() {
            @Override public void accept(ApiException error) {
                setBusy(false);
                message.setForeground(UiTheme.ACCENT);
                message.setText(error.getMessage());
                login.setText("");
                password.setText("");
                confirmation.setText("");
            }
        });
    }
    private void setBusy(boolean busy) {
        submit.setEnabled(!busy);
        cancel.setEnabled(!busy);
        name.setEnabled(!busy);
        login.setEnabled(!busy);
        password.setEnabled(!busy);
        confirmation.setEnabled(!busy);
        submit.setText(busy ? "正在验证并开户…" : "确认开户");
        setDefaultCloseOperation(busy ? DO_NOTHING_ON_CLOSE : DISPOSE_ON_CLOSE);
    }

    @Override
    public void dispose() {
        login.setText("");
        password.setText("");
        confirmation.setText("");
        super.dispose();
    }
}
