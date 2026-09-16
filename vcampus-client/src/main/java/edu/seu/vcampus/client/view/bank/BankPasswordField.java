package edu.seu.vcampus.client.view.bank;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JPasswordField;
import javax.swing.JPanel;

/** 带显示/隐藏切换按钮的银行密码输入框。 */
final class BankPasswordField extends JPanel {
    private static final long serialVersionUID = 1L;
    private final JPasswordField field = new JPasswordField(18);
    private final JButton eye = new JButton("显示");
    private final char echo;
    private boolean showing;
    BankPasswordField() {
        super(new BorderLayout(6, 0));
        char initialEcho = field.getEchoChar();
        echo = initialEcho == 0 ? '*' : initialEcho;
        field.setEchoChar(echo);
        eye.setFocusable(false);
        eye.addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent e) {
            showing = !showing;
            field.setEchoChar(showing ? (char) 0 : echo);
            eye.setText(showing ? "隐藏" : "显示");
        }});
        add(field, BorderLayout.CENTER); add(eye, BorderLayout.EAST);
    }
    char[] getPassword() { return field.getPassword(); }
    void clear() { field.setText(""); }
}
