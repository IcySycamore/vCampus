package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.client.VCampusClientApp;
import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.client.api.ClientApis;
import edu.seu.vcampus.common.constant.NetworkConstant;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.user.entity.Role;
import edu.seu.vcampus.common.user.entity.SessionEntry;

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
            ClientApis apis = VCampusClientApp.connect(NetworkConstant.DEFAULT_HOST,
                    NetworkConstant.DEFAULT_PORT);
            apis.user().login(userName, Role.fromDisplayName(role), password);
            SessionEntry entry = apis.user().currentSession();
            if (entry == null) {// 登录成功必有会话；缺失视为协议异常
                VCampusClientApp.stopQuietly();
                showMessage("登录响应异常，请稍后重试");
                return;
            }
            // 身份以服务器下发的会话为准，不采信登录页所选项；
            // 姓名同样取自会话——原先这里传登录名，界面上只能看到学号/工号，
            // 即「显示的都是用户名」的根因。会话缺姓名时（老协议）回落到登录名。
            openMain(apis, shownName(entry), entry.getRole());
        } catch (ApiException e) {
            VCampusClientApp.stopQuietly();// 登录未成功：关闭已建立的连接
            showMessage(loginMessage(e));
        } catch (IOException e) {
            VCampusClientApp.stopQuietly();
            showMessage("无法连接服务器：" + e.getMessage());
        }
    }

    /**
     * 取会话里的姓名；缺姓名时回落到登录名（服务端保证有姓名，这里只是兜底）。
     *
     * @param entry 会话记录
     * @return 界面上要显示的姓名
     */
    private static String shownName(SessionEntry entry) {
        String name = entry.getDisplayName();
        return name == null || name.trim().length() == 0 ? entry.getUsername() : name.trim();
    }

    private void openMain(final ClientApis apis, final String userName, final String role) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                MainFrame main = new MainFrame(apis, userName, role);
                if ((frame.getExtendedState() & JFrame.MAXIMIZED_BOTH) == JFrame.MAXIMIZED_BOTH) {
                    main.setExtendedState(JFrame.MAXIMIZED_BOTH);
                }
                main.setVisible(true);
                frame.dispose();
            }
        });
    }

    private String loginMessage(ApiException e) {
        if (StatusCode.UNAUTHORIZED.equals(e.getStatusCode())) {
            return "账号或密码错误";// 登录场景下的 401 就是密码/用户名不对
        }
        return e.getMessage();
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
