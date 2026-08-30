package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.client.view.library.LibraryPanel;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.util.function.Consumer;

/**
 * 主窗口的可切换内容区域。
 */
public class MainContentPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    public static final String HOME = "home";
    public static final String USER = "user";
    public static final String STUDENT = "student";
    public static final String COURSE = "course";
    public static final String LIBRARY = "library";
    public static final String SHOP = "shop";
    public static final String BANK = "bank";
    private final CardLayout cardLayout = new CardLayout();
    private String currentPage = HOME;
    private Consumer<String> pageChangeListener = page -> { };

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
        setLayout(cardLayout);
        setBackground(UiTheme.BACKGROUND);
        add(new OaDashboardPanel(userId, role, page -> showPage(page)), HOME);
        add(createPlaceholder("用户中心", "管理个人资料、登录密码与身份信息", "user"), USER);
        add(createPlaceholder("学生学籍", "集中查看和维护个人学籍信息", "student"), STUDENT);
        add(createPlaceholder("选课与成绩", "管理课程安排，查询学习成果", "course"), COURSE);
        add(new LibraryPanel(), LIBRARY);
        add(createPlaceholder("校园商店", "浏览校园商品与订单", "shop"), SHOP);
        add(createPlaceholder("校园银行", "管理余额与校园消费流水", "bank"), BANK);
    }

    /**
     * 切换到指定页面。
     *
     * @param page 已注册的页面标识
     */
    public void showPage(String page) {
        currentPage = page;
        cardLayout.show(this, page);
        pageChangeListener.accept(page);
    }

    /**
     * 设置页面切换监听器，用于同步侧栏选中状态。
     *
     * @param listener 页面切换监听器
     */
    public void setPageChangeListener(Consumer<String> listener) {
        pageChangeListener = listener == null ? page -> { } : listener;
    }

    /**
     * 返回当前页面标识，供导航状态和测试使用。
     *
     * @return 当前页面标识
     */
    public String getCurrentPage() {
        return currentPage;
    }

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
