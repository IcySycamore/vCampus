package edu.seu.vcampus.client.view.shell;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * 登录后独立显示的校园工作台。
 */
public class OaDashboardPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final String[][] SERVICES = {
        {"student", "学生学籍", MainContentPanel.STUDENT},
        {"course", "选课成绩", MainContentPanel.COURSE},
        {"library", "图书馆", MainContentPanel.LIBRARY},
        {"shop", "校园商店", MainContentPanel.SHOP},
        {"bank", "校园银行", MainContentPanel.BANK},
        {"user", "用户中心", MainContentPanel.USER}
    };
    private static final Color[] STAT_COLORS = {new Color(43, 103, 153),
        new Color(194, 57, 62), new Color(43, 132, 94), new Color(196, 125, 38)};
    private final StringHandler navigator;
    private final String userId;
    private final String role;
    /** 创建只读校园工作台。 */
    public OaDashboardPanel() {
        this("用户", "学生", null);
    }
    /**
     * 创建可跳转的校园工作台。
     *
     * @param navigator 页面跳转回调
     */
    public OaDashboardPanel(StringHandler navigator) {
        this("用户", "学生", navigator);
    }

    /**
     * 创建带问候信息的校园工作台。
     *
     * @param userId 用户 ID
     * @param role 当前身份
     * @param navigator 页面跳转回调
     */
    public OaDashboardPanel(String userId, String role, StringHandler navigator) {
        this.userId = userId;
        this.role = role;
        this.navigator = navigator;
        setLayout(new ProportionalLayout(ProportionalLayout.VERTICAL, 20,
                0.10F, 0.90F));
        setBackground(UiTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(18, 22, 22, 22));
        add(createGreeting());
        add(createDashboard());
    }

    private JPanel createGreeting() {
        RoundedPanel card = new RoundedPanel(new BorderLayout(), 18, UiTheme.SURFACE);
        card.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));
        JLabel greeting = new JLabel("你好，" + role + " · " + userId);
        greeting.setForeground(UiTheme.TEXT);
        greeting.setFont(UiTheme.font(Font.BOLD, 29F));
        card.add(greeting, BorderLayout.WEST);
        JLabel identity = new JLabel(role);
        identity.setForeground(UiTheme.MUTED);
        identity.setFont(UiTheme.font(Font.BOLD, 13F));
        card.add(identity, BorderLayout.EAST);
        return card;
    }

    private JPanel createDashboard() {
        JPanel body = new JPanel(new ProportionalLayout(ProportionalLayout.VERTICAL,
                20, 0.18F, 0.42F, 0.40F));
        body.setOpaque(false);
        JPanel statistics = new JPanel(new GridLayout(1, 4, 20, 0));
        statistics.setOpaque(false);
        statistics.add(statCard("校园服务", "6", "student", 0));
        statistics.add(statCard("当前身份", role, "user", 1));
        statistics.add(statCard("待办事项", "3", "course", 2));
        statistics.add(statCard("借阅图书", "2", "library", 3));
        body.add(statistics);
        JPanel lower = new JPanel(new GridBagLayout());
        lower.setOpaque(false);
        GridBagConstraints grid = new GridBagConstraints();
        grid.fill = GridBagConstraints.BOTH;
        grid.weighty = 1;
        grid.weightx = 0.60;
        grid.insets = new java.awt.Insets(0, 0, 0, 12);
        lower.add(section("待办事项", createTasks()), grid);
        grid.gridx = 1;
        grid.weightx = 0.40;
        grid.insets = new java.awt.Insets(0, 12, 0, 0);
        lower.add(section("快捷入口", createServices()), grid);
        body.add(lower);
        JPanel filler = new JPanel();
        filler.setOpaque(false);
        body.add(filler);
        return body;
    }

    private JPanel statCard(String label, String value, String icon, int colorIndex) {
        RoundedPanel card = new RoundedPanel(new BorderLayout(10, 0), 16, UiTheme.SURFACE);
        card.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 14));
        JPanel text = new JPanel(new GridLayout(2, 1, 0, 5));
        text.setOpaque(false);
        JLabel caption = new JLabel(label);
        caption.setForeground(UiTheme.MUTED);
        JLabel number = new JLabel(value);
        number.setForeground(STAT_COLORS[colorIndex]);
        number.setFont(UiTheme.font(Font.BOLD, 31F));
        text.add(caption);
        text.add(number);
        card.add(text, BorderLayout.CENTER);
        RoundedPanel badge = new RoundedPanel(new BorderLayout(), 44,
                STAT_COLORS[colorIndex]);
        badge.setPreferredSize(new Dimension(44, 44));
        badge.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        badge.add(new JLabel(UiIcons.load(icon + "-light", 22)));
        JPanel badgeArea = new JPanel(new GridBagLayout());
        badgeArea.setOpaque(false);
        badgeArea.add(badge);
        card.add(badgeArea, BorderLayout.EAST);
        return card;
    }
    private JPanel createTasks() {
        JPanel list = new JPanel(new GridLayout(3, 1, 0, 8));
        list.setOpaque(false);
        list.add(infoRow("course", "完善本学期课程安排", "查看 →"));
        list.add(infoRow("return", "归还即将到期的图书", "查看 →"));
        list.add(infoRow("user", "阅读新的校园通知", "查看 →"));
        return list;
    }

    private JPanel createServices() {
        JPanel grid = new JPanel(new GridLayout(2, 3, 14, 14));
        grid.setOpaque(false);
        for (int index = 0; index < SERVICES.length; index++) {
            JPanel card = new RoundedPanel(new BorderLayout(0, 2), 12,
                    new Color(235, 235, 231));
            card.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
            JLabel icon = UiIcons.responsiveLabel(SERVICES[index][0], 20,
                    SwingConstants.CENTER);
            JLabel title = new JLabel(SERVICES[index][1], SwingConstants.CENTER);
            title.setForeground(UiTheme.TEXT);
            title.setFont(UiTheme.font(Font.PLAIN, 14F));
            card.add(icon, BorderLayout.CENTER);
            card.add(title, BorderLayout.SOUTH);
            final String page = SERVICES[index][2];
            card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            card.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent event) {
                    if (navigator != null) {
                        navigator.handle(page);
                    }
                }
            });
            grid.add(card);
        }
        return grid;
    }

    private JPanel section(String title, JPanel content) {
        RoundedPanel panel = new RoundedPanel(new BorderLayout(0, 12), 16,
                UiTheme.SURFACE);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        JLabel heading = new JLabel(title);
        heading.setForeground(UiTheme.TEXT);
        heading.setFont(UiTheme.font(Font.BOLD, 20F));
        panel.add(heading, BorderLayout.NORTH);
        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    private JPanel infoRow(String icon, String title, String action) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        JLabel text = new JLabel(title, UiIcons.load(icon, 20), JLabel.LEFT);
        text.setIconTextGap(12);
        text.setFont(UiTheme.font(Font.PLAIN, 17F));
        text.setForeground(UiTheme.TEXT);
        row.add(text, BorderLayout.CENTER);
        JLabel link = new JLabel(action);
        link.setForeground(UiTheme.ACCENT);
        link.setFont(UiTheme.font(Font.BOLD, 14F));
        row.add(link, BorderLayout.EAST);
        return row;
    }
}
