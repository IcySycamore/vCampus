package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.client.VCampusClientApp;
import edu.seu.vcampus.client.user.ClientSession;
import edu.seu.vcampus.client.user.UserService;
import edu.seu.vcampus.client.view.component.RoundedPanel;
import edu.seu.vcampus.client.view.dialog.RegisterDialog;
import edu.seu.vcampus.client.view.theme.UiFactory;
import edu.seu.vcampus.client.view.theme.UiTheme;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * 个人信息页：学生与教师共用（组长要求「教师也有信息查看需求」）。
 *
 * <p>
 * 身份卡的数据直接来自登录会话，登录后首屏就有，不必等请求返回；档案明细交给
 * {@link ProfileDetailPanel}，那里调 201 取数。
 *
 * <p>
 * 管理员额外看到一个「注册新用户」入口——注册需要管理员会话，而登录页那个入口在登录前，
 * 拿不到会话，所以真正的注册动作放在这里。
 */
public class ProfilePanel extends JPanel {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 当前会话；未连接时可能为 null。 */
    private final ClientSession m_session;

    /**
     * 创建个人信息页。
     *
     * @param session 当前会话；未连接时可为 null
     */
    public ProfilePanel(ClientSession session) {
        this.m_session = session;
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
        JLabel name = new JLabel(m_session == null || m_session.getRealName() == null
                ? "未登录"
                : m_session.getRealName());
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
        card.add(new ProfileDetailPanel(), BorderLayout.CENTER);
        body.add(card, BorderLayout.NORTH);
        if (isAdministrator()) {
            body.add(createRegisterAction(), BorderLayout.SOUTH);
        }
        return body;
    }

    /** 注册入口：只有管理员看得到（服务端仍会再判一次权限）。 */
    private JPanel createRegisterAction() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        JButton register = UiFactory.primaryButton("注册新用户", "user");
        register.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                UserService service = VCampusClientApp.getUserService();
                new RegisterDialog(SwingUtilities.getWindowAncestor(ProfilePanel.this),
                        service).setVisible(true);
            }
        });
        panel.add(register, BorderLayout.EAST);
        return panel;
    }

    /** 取角色显示名；未登录返回空串。 */
    private String roleText() {
        return m_session == null || m_session.getRole() == null ? "" : m_session.getRole();
    }

    /** 当前用户是否为管理员（决定是否显示注册入口）。 */
    private boolean isAdministrator() {
        return "管理员".equals(roleText());
    }
}
