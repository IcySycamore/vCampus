package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.client.student.StudentService;
import edu.seu.vcampus.client.view.component.RoundedPanel;
import edu.seu.vcampus.client.view.theme.UiTheme;
import edu.seu.vcampus.common.user.entity.SessionEntry;

import java.awt.BorderLayout;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * 个人信息页：学生与教师共用（组长要求「教师也有信息查看需求」）。
 *
 * <p>
 * 身份卡的数据直接来自登录会话，登录后首屏就有，不必等请求返回；档案明细交给
 * {@link ProfileDetailPanel}，那里调 201 取数。
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

    /** 主体：档案明细 +（管理员）注册入口。 */
    private JPanel createBody() {
        JPanel body = new JPanel(new BorderLayout(0, 14));
        body.setOpaque(false);
        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 12), 18, UiTheme.SURFACE);
        card.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        JLabel title = new JLabel("在校档案");
        title.setForeground(UiTheme.TEXT);
        title.setFont(UiTheme.font(Font.BOLD, 17F));
        card.add(title, BorderLayout.NORTH);
        card.add(new ProfileDetailPanel(m_student), BorderLayout.CENTER);
        body.add(card, BorderLayout.NORTH);
        return body;
    }

    /** 取角色显示名；未登录返回空串。 */
    private String roleText() {
        return m_session == null || m_session.getRole() == null ? "" : m_session.getRole();
    }
}
