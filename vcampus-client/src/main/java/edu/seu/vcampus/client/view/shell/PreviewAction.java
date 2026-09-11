package edu.seu.vcampus.client.view.shell;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.JToggleButton;

/** 显式打开离线预览窗口，不创建网络连接或登录会话。 */
final class PreviewAction implements ActionListener {
    private final JFrame loginFrame;
    private final JTextField username;
    private final JToggleButton[] roles;

    PreviewAction(JFrame loginFrame, JTextField username, JToggleButton[] roles) {
        this.loginFrame = loginFrame;
        this.username = username;
        this.roles = roles;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        String name = username.getText().trim();
        String role = "学生";
        for (JToggleButton button : roles) {
            if (button.isSelected()) {
                role = button.getText();
                break;
            }
        }
        MainFrame main = new MainFrame(name.length() == 0 ? "预览用户" : name, role);
        if ((loginFrame.getExtendedState() & JFrame.MAXIMIZED_BOTH) == JFrame.MAXIMIZED_BOTH) {
            main.setExtendedState(JFrame.MAXIMIZED_BOTH);
        }
        main.setVisible(true);
        loginFrame.dispose();
    }
}
