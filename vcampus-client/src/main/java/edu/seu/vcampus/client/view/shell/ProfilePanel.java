package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.client.student.StudentService;
import edu.seu.vcampus.client.view.component.ModernTabbedPaneUI;
import edu.seu.vcampus.client.view.component.RoundedPanel;
import edu.seu.vcampus.client.view.theme.UiTheme;
import edu.seu.vcampus.common.user.entity.Capability;
import edu.seu.vcampus.common.user.entity.Permissions;
import edu.seu.vcampus.common.user.entity.Role;
import edu.seu.vcampus.common.user.entity.SessionEntry;

import java.awt.BorderLayout;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

/**
 * 个人信息页：学生与教师共用（组长要求「教师也有信息查看需求」）。
 *
 * <p>
 * 身份卡的数据直接来自登录会话，登录后首屏就有，不必等请求返回；档案明细交给
 * {@link ProfileDetailPanel}，那里调 201 取数。
 *
 * <p>
 * 「学籍管理」（需要 {@code STUDENT_VIEW_ALL}）只挂给<b>没有</b>用户管理权限的角色（教师）：
 * 管理员在用户中心的管理控制台里管学籍，避免出现两个管理入口；面板实现仍是同一份
 * {@link StudentManagePanel}——教师拿到的是一份<b>只读</b>的它：能查、能翻页，但操作栏是空的。
 * 再往下看「修改审核」（需要 {@code STUDENT_MODIFY_AUDIT}）——教师也看不到，审核权是管理员的。
 * 页签按 {@link Permissions} 逐项决定出不出现（ADR-0009 D6）。客户端判定只管「显示与否」，
 * 服务端 403 才是最终防线。
 *
 * <p>
 * 学生在这里多一个「我的申请」（{@link MyRequestsPanel}）：申请交出去之后得有地方看进展，
 * 否则「学生申请 → 教务审核」对学生就是个黑盒。它与「修改审核」互斥地出现——提的人和批的人
 * 看的是同一条命令的两条视角。
 *
 * <p>
 * 注册入口不在这里：注册需要管理员会话，已由用户中心的 {@code UserManagePanel} 承担，
 * 避免两处各写一套入口。
 */
public class ProfilePanel extends JPanel {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 当前会话记录；未登录时为 null。 */
    private final SessionEntry m_session;

    /** 学籍 API（取本人档案明细用）；未装配时可为 null。 */
    private final StudentService m_student;

    /**
     * 创建个人信息页。
     *
     * @param session 当前会话记录；未登录时可为 null
     * @param student 学籍 API；未装配时可为 null
     */
    public ProfilePanel(SessionEntry session, StudentService student) {
        this.m_session = session;
        this.m_student = student;
        setLayout(new BorderLayout(0, 18));
        setBackground(UiTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(18, 22, 22, 22));
        add(createIdentityCard(), BorderLayout.NORTH);
        add(createBody(), BorderLayout.CENTER);
    }

    /** 身份卡：姓名取自会话，登录后立刻就有。 */
    private JPanel createIdentityCard() {
        RoundedPanel card = new RoundedPanel(new BorderLayout(), 18, UiTheme.SURFACE);
        card.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));
        JLabel name = new JLabel(m_session == null || m_session.getDisplayName() == null
                ? "未登录"
                : m_session.getDisplayName());
        name.setForeground(UiTheme.TEXT);
        name.setFont(UiTheme.font(Font.BOLD, 29F));
        card.add(name, BorderLayout.WEST);
        JLabel identity = new JLabel(roleText());
        identity.setForeground(UiTheme.MUTED);
        identity.setFont(UiTheme.font(Font.BOLD, 13F));
        card.add(identity, BorderLayout.EAST);
        return card;
    }

    /**
     * 主体：本人档案，再按能力追加页签（学籍管理 / 修改审核 / 我的申请）。
     *
     * <p>
     * 页签逐个按 {@link Permissions} 决定出不出现，角色之间互不牵扯：学生只多一个「我的申请」，
     * 教师多一个只读的「学籍管理」，管理员的学籍管理在用户中心（避免两个管理入口）。
     *
     * @return 主体面板；学籍 API 未装配时是一张只读卡
     */
    private JComponent createBody() {
        Role role = m_session == null ? null : Role.fromDisplayName(m_session.getRole());
        // 未装配 API（预览、单测）时只给一张只读卡，不去构造会立刻发请求的面板。
        if (m_student == null) {
            return card("在校档案", new ProfileDetailPanel(m_student, role));
        }
        JTabbedPane tabs = new JTabbedPane();
        tabs.setUI(new ModernTabbedPaneUI());
        tabs.addTab("我的档案", card("在校档案", new ProfileDetailPanel(m_student, role)));
        // 管理员的学籍管理收在用户中心（组长：用户管理与学籍管理功能重复），这里只留给教师。
        if (Permissions.can(role, Capability.STUDENT_VIEW_ALL)
                && !Permissions.can(role, Capability.USER_MANAGE)) {
            tabs.addTab("学籍管理", new StudentManagePanel(m_student, role));
        }
        if (Permissions.can(role, Capability.STUDENT_MODIFY_AUDIT)) {
            tabs.addTab("修改审核", new StudentModifyAuditPanel(m_student));
        }
        // 能提申请、没有审批权的人（学生）：申请交出去之后得有地方看结果。
        if (Permissions.can(role, Capability.STUDENT_MODIFY_APPLY)
                && !Permissions.can(role, Capability.STUDENT_MODIFY_AUDIT)) {
            tabs.addTab("我的申请", new MyRequestsPanel(m_student));
        }
        return tabs;
    }

    /**
     * 把内容包进一张带标题的卡片。
     *
     * @param title 卡片标题
     * @param content 卡片内容
     * @return 卡片面板
     */
    private static JPanel card(String title, JPanel content) {
        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 12), 18, UiTheme.SURFACE);
        card.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        JLabel heading = new JLabel(title);
        heading.setForeground(UiTheme.TEXT);
        heading.setFont(UiTheme.font(Font.BOLD, 17F));
        card.add(heading, BorderLayout.NORTH);
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    /** 取角色显示名；未登录返回空串。 */
    private String roleText() {
        return m_session == null || m_session.getRole() == null ? "" : m_session.getRole();
    }
}
