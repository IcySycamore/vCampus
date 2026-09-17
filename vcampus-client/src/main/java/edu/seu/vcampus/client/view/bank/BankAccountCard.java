package edu.seu.vcampus.client.view.bank;

import edu.seu.vcampus.client.view.component.RoundedPanel;
import edu.seu.vcampus.client.view.theme.UiFactory;
import edu.seu.vcampus.client.view.theme.UiTheme;
import edu.seu.vcampus.common.bank.dto.BankAccountResponse;
import edu.seu.vcampus.common.bank.entity.BankAccountStatus;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.BoxLayout;
import javax.swing.Box;
import java.text.SimpleDateFormat;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** 银行账户卡：独立呈现加载、未开户和已开户状态。 */
final class BankAccountCard extends RoundedPanel {
    private static final long serialVersionUID = 1L;
    final JButton action = UiFactory.primaryButton("开通账户", "bank");
    final JButton freeze = UiFactory.secondaryButton("主动挂失", "bank");
    final JButton unfreeze = UiFactory.secondaryButton("解除挂失", "bank");
    final JButton changePassword = UiFactory.secondaryButton("修改密码", "bank");
    private final JLabel eyebrow = label("我的校园账户", 13, new Color(184, 204, 218));
    private final JLabel balance = label("— —", 40, Color.WHITE);
    private final JLabel accountId = label("正在读取账户信息", 12, new Color(184, 204, 218));
    private final JLabel state = label("查询中", 12, Color.WHITE);
    private final JLabel date = label(" ", 12, new Color(184, 204, 218));
    private BankAccountResponse account;
    private boolean canOpen;

    BankAccountCard() {
        super(new BorderLayout(24, 0), 22, UiTheme.NAVY);
        setBorder(BorderFactory.createEmptyBorder(22, 26, 22, 26));
        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setOpaque(false);
        text.add(eyebrow);
        text.add(Box.createVerticalStrut(10));
        text.add(balance);
        text.add(Box.createVerticalStrut(12));
        text.add(accountId);
        add(text, BorderLayout.CENTER);
        JPanel right = new JPanel(new BorderLayout(0, 18));
        right.setOpaque(false);
        state.setHorizontalAlignment(JLabel.RIGHT);
        right.add(state, BorderLayout.NORTH);
        date.setHorizontalAlignment(JLabel.RIGHT);
        right.add(date, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        actions.setOpaque(false);
        actions.add(action);
        actions.add(freeze);
        actions.add(unfreeze);
        actions.add(changePassword);
        right.add(actions, BorderLayout.SOUTH);
        add(right, BorderLayout.EAST);
        action.setEnabled(false);
        freeze.setVisible(false);
        unfreeze.setVisible(false);
        changePassword.setVisible(false);
    }

    void showAccount(BankAccountResponse value) {
        account = value;
        canOpen = false;
        eyebrow.setText("账户余额 · 人民币");
        balance.setText("¥ " + BankTableModels.money(value.getBalance()));
        balance.setFont(UiTheme.font(Font.BOLD, 40F));
        accountId.setText("账户号  " + value.getAccountId());
        state.setText("●  " + value.getStatus().getDisplayName());
        state.setForeground(value.getStatus() == BankAccountStatus.NORMAL
                ? new Color(131, 225, 190) : new Color(255, 202, 130));
        date.setText(value.getCreatedAt() == null ? " " : "开户于 "
                + new SimpleDateFormat("yyyy.MM.dd").format(value.getCreatedAt()));
        action.setText("账户充值");
        boolean frozen = value.getStatus() == BankAccountStatus.FROZEN;
        freeze.setVisible(!frozen);
        unfreeze.setVisible(frozen);
        changePassword.setVisible(true);
    }

    void showUnopened() {
        account = null;
        canOpen = true;
        eyebrow.setText("开启你的校园账户");
        balance.setText("校园生活，从这里开始");
        balance.setFont(UiTheme.font(Font.BOLD, 26F));
        accountId.setText("填写校园账号、登录密码与独立银行密码");
        state.setText("尚未开户");
        date.setText("银行密码用于付款与扣款");
        action.setText("开通账户");
        freeze.setVisible(false);
        unfreeze.setVisible(false);
        changePassword.setVisible(false);
    }

    void showUnavailable() {
        account = null;
        canOpen = false;
        balance.setText("— —");
        accountId.setText("账户暂不可用，请刷新重试");
        state.setText("读取失败");
        date.setText(" ");
    }

    void setBusy(boolean busy) {
        action.setEnabled(!busy && (canOpen
                || account != null && account.getStatus() == BankAccountStatus.NORMAL));
        freeze.setEnabled(!busy && account != null && account.getStatus() == BankAccountStatus.NORMAL);
        unfreeze.setEnabled(!busy && account != null && account.getStatus() == BankAccountStatus.FROZEN);
    }

    boolean isUnopened() {
        return canOpen;
    }

    private static JLabel label(String text, int size, Color color) {
        JLabel result = new JLabel(text);
        result.setForeground(color);
        result.setFont(UiTheme.font(Font.PLAIN, (float) size));
        return result;
    }
}
