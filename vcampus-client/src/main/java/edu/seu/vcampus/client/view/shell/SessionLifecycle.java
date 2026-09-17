package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.client.VCampusClientApp;
import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.client.api.ClientApis;
import edu.seu.vcampus.client.handler.ConnectionListener;
import edu.seu.vcampus.client.view.UiTasks;
import edu.seu.vcampus.client.view.dialog.ChangePasswordDialog;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * 会话收尾：退出登录与断线都回到登录页（见 ADR-0009 D9）。
 *
 * <p>
 * 从 {@code MainFrame} 抽出（原文件破 200 行上限）。集中在一处的原因：断线必须<b>只有一个</b>处理点，
 * 否则每个页面各弹一个「连接已断开」窗口，而且「连接没了但界面还开着」的卡死态就是漏掉这条路径造成的。
 */
public final class SessionLifecycle {

    /** 主窗口（需要销毁并回到登录页）。 */
    private final MainFrame m_frame;

    /** 各模块 API；null 表示未装配（预览/测试）。 */
    private final ClientApis m_apis;

    /** 是否已在回登录页，避免登出与断线重复处理。 */
    private boolean m_closing;

    /**
     * 构造会话收尾器。
     *
     * @param frame 主窗口
     * @param apis  各模块 API 容器；null 表示未装配
     */
    public SessionLifecycle(MainFrame frame, ClientApis apis) {
        this.m_frame = frame;
        this.m_apis = apis;
    }

    /** 登记断线与登录态失效监听：两者都立即回登录页。 */
    public void listenForDisconnect() {
        if (m_apis == null || m_apis.user() == null) {
            return;
        }
        m_apis.addConnectionListener(new ConnectionListener() {
            @Override
            public void connectionClosed(Exception cause) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        backToLogin("连接已断开，请重新登录");
                    }
                });
            }
        });
        // token 失效（401）同样立即结束会话：不这样做，界面会停在「已登录」而每个操作都报「登录状态已失效」。
        m_apis.setSessionExpiredAction(new Runnable() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        backToLogin("登录状态已失效，请重新登录");
                    }
                });
            }
        });
    }

    /**
     * 「修改密码」动作（账户弹窗里触发）。
     *
     * @return 动作
     */
    public Runnable changePasswordAction() {
        return new Runnable() {
            @Override
            public void run() {
                if (m_apis != null) {
                    new ChangePasswordDialog(m_frame, m_apis.user()).setVisible(true);
                }
            }
        };
    }

    /**
     * 「退出登录」动作：后台通知服务器使会话失效，无论结果都回登录页。
     *
     * @return 动作
     */
    public Runnable logoutAction() {
        return new Runnable() {
            @Override
            public void run() {
                logout();
            }
        };
    }

    /** 登出：先通知服务器，再回登录页（服务器不可达时本地会话也已清空）。 */
    private void logout() {
        if (m_apis == null) {
            backToLogin("已退出登录");
            return;
        }
        UiTasks.run(new UiTasks.Task<Void>() {
            @Override
            public Void run() {
                m_apis.user().logout();
                return null;
            }
        }, new UiTasks.Success<Void>() {
            @Override
            public void accept(Void ignored) {
                backToLogin("已退出登录");
            }
        }, new UiTasks.Failure() {
            @Override
            public void accept(ApiException error) {
                backToLogin("已退出登录");
            }
        });
    }

    /**
     * 关闭连接、销毁主窗口并回到登录页。
     *
     * @param message 回登录页前的提示文案；null 表示静默
     */
    public void backToLogin(String message) {
        if (m_closing) {
            return;
        }
        m_closing = true;
        VCampusClientApp.stopQuietly();
        m_frame.dispose();
        if (message != null) {
            JOptionPane.showMessageDialog(null, message, "vCampus",
                    JOptionPane.INFORMATION_MESSAGE);
        }
        new LoginFrame().setVisible(true);
    }
}
