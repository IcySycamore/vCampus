package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.client.api.ClientApis;
import edu.seu.vcampus.client.course.CoursePanel;
import edu.seu.vcampus.client.view.bank.BankAdminPanel;
import edu.seu.vcampus.client.view.bank.BankPanel;
import edu.seu.vcampus.client.view.library.LibraryPanel;
import edu.seu.vcampus.client.view.shop.ShopAdminOrderPanel;
import edu.seu.vcampus.client.view.shop.ShopAdminPanel;
import edu.seu.vcampus.client.view.shop.ShopPanel;
import edu.seu.vcampus.client.view.theme.UiTheme;
import edu.seu.vcampus.common.user.entity.Capability;
import edu.seu.vcampus.common.user.entity.Permissions;
import edu.seu.vcampus.common.user.entity.Role;
import edu.seu.vcampus.common.user.entity.SessionEntry;

import java.awt.Component;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

/**
 * 主窗口的可切换内容区域。
 *
 * <p>
 * 页面与角色绑定：只有具备对应能力的角色才会注册用户管理页。 未注册页面无法通过程序化跳转进入，过滤规则与侧栏同源。
 */
public class MainContentPanel extends JPanel implements StringHandler {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 页面路由。 */
    private final AppRouter router;

    /** 图书馆页面：进入时刷新当前读者数据。 */
    private final LibraryPanel libraryPanel;

    /** 用户银行页面：Shop 支付成功后自动刷新账户和流水。 */
    private final BankPanel bankPanel;

    /** 已注册页面（用于拦截无权限跳转）。 */
    private final Set<String> pages = new LinkedHashSet<String>();

    /** 页面切换监听器。 */
    private StringHandler pageChangeListener;

    /** 创建并注册所有一级页面（未装配 API，页面回落为占位）。 */
    public MainContentPanel() {
        this(null, (SessionEntry) null);
    }

    /**
     * 创建带当前用户信息的内容区（不接入模块 API）。
     *
     * @param userId 当前用户 ID
     * @param role   当前身份
     */
    public MainContentPanel(String userId, String role) {
        this(null, new SessionEntry(null, userId, role, 0L));
    }

    /**
     * 创建内容区并接入各模块客户端 API。
     *
     * @param apis   各模块 API 容器；null 表示未装配
     * @param userId 当前用户 ID
     * @param role   当前身份
     */
    public MainContentPanel(ClientApis apis, String userId, String role) {
        this(apis, new SessionEntry(null, userId, role, 0L));
    }

    /**
     * 创建内容区（身份取自会话）。
     *
     * @param apis    各模块 API 容器；null 表示未装配
     * @param session 当前会话；null 表示无身份
     */
    public MainContentPanel(ClientApis apis, SessionEntry session) {
        router = new AppRouter(this, PageNames.HOME);
        setBackground(UiTheme.BACKGROUND);
        Role role = session == null ? null : Role.fromDisplayName(session.getRole());
        register(PageNames.HOME, new OaDashboardPanel(apis, session, this));
        register(PageNames.STUDENT,
                apis == null ? PlaceholderPage.create("个人信息", "查看个人资料与在校状态", "student")
                        : new ProfilePanel(apis.user().currentSession(), apis.student()));
        register(PageNames.COURSE,
                apis == null
                        ? PlaceholderPage.create("选课与成绩", "管理课程安排，查询学习成果", "course")
                        : new CoursePanel(apis.course(), courseRole(role)));
        libraryPanel = new LibraryPanel(apis == null ? null : apis.library());
        register(PageNames.LIBRARY, libraryPanel);
        bankPanel = role == Role.ADMIN ? null
                : new BankPanel(apis == null ? null : apis.bank());
        JScrollPane shop = new JScrollPane(apis == null ? new ShopPanel(null)
                : new ShopPanel(apis.shop(), bankRefreshAction()));
        shop.setBorder(BorderFactory.createEmptyBorder());
        shop.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        register(PageNames.SHOP, shop);
        Component bankPage;
        if (role == Role.ADMIN) {
            bankPage = new BankAdminPanel(apis == null ? null : apis.bank());
        } else {
            JScrollPane scroll = new JScrollPane(bankPanel);
            scroll.setBorder(BorderFactory.createEmptyBorder());
            scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            bankPage = scroll;
        }
        register(PageNames.BANK, bankPage);
        if (Permissions.can(role, Capability.USER_MANAGE)) {
            register(PageNames.USER_ADMIN,
                    apis == null
                            ? PlaceholderPage.create("用户管理", "注册、启停、编辑与注销校园账号", "user")
                            : new AdminConsolePanel(apis.userAdmin(), apis.student(), role));
        }
        if (Permissions.can(role, Capability.USER_MANAGE)) {
            JTabbedPane shopAdminTabs = new JTabbedPane();
            shopAdminTabs.addTab("商品管理",
                    apis == null ? new ShopAdminPanel(null) : new ShopAdminPanel(apis.shop()));
            shopAdminTabs.addTab("订单管理", apis == null ? new ShopAdminOrderPanel(null)
                    : new ShopAdminOrderPanel(apis.shop()));
            JScrollPane shopAdmin = new JScrollPane(shopAdminTabs);
            shopAdmin.setBorder(BorderFactory.createEmptyBorder());
            shopAdmin.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            register(PageNames.SHOP_ADMIN, shopAdmin);
        }
    }

    private Runnable bankRefreshAction() {
        return new Runnable() {
            @Override
            public void run() {
                if (bankPanel != null) {
                    bankPanel.refreshData();
                }
            }
        };
    }

    /** 注册页面并记录（记录用于拦截无权限跳转）。 */
    private void register(String page, Component component) {
        router.register(page, component);
        pages.add(page);
    }

    /** 取选课页使用的角色显示名；未登录时回落为学生。 */
    private static String courseRole(Role role) {
        return role == null ? Role.STUDENT.getDisplayName() : role.getDisplayName();
    }

    /**
     * 切换到指定页面；未注册（多为无权限）时静默忽略。
     *
     * @param page 页面标识
     */
    public void showPage(String page) {
        if (!pages.contains(page)) {
            return;
        }
        router.navigate(page);
        if (PageNames.LIBRARY.equals(page)) {
            libraryPanel.refresh();
        }
        if (pageChangeListener != null) {
            pageChangeListener.handle(page);
        }
    }

    /**
     * 设置页面切换监听器，用于同步侧栏选中状态。
     *
     * @param listener 页面切换监听器
     */
    public void setPageChangeListener(StringHandler listener) {
        pageChangeListener = listener;
    }

    /** {@inheritDoc} */
    @Override
    public void handle(String page) {
        showPage(page);
    }

    /** @return 当前页面标识 */
    public String getCurrentPage() {
        return router.getCurrentPage();
    }

    /**
     * 判断页面是否已注册。
     *
     * @param page 页面标识
     * @return 已注册返回 true
     */
    public boolean isRegistered(String page) {
        return pages.contains(page);
    }

    /** @return 当前身份可搜索到的页面（标题到页面标识） */
    public Map<String, String> searchPages() {
        Map<String, String> entries = new LinkedHashMap<String, String>();
        for (Map.Entry<String, String> entry : PageNames.searchTitles().entrySet()) {
            if (pages.contains(entry.getValue())) {
                entries.put(entry.getKey(), entry.getValue());
            }
        }
        return entries;
    }
}
