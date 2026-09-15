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
 * {@link SessionEntry}，构造参数只在无会话的预览/测试场景下兜底。
 */
public class MainFrame extends JFrame {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 内容区。 */
    private final MainContentPanel contentPanel;

    /** 会话收尾器。 */
    private final SessionLifecycle m_lifecycle;

    /**
     * 创建主窗口（不接入模块 API）。
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
     * @param role   当前登录身份
     */
    public MainFrame(String userId, String role) {
        this(null, userId, role);
    }

    /**
     * 创建带身份信息与模块 API 的主窗口。
     *
     * @param apis   各模块 API 容器；null 表示未装配
     * @param userId 当前用户 ID（无会话时的兜底显示名）
     * @param role   当前登录身份（无会话时的兜底身份）
     */
    public MainFrame(ClientApis apis, String userId, String role) {
        super("vCampus 虚拟校园");
        SessionEntry session = resolveSession(apis, userId, role);
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

    /** 取会话：优先已登录会话，缺失时用构造参数兜底（预览/测试）。 */
    private SessionEntry resolveSession(ClientApis apis, String userId, String role) {
        if (apis != null && apis.user() != null && apis.user().currentSession() != null) {
            return apis.user().currentSession();
        }
        return new SessionEntry(null, userId, role, 0L);
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
