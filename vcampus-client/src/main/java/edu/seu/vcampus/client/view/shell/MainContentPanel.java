package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.client.api.ClientApis;
import edu.seu.vcampus.client.view.bank.BankPanel;
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

/**
 * 主窗口的可切换内容区域。
 *
 * <p>
 * 页面与角色绑定：只有具备对应能力的角色才会<b>注册</b>「用户管理」页。未注册的页面连程序化跳转也到不了
 * （比「只把侧栏按钮藏起来」更硬），跳转到未注册页面时静默忽略而不是抛异常。过滤规则与侧栏同源， 都用 {@link Permissions#can(Role, Capability)}。
 *
 * <p>
 * 原「用户中心」页已下线：资料展示与修改密码/退出登录收进右上角账户弹窗（见 {@code AccountPopupPanel}）， 用户管理升格为独立页面。
 */
public class MainContentPanel extends JPanel implements StringHandler {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 页面路由。 */
    private final AppRouter router;

    /** 已注册页面（用于拦截无权限跳转）。 */
    private final Set<String> pages = new LinkedHashSet<String>();

    /** 页面切换监听器。 */
    private StringHandler pageChangeListener;

    /**
     * 创建并注册所有一级页面（未装配 API，页面回落为占位）。
     */
    public MainContentPanel() {
        this(null, (SessionEntry) null);
    }

    /**
     * 创建带当前用户信息的内容区（不接入模块 API，页面回落为占位）。
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
     * <p>
     * 每个页面只接收自己那一个 API（如 {@code ProfilePanel(student())}），容器本身不往下传（见 ADR-0009 D8）。
     *
     * @param apis    各模块 API 容器；null 表示未装配
     * @param session 当前会话；null 表示无身份（仅注册公共页面）
     */
    public MainContentPanel(ClientApis apis, SessionEntry session) {
        router = new AppRouter(this, PageNames.HOME);
        setBackground(UiTheme.BACKGROUND);
        Role role = session == null ? null : Role.fromDisplayName(session.getRole());
        register(PageNames.HOME, new OaDashboardPanel(session, this));
        register(PageNames.STUDENT,
                apis == null ? PlaceholderPage.create("个人信息", "查看个人资料与在校状态", "student")
                        : new ProfilePanel(apis.user().currentSession(), apis.student()));
        register(PageNames.COURSE,
                PlaceholderPage.create("选课与成绩", "管理课程安排，查询学习成果", "course"));
        register(PageNames.LIBRARY,
                PlaceholderPage.create("智慧图书馆", "检索馆藏，管理个人借阅与归还", "library"));
        register(PageNames.SHOP,
                PlaceholderPage.create("校园商店", "浏览校园商品与订单", "shop"));
        JScrollPane bank = new JScrollPane(apis == null ? new BankPanel()
                : new BankPanel(apis.bank()));
        bank.setBorder(BorderFactory.createEmptyBorder());
        bank.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        register(PageNames.BANK, bank);
        if (Permissions.can(role, Capability.USER_MANAGE)) {
            register(PageNames.USER_ADMIN,
                    apis == null
                            ? PlaceholderPage.create("用户管理", "注册、启停、编辑与注销校园账号", "user")
                            : new UserManagePage(apis.userAdmin()));
        }
    }

    /** 注册页面并记录（记录用于拦截无权限跳转）。 */
    private void register(String page, Component component) {
        router.register(page, component);
        pages.add(page);
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

    /**
     * 响应工作台发出的页面跳转请求。
     *
     * @param page 页面标识
     */
    @Override
    public void handle(String page) {
        showPage(page);
    }

    /**
     * 返回当前页面标识，供导航状态和测试使用。
     *
     * @return 当前页面标识
     */
    public String getCurrentPage() {
        return router.getCurrentPage();
    }

    /**
     * 判断页面是否已注册（供测试与全局搜索共用）。
     *
     * @param page 页面标识
     * @return 已注册返回 true
     */
    public boolean isRegistered(String page) {
        return pages.contains(page);
    }

    /**
     * 当前身份可搜索到的页面（标题 → 页面标识）。
     *
     * <p>
     * 用已注册页面过滤标题表，因此学生/教师搜不到「用户管理」——搜索入口与侧栏用同一套规则。
     *
     * @return 可搜索页面
     */
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
