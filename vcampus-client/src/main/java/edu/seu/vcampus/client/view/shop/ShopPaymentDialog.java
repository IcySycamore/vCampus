package edu.seu.vcampus.client.view.shop;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.Arrays;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;

/** Shop 支付密码输入框。 */
final class ShopPaymentDialog {

    private ShopPaymentDialog() {
    }

    /**
     * 请求银行密码。
     *
     * @param parent 父组件
     * @param totalText 本次待支付金额文本
     * @return 密码；取消时返回 null
     */
    static char[] request(Component parent, String totalText) {
        JPasswordField passwordField = new JPasswordField(20);
        passwordField.getAccessibleContext().setAccessibleName("银行密码");
        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.add(new JLabel(totalText), BorderLayout.NORTH);
        JPanel passwordRow = new JPanel(new BorderLayout(8, 0));
        JLabel passwordLabel = new JLabel("银行密码");
        passwordLabel.setLabelFor(passwordField);
        passwordRow.add(passwordLabel, BorderLayout.WEST);
        passwordRow.add(passwordField, BorderLayout.CENTER);
        content.add(passwordRow, BorderLayout.CENTER);

        int option = JOptionPane.showConfirmDialog(parent, content, "确认支付",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (option != JOptionPane.OK_OPTION) {
            char[] cancelled = passwordField.getPassword();
            Arrays.fill(cancelled, '\0');
            passwordField.setText("");
            return null;
        }
        char[] password = passwordField.getPassword();
        passwordField.setText("");
        return password;
    }
}
