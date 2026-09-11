package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.client.auth.ClientSession;
import edu.seu.vcampus.client.auth.SessionCleanup;
import edu.seu.vcampus.client.handler.UIUpdateHandler;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.client.view.dialog.GlobalSearchDialog;
import edu.seu.vcampus.client.view.dialog.SettingsDialog;
import edu.seu.vcampus.client.view.theme.ResponsiveTypography;
import edu.seu.vcampus.client.view.theme.UiTheme;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * 登录后的客户端主窗口。
 */
public class MainFrame extends JFrame implements UIUpdateHandler {
    private static final long serialVersionUID = 1L;
    private final MainContentPanel contentPanel;
    private final ClientSession session;
    private boolean sessionEnded;

    /**
     * 创建主窗口。
     *
     * @param userId 当前用户 ID
     */
    public MainFrame(String userId) {
        this(userId, "学生");
    }

    /**
     * 创建带身份信息的主窗口。
     *
     * @param userId 当前用户 ID
     * @param role 当前登录身份
     */
    public MainFrame(String userId, String role) {
        this(userId, role, null);
    }

    /**
     * 使用真实登录结果及原连接打开主窗口。
     * @param session 已认证的客户端会话
     */
    public MainFrame(ClientSession session) {
        this(session.getUsername(), session.getRole(), session);
    }

    private MainFrame(String userId, String role, ClientSession session) {
        super(session == null ? "vCampus 虚拟校园 · 离线预览" : "vCampus 虚拟校园");
        this.session = session;
        contentPanel = new MainContentPanel(userId, role, session);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(1000, 650));
        setSize(1180, 760);
        setLocationRelativeTo(null);
        getContentPane().setBackground(UiTheme.BACKGROUND);
        SidebarPanel sidebar = new SidebarPanel(contentPanel, session != null);
        add(sidebar, BorderLayout.WEST);
        ResponsiveTypography.installProportionalWidth(this, sidebar, 0.15F);
        JPanel workspace = new JPanel(new BorderLayout());
        workspace.setBackground(UiTheme.BACKGROUND);
        workspace.add(new MainHeaderPanel(userId, role, new StringHandler() {
            @Override
            public void handle(String query) {
                openSearch(query);
            }
        }, new Runnable() {
            @Override
            public void run() {
                new SettingsDialog(MainFrame.this).setVisible(true);
            }
        }), BorderLayout.NORTH);
        workspace.add(contentPanel, BorderLayout.CENTER);
        add(workspace, BorderLayout.CENTER);
        contentPanel.setPageChangeListener(sidebar);
        ResponsiveTypography.install(this, 1180);
        if (session != null) {
            session.setHandler(this);
        }
    }

    @Override
    public void handleMessage(final Message message) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (sessionEnded) {
                    return;
                }
                if (StatusCode.UNAUTHORIZED.equals(message.getStatusCode())) {
                    returnToLogin("登录已过期，请重新登录");
                } else {
                    contentPanel.handleMessage(message);
                }
            }
        });
    }

    @Override
    public void connectionClosed(Exception cause) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                returnToLogin("连接已断开，请重新登录");
            }
        });
    }

    @Override
    public void dispose() {
        sessionEnded = true;
        SessionCleanup.closeAsync(session);
        super.dispose();
    }

    private void returnToLogin(String message) {
        if (!sessionEnded) {
            new LoginFrame(message).setVisible(true);
            dispose();
        }
    }

    private void openSearch(String query) {
        new GlobalSearchDialog(this, query, contentPanel).setVisible(true);
    }
}
