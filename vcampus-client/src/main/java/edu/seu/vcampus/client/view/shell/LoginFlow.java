package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.client.VCampusClientApp;
import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.client.api.ClientApis;
import edu.seu.vcampus.client.network.ClientServerConfig;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.user.entity.SessionEntry;

import java.io.IOException;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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
    private volatile boolean cancelled;
    private volatile ClientApis activeApis;
    private boolean running;
    private boolean handedOff;

    /**
     * 构造登录流程。
     *
     * @param frame        登录窗口
     * @param messageLabel 登录提示标签
     */
    public LoginFlow(LoginFrame frame, JLabel messageLabel) {
        this.frame = frame;
        this.messageLabel = messageLabel;
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                cancel();
            }

            @Override
            public void windowClosed(WindowEvent event) {
                if (!handedOff) {
                    cancel();
                }
            }
        });
    }

    /**
     * 异步启动登录。
     *
     * @param userName 登录名
     * @param password 明文密码
     */
    public void start(final String userName, final String password) {
        if (running || cancelled) {
            return;
        }
        running = true;
        frame.setBusy(true);
        new Thread(new Runnable() {
            @Override
            public void run() {
                perform(userName, password);
            }
        }, "vcampus-login").start();
    }

    private void perform(String userName, String password) {
        try {
            ClientServerConfig config = ClientServerConfig.load();
            ClientApis apis = VCampusClientApp.connect(config.host(), config.port());
            activeApis = apis;
            if (cancelled) {
                VCampusClientApp.stopAsync(apis);
                return;
            }
            apis.user().login(userName, password);
            SessionEntry entry = apis.user().currentSession();
            if (entry == null) {// 登录成功必有会话；缺失视为协议异常
                VCampusClientApp.stopAsync(activeApis);
                showMessage("登录响应异常，请稍后重试");
                return;
            }
            // 身份以服务器下发的会话为准 —— 登录页根本没有角色可选项：
            // 角色是账户的属性，不是登录时选出来的（旧登录页那个三选一只是装饰，
            // 选了“管理员”也进不去管理页，反而让人以为是自己选错了）。
            openMain(apis);
        } catch (ApiException e) {
            VCampusClientApp.stopAsync(activeApis);// 登录未成功：关闭已建立的连接
            showMessage(loginMessage(e));
        } catch (IOException e) {
            VCampusClientApp.stopAsync(activeApis);
            showMessage("无法连接服务器：" + e.getMessage());
        }
    }

    /**
     * 登录成功后打开主窗口。
     *
     * @param apis 已建立且已登录的 API 容器
     */
    private void openMain(final ClientApis apis) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (cancelled || !frame.isDisplayable() || !apis.user().isLoggedIn()) {
                    VCampusClientApp.stopAsync(apis);
                    return;
                }
                MainFrame main = new MainFrame(apis);
                if ((frame.getExtendedState() & JFrame.MAXIMIZED_BOTH) == JFrame.MAXIMIZED_BOTH) {
                    main.setExtendedState(JFrame.MAXIMIZED_BOTH);
                }
                main.setVisible(true);
                handedOff = true;
                frame.dispose();
            }
        });
    }

    private void cancel() {
        cancelled = true;
        if (activeApis != null) {
            VCampusClientApp.stopAsync(activeApis);
        }
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
                running = false;
                frame.setBusy(false);
                messageLabel.setText(text);
            }
        });
    }
}
