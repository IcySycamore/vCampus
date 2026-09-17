package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.client.view.component.ProportionalLayout;
import edu.seu.vcampus.client.view.component.RoundedPanel;
import edu.seu.vcampus.client.view.component.StatCardPanel;
import edu.seu.vcampus.client.view.theme.UiIcons;
import edu.seu.vcampus.client.view.theme.UiTheme;
import edu.seu.vcampus.common.user.entity.Role;
import edu.seu.vcampus.common.user.entity.SessionEntry;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * 登录后独立显示的校园工作台。
 */
public class OaDashboardPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final Color[] STAT_COLORS = { new Color(43, 103, 153),
            new Color(41, 128, 185), new Color(43, 132, 94), new Color(196, 125, 38) };
    private final StringHandler navigator;
    private final SessionEntry m_session;

    /** 创建只读校园工作台。 */
    public OaDashboardPanel() {
        this((SessionEntry) null, null);
    }

    /**
     * 创建可跳转的校园工作台。
     *
     * @param navigator 页面跳转回调
     */
    public OaDashboardPanel(StringHandler navigator) {
        this((SessionEntry) null, navigator);
    }

    /**
     * 创建带问候信息的校园工作台。
     *
     * @param userId    用户 ID
     * @param role      当前身份
     * @param navigator 页面跳转回调
     */
    public OaDashboardPanel(String userId, String role, StringHandler navigator) {
        this(new SessionEntry(null, userId, role, 0L), navigator);
    }

    /**
     * 创建带问候信息的校园工作台（显示名取自会话）。
     *
     * @param session   当前会话；null 表示无身份
     * @param navigator 页面跳转回调
     */
    public OaDashboardPanel(SessionEntry session, StringHandler navigator) {
        this.m_session = session;
        this.navigator = navigator;
        setLayout(new ProportionalLayout(ProportionalLayout.VERTICAL, 20,
                0.10F, 0.90F));
        setBackground(UiTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(18, 22, 22, 22));
        add(createGreeting());
        add(createDashboard());
    }

    /** 显示名：优先姓名，缺姓名时用登录名（管理员天然走这条）。 */
    private String displayName() {
        if (m_session == null) {
            return "用户";
        }
        String name = m_session.getDisplayName();
        if (name != null && name.trim().length() > 0) {
            return name.trim();
        }
        String userName = m_session.getUsername();
        return userName == null || userName.trim().length() == 0 ? "用户" : userName.trim();
    }

    /** 当前角色；无会话时为 null。 */
    private Role roleOf() {
        return m_session == null ? null : Role.fromDisplayName(m_session.getRole());
    }

    /** 角色显示名。 */
    private String roleName() {
        Role role = roleOf();
        return role == null ? "-" : role.getDisplayName();
    }

    private JPanel createGreeting() {
        RoundedPanel card = new RoundedPanel(new BorderLayout(), 18, UiTheme.SURFACE);
        card.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));
        JLabel greeting = new JLabel("你好，" + displayName());
        greeting.setForeground(UiTheme.TEXT);
        greeting.setFont(UiTheme.font(Font.BOLD, 29F));
        card.add(greeting, BorderLayout.WEST);
        JLabel identity = new JLabel(roleName());
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
        statistics.add(statCard("校园服务",
                String.valueOf(DashboardServicesPanel.visibleCount(roleOf())), "student", 0));
        statistics.add(statCard("当前身份", roleName(), "user", 1));
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
        lower.add(section("快捷入口", new DashboardServicesPanel(roleOf(), navigator)), grid);
        body.add(lower);
        JPanel filler = new JPanel();
        filler.setOpaque(false);
        body.add(filler);
        return body;
    }

    private JPanel statCard(String label, String value, String icon, int colorIndex) {
        return new StatCardPanel(label, value, icon, STAT_COLORS[colorIndex]);
    }

    private JPanel createTasks() {
        JPanel list = new JPanel(new GridLayout(3, 1, 0, 8));
        list.setOpaque(false);
        list.add(infoRow("course", "完善本学期课程安排", "查看 →"));
        list.add(infoRow("return", "归还即将到期的图书", "查看 →"));
        list.add(infoRow("user", "阅读新的校园通知", "查看 →"));
        return list;
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
