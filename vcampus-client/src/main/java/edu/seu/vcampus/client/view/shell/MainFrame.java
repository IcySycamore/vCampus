package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.client.VCampusClientApp;
import edu.seu.vcampus.client.api.ClientApis;
import edu.seu.vcampus.client.view.dialog.GlobalSearchDialog;
import edu.seu.vcampus.client.view.dialog.SettingsDialog;
import edu.seu.vcampus.client.view.theme.ResponsiveTypography;
import edu.seu.vcampus.client.view.theme.UiTheme;
import edu.seu.vcampus.common.user.entity.Role;
import edu.seu.vcampus.common.user.entity.SessionEntry;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * 登录后的客户端主窗口。
 *
 * <p>
 * 会话收尾（退出登录 / 断线回登录页）交给 {@link SessionLifecycle}，本类只负责组装：侧栏、顶栏、内容区。 身份一律取自服务端下发的
 * {@link SessionEntry}。
 *
 * <p>
 * <b>没有会话就没有主窗口</b>：以前还留着两个「不接 API、用构造参数兜底身份」的构造器，它们造一个 凭空的 {@link SessionEntry}
 * 就能把主界面打开，于是任何不登录的入口都能进主界面，里面还顶着一个假身份。 那几个构造器已删除，现在唯一的入口要求 {@code apis.user().currentSession()}
 * 非空。
 */
public class MainFrame extends JFrame {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 内容区。 */
    private final MainContentPanel contentPanel;

    /** 会话收尾器。 */
    private final SessionLifecycle m_lifecycle;

    /**
     * 用已认证的会话创建主窗口。
     *
     * @param apis 各模块 API 容器，且必须已有登录会话
     */
    public MainFrame(ClientApis apis) {
        super("vCampus 虚拟校园");
        SessionEntry session = requireSession(apis);
        m_lifecycle = new SessionLifecycle(this, apis);
        contentPanel = new MainContentPanel(apis, session);
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
        SidebarPanel sidebar = new SidebarPanel(contentPanel,
                Role.fromDisplayName(session.getRole()));
        add(sidebar, BorderLayout.WEST);
        ResponsiveTypography.installProportionalWidth(this, sidebar, 0.15F);
        add(createWorkspace(session), BorderLayout.CENTER);
        contentPanel.setPageChangeListener(sidebar);
        ResponsiveTypography.install(this, 1180);
        m_lifecycle.listenForDisconnect();
    }

    /**
     * 取当前登录会话，取不到就拒绝。
     *
     * <p>
     * 单独抽成静态方法是为了让「没有会话就不给开窗」这条约束能被直接测到 —— 它现在是主窗口 唯一的入口条件。
     *
     * @param apis 各模块 API 容器
     * @return 会话记录
     * @throws IllegalStateException 未登录
     */
    static SessionEntry requireSession(ClientApis apis) {
        if (apis == null || apis.user() == null || apis.user().currentSession() == null) {
            throw new IllegalStateException("未登录：主窗口只能由已认证的会话来打开");
        }
        return apis.user().currentSession();
    }

    /** 组装「顶栏 + 内容区」。 */
    private JPanel createWorkspace(SessionEntry session) {
        JPanel workspace = new JPanel(new BorderLayout());
        workspace.setBackground(UiTheme.BACKGROUND);
        workspace.add(new MainHeaderPanel(session, searchHandler(), settingsAction(),
                m_lifecycle.changePasswordAction(), m_lifecycle.logoutAction()),
                BorderLayout.NORTH);
        workspace.add(contentPanel, BorderLayout.CENTER);
        return workspace;
    }

    private StringHandler searchHandler() {
        return new StringHandler() {
            @Override
            public void handle(String query) {
                openSearch(query);
            }
        };
    }

    private Runnable settingsAction() {
        return new Runnable() {
            @Override
            public void run() {
                new SettingsDialog(MainFrame.this).setVisible(true);
            }
        };
    }

    private void openSearch(String query) {
        new GlobalSearchDialog(this, query, contentPanel, contentPanel.searchPages())
                .setVisible(true);
    }
}
