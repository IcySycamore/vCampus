package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.client.api.ClientApis;
import edu.seu.vcampus.client.view.UiTasks;
import edu.seu.vcampus.client.view.component.ProportionalLayout;
import edu.seu.vcampus.client.view.component.RoundedPanel;
import edu.seu.vcampus.client.view.component.StatCardPanel;
import edu.seu.vcampus.client.view.theme.UiIcons;
import edu.seu.vcampus.client.view.theme.UiTheme;
import edu.seu.vcampus.common.library.entity.BorrowRecord;
import edu.seu.vcampus.common.student.dto.ModifyRequestQuery;
import edu.seu.vcampus.common.student.entity.ModifyRequestStatus;
import edu.seu.vcampus.common.student.entity.StudentModifyRequest;
import edu.seu.vcampus.common.user.entity.Role;
import edu.seu.vcampus.common.user.entity.SessionEntry;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * 登录后独立显示的校园工作台。
 *
 * <p>
 * 四个统计卡与「待办事项」全部来自真实接口：待办数量 = 待办条数，借阅数量 = 本人未归还的借阅，
 * 待办内容按角色取自「未归还/即将到期图书」「待审核的学籍修改申请」「未排课课程」。原来这三条待办 与「借阅图书 2」都是写死的常量，与库里数据无关。
 */
