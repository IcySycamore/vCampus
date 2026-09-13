package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.client.VCampusClientApp;
import edu.seu.vcampus.client.api.ClientApis;
import edu.seu.vcampus.client.handler.ConnectionListener;
import javax.swing.SwingUtilities;
import edu.seu.vcampus.client.view.dialog.GlobalSearchDialog;
import edu.seu.vcampus.client.view.dialog.SettingsDialog;
import edu.seu.vcampus.client.view.theme.ResponsiveTypography;
import edu.seu.vcampus.client.view.theme.UiTheme;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * 登录后的客户端主窗口。
 */
public class MainFrame extends JFrame implements ConnectionListener {
    private static final long serialVersionUID = 1L;
    private final MainContentPanel contentPanel;
    private final ClientApis apis;
    private boolean sessionEnded;

    /**
     * 创建主窗口。
     *
     * @param userId 当前用户 ID
     */
    public MainFrame(String userId) {
        this(null, userId, "学生");
    }

    /**
     * 创建带身份信息的主窗口（不接入模块 API）。
     *
     * @param userId 当前用户 ID
     * @param role 当前登录身份
     */
    public MainFrame(String userId, String role) {
        this(null, userId, role);
    }

    /**
     * 创建带身份信息与模块 API 的主窗口。
     *
     * @param apis 各模块 API 容器；null 表示未装配
     * @param userId 当前用户 ID
     * @param role 当前登录身份
     */
    public MainFrame(ClientApis apis, String userId, String role) {
        super(apis == null ? "vCampus 虚拟校园 · 离线预览" : "vCampus 虚拟校园");
        this.apis = apis;
        contentPanel = new MainContentPanel(apis, userId, role);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(1000, 650));
        setSize(1180, 760);
        setLocationRelativeTo(null);
        getContentPane().setBackground(UiTheme.BACKGROUND);
        SidebarPanel sidebar = new SidebarPanel(contentPanel, apis != null);
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
        if (apis != null) {
            apis.addConnectionListener(this);
            if (!apis.user().isLoggedIn()) {
                connectionClosed(null);
            }
        }
    }

    @Override
    public void connectionClosed(Exception cause) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (!sessionEnded) {
                    new LoginFrame("登录已失效或连接已断开，请重新登录").setVisible(true);
                    dispose();
                }
            }
        });
    }

    @Override
    public void dispose() {
        if (!sessionEnded && apis != null) {
            VCampusClientApp.stopAsync(apis);
        }
        sessionEnded = true;
        super.dispose();
    }

    private void openSearch(String query) {
        new GlobalSearchDialog(this, query, contentPanel).setVisible(true);
    }
}
