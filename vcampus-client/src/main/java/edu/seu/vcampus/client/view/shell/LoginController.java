package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.client.auth.ClientSession;
import edu.seu.vcampus.client.auth.SessionCleanup;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingWorker;

/** 在后台完成登录，并把同一个已认证连接移交主窗口。 */
final class LoginController implements ActionListener {
    private final LoginFrame frame;
    private final JTextField username;
    private final JPasswordField password;
    private final JLabel message;
    private final JToggleButton[] roles;
    private ClientSession pending;

    LoginController(LoginFrame frame, JTextField username, JPasswordField password,
            JLabel message, JToggleButton[] roles) {
        this.frame = frame;
        this.username = username;
        this.password = password;
        this.message = message;
        this.roles = roles;
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                SessionCleanup.closeAsync(pending);
            }
            @Override
            public void windowClosed(WindowEvent event) {
                SessionCleanup.closeAsync(pending);
            }
        });
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        if (pending != null) {
            return;
        }
        final String user = username.getText().trim();
        final char[] secret = password.getPassword();
        if (user.length() == 0 || secret.length == 0) {
            Arrays.fill(secret, '\0');
            message.setText("请输入用户 ID 和密码");
            return;
        }
        final ClientSession session;
        try {
            session = new ClientSession(System.getProperty("vcampus.server.host", "127.0.0.1"),
                    Integer.parseInt(System.getProperty("vcampus.server.port", "8888")));
        } catch (IllegalArgumentException exception) {
            Arrays.fill(secret, '\0');
            message.setText("服务器地址或端口配置不正确");
            return;
        }
        final String role = selectedRole();
        pending = session;
        password.setText("");
        setBusy(true);
        message.setText("正在连接并验证身份…");
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                session.login(user, secret, role);
                return null;
            }

            @Override
            protected void done() {
                pending = null;
                if (!frame.isDisplayable()) {
                    SessionCleanup.closeAsync(session);
                    return;
                }
                try {
                    get();
                    MainFrame main = new MainFrame(session);
                    if ((frame.getExtendedState() & JFrame.MAXIMIZED_BOTH)
                            == JFrame.MAXIMIZED_BOTH) {
                        main.setExtendedState(JFrame.MAXIMIZED_BOTH);
                    }
                    main.setVisible(true);
                    frame.dispose();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    SessionCleanup.closeAsync(session);
                    message.setText("登录已取消");
                } catch (ExecutionException exception) {
                    message.setText(errorMessage(exception.getCause()));
                } finally {
                    setBusy(false);
                }
            }
        }.execute();
    }

    private String selectedRole() {
        for (JToggleButton button : roles) {
            if (button.isSelected()) {
                return button.getText();
            }
        }
        return "学生";
    }

    private void setBusy(boolean busy) {
        frame.getRootPane().getDefaultButton().setEnabled(!busy);
        username.setEnabled(!busy);
        password.setEnabled(!busy);
        for (JToggleButton button : roles) {
            button.setEnabled(!busy);
        }
    }

    private String errorMessage(Throwable cause) {
        if (cause instanceof SocketException || cause instanceof SocketTimeoutException) {
            return "无法连接服务器，请确认服务器已启动";
        }
        return cause.getMessage() == null ? "登录失败，请稍后重试" : cause.getMessage();
    }
}