public class OaDashboardPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    /** 到期前多少天内算「即将到期」。 */
    private static final long DUE_SOON_MILLIS = 7L * 24L * 60L * 60L * 1000L;

    private static final Color[] STAT_COLORS = { new Color(43, 103, 153),
            new Color(41, 128, 185), new Color(43, 132, 94), new Color(196, 125, 38) };

    private final StringHandler navigator;
    private final SessionEntry m_session;
    private final ClientApis m_apis;

    private final JPanel statistics = new JPanel(new GridLayout(1, 4, 20, 0));
    private final JPanel taskList = new JPanel(new GridLayout(0, 1, 0, 8));

    /** 创建只读校园工作台。 */
    public OaDashboardPanel() {
        this(null, (SessionEntry) null, null);
    }

    /**
     * 创建可跳转的校园工作台。
     *
     * @param navigator 页面跳转回调
     */
    public OaDashboardPanel(StringHandler navigator) {
        this(null, (SessionEntry) null, navigator);
    }

    /**
     * 创建带问候信息的校园工作台（无接口数据，统计显示占位）。
     *
     * @param userId    用户 ID
     * @param role      当前身份
     * @param navigator 页面跳转回调
     */
    public OaDashboardPanel(String userId, String role, StringHandler navigator) {
        this(null, new SessionEntry(null, userId, role, 0L), navigator);
    }

    /**
     * 创建带问候信息的校园工作台（无接口数据，统计显示占位）。
     *
     * @param session   当前会话；null 表示无身份
     * @param navigator 页面跳转回调
     */
    public OaDashboardPanel(SessionEntry session, StringHandler navigator) {
        this(null, session, navigator);
    }

    /**
     * 创建接入各模块的工作台：统计与待办都取真实数据。
     *
     * @param apis      各模块 API；null 时统计显示占位、待办只给引导语
     * @param session   当前会话
     * @param navigator 页面跳转回调
     */
    public OaDashboardPanel(ClientApis apis, SessionEntry session, StringHandler navigator) {
        this.m_apis = apis;
        this.m_session = session;
        this.navigator = navigator;
        setLayout(new ProportionalLayout(ProportionalLayout.VERTICAL, 20, 0.10F, 0.90F));
        setBackground(UiTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(18, 22, 22, 22));
        add(createGreeting());
        add(createDashboard());
        renderStatistics(null, null);
        renderTasks(new ArrayList<TaskRow>());
        load();
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
        statistics.setOpaque(false);
        body.add(statistics);
        JPanel lower = new JPanel(new GridBagLayout());
        lower.setOpaque(false);
        GridBagConstraints grid = new GridBagConstraints();
        grid.fill = GridBagConstraints.BOTH;
        grid.weighty = 1;
        grid.weightx = 0.60;
        grid.insets = new java.awt.Insets(0, 0, 0, 12);
        lower.add(section("待办事项", taskList), grid);
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

    /** 重画四张统计卡；{@code todoText}/{@code borrowText} 为 null 时显示占位。 */
    private void renderStatistics(String todoText, String borrowText) {
        statistics.removeAll();
        statistics.add(statCard("校园服务",
                String.valueOf(DashboardServicesPanel.visibleCount(roleOf())), "student", 0));
        statistics.add(statCard("当前身份", roleName(), "user", 1));
        statistics.add(statCard("待办事项", todoText == null ? "…" : todoText, "course", 2));
        statistics.add(statCard("借阅图书", borrowText == null ? "…" : borrowText, "library", 3));
        statistics.revalidate();
        statistics.repaint();
    }

    /**
     * 重画待办列表。
     *
     * @param tasks 待办；空列表显示一条引导语
     */
    private void renderTasks(List<TaskRow> tasks) {
        taskList.removeAll();
        if (tasks.isEmpty()) {
            JPanel empty = new JPanel(new BorderLayout());
            empty.setOpaque(false);
            JLabel hint = new JLabel(m_apis == null ? "登录后可查看待办事项"
                    : "暂时没有待办事项");
            hint.setForeground(UiTheme.MUTED);
            hint.setFont(UiTheme.font(Font.PLAIN, 17F));
            empty.add(hint, BorderLayout.WEST);
            taskList.add(empty);
        } else {
            for (TaskRow task : tasks) {
                taskList.add(infoRow(task));
            }
        }
        taskList.revalidate();
        taskList.repaint();
    }

    /** 从各模块取真实数据，回到 EDT 后刷新卡片与待办。 */
    private void load() {
        if (m_apis == null) {
            return;
        }
        UiTasks.run(new UiTasks.Task<Snapshot>() {
            @Override
            public Snapshot run() {
                return collect();
            }
        }, new UiTasks.Success<Snapshot>() {
            @Override
            public void accept(Snapshot snapshot) {
                List<TaskRow> tasks = deriveTasks(roleOf(), snapshot);
                renderStatistics(String.valueOf(tasks.size()), borrowText(snapshot));
                renderTasks(tasks);
            }
        }, UiTasks.failureWithDialog(this, "工作台数据加载失败", null));
    }

    private Snapshot collect() {
        Snapshot snapshot = new Snapshot();
        if (m_apis.library() != null && roleOf() != Role.ADMIN) {
            List<BorrowRecord> borrows = m_apis.library().listMyBorrows();
            long now = System.currentTimeMillis();
            if (borrows != null) {
                for (BorrowRecord record : borrows) {
                    if (record.getReturnedAt() != null) {
                        continue;
                    }
                    snapshot.notReturned++;
                    Date due = record.getDueAt();
                    if (due == null) {
                        continue;
                    }
                    long remain = due.getTime() - now;
                    if (remain < 0) {
                        snapshot.overdue++;
                    } else if (remain <= DUE_SOON_MILLIS) {
                        snapshot.dueSoon++;
                    }
                }
            }
        }
        if (m_apis.course() != null) {
            if (roleOf() == Role.ADMIN) {
                snapshot.unscheduled = countUnscheduled(m_apis.course().listCourses());
            } else if (roleOf() == Role.TEACHER) {
                snapshot.unscheduled = countUnscheduled(m_apis.course().listMyTeachingCourses());
            } else {
                snapshot.selected = size(m_apis.course().listMySelections());
            }
        }
        if (m_apis.student() != null) {
            ModifyRequestQuery query = new ModifyRequestQuery(ModifyRequestStatus.PENDING);
            query.setPageSize(1);
            if (roleOf() == Role.STUDENT) {
                query.setApplicantUuid(m_session == null ? null : m_session.getUuid());
            }
            snapshot.pendingAudit = total(m_apis.student().listModifyRequests(query));
        }
        return snapshot;
    }

    private static int countUnscheduled(List<edu.seu.vcampus.common.course.Course> courses) {
        int count = 0;
        if (courses != null) {
            for (edu.seu.vcampus.common.course.Course course : courses) {
                if (course.getTimeslot() == null) {
                    count++;
                }
            }
        }
        return count;
    }

    private static int size(List<?> list) {
        return list == null ? 0 : list.size();
    }

    private static int total(edu.seu.vcampus.common.message.PageResponse<?> page) {
        return page == null ? 0 : (int) page.getTotal();
    }

    /** @return 借阅卡的展示文案：管理员不使用读者额度，其余按未归还本数。 */
    private String borrowText(Snapshot snapshot) {
        return roleOf() == Role.ADMIN ? "—" : String.valueOf(snapshot.notReturned);
    }

    /**
     * 按角色把统计数字翻译成待办条目。
     *
     * @param role     当前角色
     * @param snapshot 统计快照
     * @return 待办列表（可能为空）
     */
    static List<TaskRow> deriveTasks(Role role, Snapshot snapshot) {
        List<TaskRow> tasks = new ArrayList<TaskRow>();
        if (snapshot == null) {
            return tasks;
        }
        if (snapshot.overdue > 0) {
            tasks.add(new TaskRow("return", "有 " + snapshot.overdue + " 本图书已逾期，需归还并结清滞纳金",
                    PageNames.LIBRARY));
        }
        if (snapshot.dueSoon > 0) {
            tasks.add(new TaskRow("return", "有 " + snapshot.dueSoon + " 本图书 7 天内到期",
                    PageNames.LIBRARY));
        }
        if (snapshot.pendingAudit > 0) {
            tasks.add(role == Role.STUDENT
                    ? new TaskRow("user", "有 " + snapshot.pendingAudit + " 条学籍修改申请在审核中",
                            PageNames.STUDENT)
                    : new TaskRow("user", "有 " + snapshot.pendingAudit + " 条学籍修改申请待审核",
                            PageNames.STUDENT));
        }
        if (snapshot.unscheduled > 0) {
            tasks.add(new TaskRow("course",
                    role == Role.TEACHER
                            ? "有 " + snapshot.unscheduled + " 门你授课的课程未排课"
                            : "有 " + snapshot.unscheduled + " 门课程未排课",
                    PageNames.COURSE));
        }
        if (role == Role.STUDENT && snapshot.selected == 0) {
            tasks.add(new TaskRow("course", "本学期还没有选课", PageNames.COURSE));
        }
        return tasks;
    }

    /** 待办统计快照。 */
    static final class Snapshot {

        /** 未归还本数。 */
        int notReturned;

        /** 7 天内到期的本数。 */
        int dueSoon;

        /** 已逾期的本数。 */
        int overdue;

        /** 未排课的课程门数（管理员=全部课程，教师=本人授课）。 */
        int unscheduled;

        /** 待审核的学籍修改申请条数（学生=本人申请）。 */
        int pendingAudit;

        /** 本人已选课程门数（仅学生）。 */
        int selected;
    }

    /** 一条待办：图标、文案、跳转目标页。 */
    static final class TaskRow {

        private final String icon;
        private final String title;
        private final String page;

        TaskRow(String icon, String title, String page) {
            this.icon = icon;
            this.title = title;
            this.page = page;
        }

        String title() {
            return title;
        }

        String page() {
            return page;
        }

        @Override
        public String toString() {
            return title;
        }
    }

    private JPanel statCard(String label, String value, String icon, int colorIndex) {
        return new StatCardPanel(label, value, icon, STAT_COLORS[colorIndex]);
    }

    private JPanel section(String title, JPanel content) {
        RoundedPanel panel = new RoundedPanel(new BorderLayout(0, 12), 16, UiTheme.SURFACE);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        JLabel heading = new JLabel(title);
        heading.setForeground(UiTheme.TEXT);
        heading.setFont(UiTheme.font(Font.BOLD, 20F));
        panel.add(heading, BorderLayout.NORTH);
        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    private JPanel infoRow(final TaskRow task) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        JLabel text = new JLabel(task.title, UiIcons.load(task.icon, 20), JLabel.LEFT);
        text.setIconTextGap(12);
        text.setFont(UiTheme.font(Font.PLAIN, 17F));
        text.setForeground(UiTheme.TEXT);
        row.add(text, BorderLayout.CENTER);
        JLabel link = new JLabel("查看 →");
        link.setForeground(UiTheme.ACCENT);
        link.setFont(UiTheme.font(Font.BOLD, 14F));
        row.add(link, BorderLayout.EAST);
        // 待办要能直接跳到对应页面，否则只是一句提示
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        row.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (navigator != null && task.page != null) {
                    navigator.handle(task.page);
                }
            }
        });
        return row;
    }
}
