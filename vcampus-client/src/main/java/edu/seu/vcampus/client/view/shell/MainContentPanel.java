package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.client.auth.ClientSession;
import edu.seu.vcampus.client.handler.UIUpdateHandler;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.client.view.component.RoundedPanel;
import edu.seu.vcampus.client.view.library.LibraryPanel;
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
public class MainContentPanel extends JPanel implements StringHandler, UIUpdateHandler {

    private static final long serialVersionUID = 1L;
    private final AppRouter router;
    private final LibraryPanel libraryPanel = new LibraryPanel();
    private StringHandler pageChangeListener;

    /**
     * 创建并注册所有一级页面。
     */
    public MainContentPanel() {
        this("用户", "学生");
    }

    /**
     * 创建带当前用户问候信息的内容区。
     *
     * @param userId 当前用户 ID
     * @param role 当前身份
     */
    public MainContentPanel(String userId, String role) {
        this(userId, role, null);
    }

    /**
     * 创建内容区并向图书馆页面传入已认证会话。
     * @param userId 当前用户名
     * @param role 服务器确认的角色
     * @param session 登录会话，null 表示离线预览
     */
    public MainContentPanel(String userId, String role, ClientSession session) {
        router = new AppRouter(this, PageNames.HOME);
        setBackground(UiTheme.BACKGROUND);
        router.register(PageNames.HOME, new OaDashboardPanel(userId, role, this));
        router.register(PageNames.USER,
                createPlaceholder("用户中心", "管理个人资料、登录密码与身份信息", "user"));
        router.register(PageNames.STUDENT,
                createPlaceholder("学生学籍", "集中查看和维护个人学籍信息", "student"));
        router.register(PageNames.COURSE,
                createPlaceholder("选课与成绩", "管理课程安排，查询学习成果", "course"));
        if (session != null) {
            libraryPanel.attach(session);
        }
        router.register(PageNames.LIBRARY, libraryPanel);
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

    @Override
    public void handleMessage(Message message) {
        libraryPanel.handleMessage(message);
    }

    @Override
    public void connectionClosed(Exception cause) {
        libraryPanel.connectionClosed(cause);
    }

    private JPanel createPlaceholder(String title, String description, String icon) {
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(UiTheme.BACKGROUND);
        page.setBorder(BorderFactory.createEmptyBorder(34, 36, 34, 36));
        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 14), 24, UiTheme.BACKGROUND);
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
