package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.client.VCampusClientApp;
import edu.seu.vcampus.client.api.ClientApis;
import edu.seu.vcampus.client.view.dialog.GlobalSearchDialog;
import edu.seu.vcampus.client.view.dialog.SettingsDialog;
import edu.seu.vcampus.client.view.theme.ResponsiveTypography;
import edu.seu.vcampus.client.view.theme.UiTheme;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * 登录后的客户端主窗口。
 */
public class MainFrame extends JFrame {
    private static final long serialVersionUID = 1L;
    private final MainContentPanel contentPanel;

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
        super("vCampus 虚拟校园");
        contentPanel = new MainContentPanel(apis, userId, role);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                VCampusClientApp.stopQuietly();// 退出客户端：关闭连接，各模块随之丢弃内存会话
            }
        });
        setMinimumSize(new Dimension(1000, 650));
        setSize(1180, 760);
        setLocationRelativeTo(null);
        getContentPane().setBackground(UiTheme.BACKGROUND);
        SidebarPanel sidebar = new SidebarPanel(contentPanel);
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
    }

    private void openSearch(String query) {
        new GlobalSearchDialog(this, query, contentPanel).setVisible(true);
    }
}
