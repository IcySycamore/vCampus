package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.client.VCampusClientApp;
import edu.seu.vcampus.client.user.AuthException;
import edu.seu.vcampus.client.user.UserService;
import edu.seu.vcampus.common.constant.NetworkConstant;
import edu.seu.vcampus.common.constant.StatusCode;

import java.io.IOException;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

/**
 * 登录流程：后台线程建连并完成挑战-应答登录；成功打开主窗口，失败在登录面板提示。
 */
public final class LoginFlow {

    /** 登录窗口。 */
    private final LoginFrame frame;

    /** 登录提示标签。 */
    private final JLabel messageLabel;

    /**
     * 构造登录流程。
     *
     * @param frame 登录窗口
     * @param messageLabel 登录提示标签
     */
    public LoginFlow(LoginFrame frame, JLabel messageLabel) {
        this.frame = frame;
        this.messageLabel = messageLabel;
    }

    /**
     * 异步启动登录。
     *
     * @param userName 登录名
     * @param role 选定角色
     * @param password 明文密码
     */
    public void start(final String userName, final String role, final String password) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                perform(userName, role, password);
            }
        }, "vcampus-login").start();
    }

    private void perform(String userName, String role, String password) {
        try {
            UserService userService = VCampusClientApp.connect(NetworkConstant.DEFAULT_HOST,
                    NetworkConstant.DEFAULT_PORT);
            userService.login(userName, role, password);
            // 姓名取服务端签发的会话：服务端保证非空（未采集时用登录名顶上）。
            // 以前这里直接传输入框里的 userName，界面上就只能看到学号/工号，即「显示的都是用户名」的根因。
            openMain(userService.getSession().getRealName(), role);
        } catch (AuthException e) {
            VCampusClientApp.stopQuietly();// 登录未成功：关闭已建立的连接
            showMessage(messageFor(e));
        } catch (IOException e) {
            VCampusClientApp.stopQuietly();
            showMessage("无法连接服务器：" + e.getMessage());
        } catch (InterruptedException e) {
            VCampusClientApp.stopQuietly();
            Thread.currentThread().interrupt();
            showMessage("登录被中断");
        }
    }

    private void openMain(final String userName, final String role) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                MainFrame main = new MainFrame(userName, role);
                if ((frame.getExtendedState() & JFrame.MAXIMIZED_BOTH) == JFrame.MAXIMIZED_BOTH) {
                    main.setExtendedState(JFrame.MAXIMIZED_BOTH);
                }
                main.setVisible(true);
                frame.dispose();
            }
        });
    }

    private String messageFor(AuthException e) {
        if (StatusCode.UNAUTHORIZED.equals(e.getStatusCode())) {
            return "账号或密码错误";
        }
        if (StatusCode.FORBIDDEN.equals(e.getStatusCode())) {
            return "该账号无此操作权限";
        }
        if (e.getStatusCode() == null) {
            return "服务器无响应，请稍后重试";
        }
        return "登录失败：" + e.getStatusCode();
    }

    private void showMessage(final String text) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                messageLabel.setText(text);
            }
        });
    }
}
