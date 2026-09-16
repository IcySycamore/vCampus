package edu.seu.vcampus.client.view.bank;

import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.client.api.ApiErrors;
import edu.seu.vcampus.client.bank.BankService;
import edu.seu.vcampus.client.view.UiTasks;
import edu.seu.vcampus.client.view.theme.UiFactory;
import edu.seu.vcampus.client.view.theme.UiTheme;
import edu.seu.vcampus.common.bank.dto.BankRechargeResponse;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

/** 充值弹窗：演示来源卡输入、快捷金额、提交状态和行内失败提示。 */
public class RechargeDialog extends JDialog {
    private static final long serialVersionUID = 1L;
    private final BankService api;
    private final UiTasks.Success<BankRechargeResponse> onSuccess;
    private final JTextField sourceCard = new JTextField(22);
    private final JPasswordField sourcePassword = new JPasswordField(22);
    private final JTextField amount = new JTextField(16);
    private final JLabel message = new JLabel("金额最多保留两位小数");
    private final JButton submit = UiFactory.primaryButton("确认充值", "bank");
    private final JButton cancel = new JButton("取消");
    private final List<JButton> shortcuts = new ArrayList<JButton>();

    /**
     * 创建充值弹窗。
     * @param owner 父窗口
     * @param api 银行 API
     * @param onSuccess 充值成功后刷新账户的回调
     */
    public RechargeDialog(Window owner, BankService api,
            UiTasks.Success<BankRechargeResponse> onSuccess) {
        super(owner, "账户充值", ModalityType.APPLICATION_MODAL);
        if (api == null || onSuccess == null) {
            throw new IllegalArgumentException("api and callback are required");
        }
        this.api = api;
        this.onSuccess = onSuccess;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(createContent());
        setMinimumSize(new Dimension(520, 560));
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
        getRootPane().setDefaultButton(submit);
    }

    private JPanel createContent() {
        JPanel root = new JPanel(new BorderLayout(0, 22));
        root.setBackground(UiTheme.BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(26, 28, 24, 28));
        JLabel title = new JLabel("为校园账户充值");
        title.setFont(UiTheme.font(Font.BOLD, 23F));
        title.setForeground(UiTheme.TEXT);
        root.add(title, BorderLayout.NORTH);
        JPanel form = new JPanel(new GridLayout(8, 1, 0, 12));
        form.setOpaque(false);

        JLabel cardLabel = new JLabel("来源银行卡号");
        cardLabel.setLabelFor(sourceCard);
        sourceCard.getAccessibleContext().setAccessibleName("来源银行卡号");
        form.add(cardLabel);
        form.add(sourceCard);
        JLabel passwordLabel = new JLabel("来源银行卡密码");
        passwordLabel.setLabelFor(sourcePassword);
        sourcePassword.getAccessibleContext().setAccessibleName("来源银行卡密码");
        form.add(passwordLabel);
        form.add(sourcePassword);
        form.add(new JLabel("充值金额 / 元"));
        amount.setFont(UiTheme.font(Font.BOLD, 24F));
        amount.getAccessibleContext().setAccessibleName("充值金额");
        form.add(amount);
        JPanel quick = new JPanel(new GridLayout(1, 4, 8, 0));
        quick.setOpaque(false);
        for (final String value : new String[] {"10", "50", "100", "200"}) {
            JButton button = new JButton(value + " 元");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    amount.setText(value);
                    amount.requestFocusInWindow();
                }
            });
            shortcuts.add(button);
            quick.add(button);
        }
        form.add(quick);
        message.setForeground(UiTheme.MUTED);
        form.add(message);
        root.add(form, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        cancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                dispose();
            }
        });
        submit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                submit();
            }
        });
        actions.add(cancel);
        actions.add(submit);
        root.add(actions, BorderLayout.SOUTH);
        return root;
    }

    private void submit() {
        if (!submit.isEnabled()) { return; }
        final String text = amount.getText();
        sourcePassword.setText("");
        setBusy(true);
        BankConnectionDialog.show(this, sourceCard.getText(), new Runnable() {
            @Override
            public void run() { performRecharge(text); }
        });
    }

    private void performRecharge(final String text) {
        UiTasks.run(new UiTasks.Task<BankRechargeResponse>() {
            @Override
            public BankRechargeResponse run() {
                return api.recharge(RechargeAmount.parse(text));
            }
        }, new UiTasks.Success<BankRechargeResponse>() {
            @Override
            public void accept(BankRechargeResponse result) {
                dispose();
                onSuccess.accept(result);
            }
        }, new UiTasks.Failure() {
            @Override
            public void accept(ApiException error) {
                setBusy(false);
                message.setForeground(UiTheme.ACCENT);
                message.setText(error.getMessage());
                if (ApiErrors.LOCAL_TIMEOUT.equals(error.getStatusCode())
                        || ApiErrors.LOCAL_NETWORK.equals(error.getStatusCode())
                        || ApiErrors.LOCAL_MALFORMED.equals(error.getStatusCode())) {
                    submit.setEnabled(false);
                    message.setText("充值结果未确认，请关闭并刷新账户后再操作");
                }
            }
        });
    }

    @Override
    public void dispose() {
        sourceCard.setText("");
        sourcePassword.setText("");
        super.dispose();
    }

    private void setBusy(boolean busy) {
        submit.setEnabled(!busy);
        submit.setText(busy ? "正在提交…" : "确认充值");
        cancel.setEnabled(!busy);
        amount.setEnabled(!busy);
        sourceCard.setEnabled(!busy);
        sourcePassword.setEnabled(!busy);
        for (JButton button : shortcuts) {
            button.setEnabled(!busy);
        }
        setDefaultCloseOperation(busy ? DO_NOTHING_ON_CLOSE : DISPOSE_ON_CLOSE);
        message.setForeground(UiTheme.MUTED);
        message.setText(busy ? "正在处理充值，请稍候" : "金额最多保留两位小数");
    }
}
