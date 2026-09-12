package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.client.api.ClientApis;
import edu.seu.vcampus.client.view.component.RoundedPanel;
import edu.seu.vcampus.client.view.theme.UiIcons;
import edu.seu.vcampus.client.view.theme.UiTheme;

import java.awt.BorderLayout;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * 主窗口的可切换内容区域。
 */
public class MainContentPanel extends JPanel implements StringHandler {

    private static final long serialVersionUID = 1L;
    private final AppRouter router;
    private StringHandler pageChangeListener;

    /**
     * 创建并注册所有一级页面。
     */
    public MainContentPanel() {
        this(null, "用户", "学生");
    }

    /**
     * 创建带当前用户问候信息的内容区（不接入模块 API，页面回落为占位）。
     *
     * @param userId 当前用户 ID
     * @param role 当前身份
     */
    public MainContentPanel(String userId, String role) {
        this(null, userId, role);
    }

    /**
     * 创建内容区并接入各模块客户端 API。
     *
     * <p>
     * 每个页面只接收自己那一个 API（如 {@code UserCenterPanel(user())}），容器本身不往下传（见 ADR-0009 D8）。
     *
     * @param apis 各模块 API 容器；null 表示未装配（页面回落为占位，供预览与测试）
     * @param userId 当前用户 ID
     * @param role 当前身份
     */
    public MainContentPanel(ClientApis apis, String userId, String role) {
        router = new AppRouter(this, PageNames.HOME);
        setBackground(UiTheme.BACKGROUND);
        router.register(PageNames.HOME, new OaDashboardPanel(userId, role, this));
        router.register(PageNames.USER,
                apis == null ? createPlaceholder("用户中心", "管理个人资料、登录密码与身份信息", "user")
                        : new UserCenterPanel(apis.user()));
        router.register(PageNames.STUDENT,
                apis == null ? createPlaceholder("个人信息", "查看个人资料与在校状态", "student")
                        : new ProfilePanel(apis.user().currentSession(), apis.student()));
        router.register(PageNames.COURSE,
                createPlaceholder("选课与成绩", "管理课程安排，查询学习成果", "course"));
        router.register(PageNames.LIBRARY,
                createPlaceholder("智慧图书馆", "检索馆藏，管理个人借阅与归还", "library"));
        router.register(PageNames.SHOP,
                createPlaceholder("校园商店", "浏览校园商品与订单", "shop"));
        router.register(PageNames.BANK,
                createPlaceholder("校园银行", "管理余额与校园消费流水", "bank"));
    }

    /**
     * 切换到指定页面。
     *
     * @param page 已注册的页面标识
     */
    public void showPage(String page) {
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
     * 创建占位页（模块未装配或尚未实现时使用）。
     *
     * @param title 页面标题
     * @param description 页面描述
     * @param icon 图标名
     * @return 占位页
     */
    private JPanel createPlaceholder(String title, String description, String icon) {
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(UiTheme.BACKGROUND);
        page.setBorder(BorderFactory.createEmptyBorder(34, 36, 34, 36));
        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 14), 24, UiTheme.SURFACE);
        card.setBorder(BorderFactory.createEmptyBorder(80, 40, 80, 40));
        JLabel iconLabel = new JLabel(UiIcons.load(icon, 72), SwingConstants.CENTER);
        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setForeground(UiTheme.TEXT);
        titleLabel.setFont(UiTheme.font(Font.BOLD, 28F));
        JLabel descriptionLabel = new JLabel(description, SwingConstants.CENTER);
        descriptionLabel.setForeground(UiTheme.MUTED);
        card.add(iconLabel, BorderLayout.NORTH);
        card.add(titleLabel, BorderLayout.CENTER);
        card.add(descriptionLabel, BorderLayout.SOUTH);
        page.add(card, BorderLayout.CENTER);
        return page;
    }
}
