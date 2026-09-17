package edu.seu.vcampus.client.view.bank;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JPasswordField;
import javax.swing.JPanel;

/** 带显示/隐藏切换按钮的银行密码输入框。 */
final class BankPasswordField extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final int MINIMUM_INPUT_HEIGHT = 36;
    private final JPasswordField field = new JPasswordField(18);
    private final JButton eye = new JButton("显示");
    private final char echo;
    private boolean showing;
    BankPasswordField() {
        super(new BorderLayout(6, 0));
        setOpaque(false);
        // BoxLayout里按左对齐摆放，避免密码框被水平居中而与左侧标题错位。
        setAlignmentX(Component.LEFT_ALIGNMENT);
        Dimension preferred = field.getPreferredSize();
        field.setPreferredSize(new Dimension(preferred.width,
                Math.max(preferred.height, MINIMUM_INPUT_HEIGHT)));
        field.setMinimumSize(new Dimension(120, MINIMUM_INPUT_HEIGHT));
        char initialEcho = field.getEchoChar();
        echo = initialEcho == 0 ? '*' : initialEcho;
        field.setEchoChar(echo);
        eye.setFocusable(false);
        eye.getAccessibleContext().setAccessibleName("显示或隐藏密码");
        eye.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                showing = !showing;
                field.setEchoChar(showing ? (char) 0 : echo);
                eye.setText(showing ? "隐藏" : "显示");
            }
        });
        add(field, BorderLayout.CENTER);
        add(eye, BorderLayout.EAST);
    }

    /** 为具体表单设置测试标识和无障碍名称。 */
    void configureInput(String name, String accessibleName) {
        field.setName(name);
        field.getAccessibleContext().setAccessibleName(accessibleName);
    }

    char[] getPassword() { return field.getPassword(); }
    void clear() { field.setText(""); }

    /** @return 实际接收键盘输入的密码框。 */
    JPasswordField input() { return field; }

    /** 请求把键盘焦点放到实际密码框，而不是外层面板。 */
    boolean requestInputFocus() { return field.requestFocusInWindow(); }

    /** 提交过程中同时锁定输入框和显隐按钮。 */
    void setInputEnabled(boolean enabled) {
        field.setEnabled(enabled);
        eye.setEnabled(enabled);
    }
}
