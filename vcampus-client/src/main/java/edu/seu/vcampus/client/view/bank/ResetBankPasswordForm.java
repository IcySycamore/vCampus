package edu.seu.vcampus.client.view.bank;

import edu.seu.vcampus.client.view.theme.UiTheme;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;

/** 管理员重置银行密码时使用的两次密码输入表单。 */
final class ResetBankPasswordForm extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 64;

    static final String NEW_PASSWORD_INPUT = "bank-admin-new-password";
    static final String CONFIRM_PASSWORD_INPUT = "bank-admin-confirm-password";

    private final BankPasswordField first = new BankPasswordField();
    private final BankPasswordField confirm = new BankPasswordField();

    ResetBankPasswordForm() {
        super(new GridLayout(2, 1, 0, 14));
        setOpaque(false);
        first.configureInput(NEW_PASSWORD_INPUT, "新银行密码");
        confirm.configureInput(CONFIRM_PASSWORD_INPUT, "确认新银行密码");
        add(row("新银行密码（" + MIN_LENGTH + "至" + MAX_LENGTH + "位）", first));
        add(row("确认新银行密码", confirm));
    }

    char[] newPassword() {
        return first.getPassword();
    }

    char[] confirmation() {
        return confirm.getPassword();
    }

    void clearPasswords() {
        first.clear();
        confirm.clear();
    }

    void setInputsEnabled(boolean enabled) {
        first.setInputEnabled(enabled);
        confirm.setInputEnabled(enabled);
    }

    boolean requestNewPasswordFocus() {
        return first.requestInputFocus();
    }

    JPasswordField newPasswordInput() {
        return first.input();
    }

    JPasswordField confirmationInput() {
        return confirm.input();
    }

    private static JPanel row(String label, BankPasswordField field) {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setOpaque(false);
        JLabel caption = new JLabel(label);
        caption.setFont(UiTheme.font(Font.PLAIN, 13F));
        caption.setForeground(UiTheme.MUTED);
        caption.setLabelFor(field.input());
        panel.add(caption, BorderLayout.NORTH);
        panel.add(field, BorderLayout.CENTER);
        return panel;
    }
}
