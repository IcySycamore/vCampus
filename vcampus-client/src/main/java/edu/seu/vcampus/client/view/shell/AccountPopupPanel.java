package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.client.view.component.RoundedPanel;
import edu.seu.vcampus.client.view.theme.UiTheme;
import edu.seu.vcampus.common.user.entity.Role;
import edu.seu.vcampus.common.user.entity.SessionEntry;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.plaf.basic.BasicButtonUI;

/**
 * 右上角账户弹窗的内容面板：**只做展示与回调**，不含任何网络调用。
 *
 * <p>
 * 版式：顶部身份卡（{@link AccountIdentityPanel}）→ 分隔线 → 登录名与账户标识的键值两列 → 分隔线 → 等宽的两个动作按钮（修改密码 /
 * 退出登录）。姓名缺失时回退显示登录名——管理员没有姓名概念， 天然走这条路径。
 */
public class AccountPopupPanel extends JPanel {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 弹窗宽度。 */
    private static final int POPUP_WIDTH = 320;

    /** 弹窗高度：留足空间，让身份卡、键值两行与两个按钮都完整显示。 */
    private static final int POPUP_HEIGHT = 330;

    /** 身份卡。 */
    private final AccountIdentityPanel m_identity;

    /** 登录名与账户标识。 */
    private final AccountFactsPanel m_facts;

    /** 修改密码按钮（主操作）。 */
    private final JButton m_password = createFillButton("修改密码");

    /** 退出登录按钮（次要操作，与主按钮同尺寸同形状）。 */
    private final JButton m_logout = createOutlineButton("退出登录");

    /**
     * 构造账户弹窗内容。
     *
     * @param session          当前会话；null 表示未登录，全部字段显示占位
     * @param onChangePassword 点击「修改密码」的回调；null 表示该按钮不响应
     * @param onLogout         点击「退出登录」的回调；null 表示该按钮不响应
     */
    public AccountPopupPanel(SessionEntry session, final Runnable onChangePassword,
            final Runnable onLogout) {
        String uuid = uuidOf(session);
        m_identity = new AccountIdentityPanel(displayNameOf(session), roleNameOf(session));
        m_facts = new AccountFactsPanel(userNameOf(session), uuid);
        m_facts.uuidTooltip(uuid);
        setLayout(new BorderLayout());
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder());
        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 18), 14, UiTheme.SURFACE);
        card.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        card.add(m_identity, BorderLayout.NORTH);
        card.add(m_facts, BorderLayout.CENTER);
        card.add(createActions(onChangePassword, onLogout), BorderLayout.SOUTH);
        add(card, BorderLayout.CENTER);
        setPreferredSize(new Dimension(POPUP_WIDTH, POPUP_HEIGHT));
    }

    /** 底部动作区：等宽两个按钮。 */
    private JPanel createActions(final Runnable onChangePassword, final Runnable onLogout) {
        m_password.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (onChangePassword != null) {
                    onChangePassword.run();
                }
            }
        });
        m_logout.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (onLogout != null) {
                    onLogout.run();
                }
            }
        });
        JPanel buttons = new JPanel(new GridLayout(2, 1, 0, 8));
        buttons.setOpaque(false);
        buttons.add(m_password);
        buttons.add(m_logout);
        JPanel actions = new JPanel(new BorderLayout(0, 12));
        actions.setOpaque(false);
        actions.add(createSeparator(), BorderLayout.NORTH);
        actions.add(buttons, BorderLayout.CENTER);
        return actions;
    }

    /** 1 像素分隔线。 */
    private JPanel createSeparator() {
        JPanel line = new JPanel();
        line.setBackground(UiTheme.BORDER);
        line.setPreferredSize(new Dimension(0, 1));
        return line;
    }

    /** 主操作按钮：深蓝填充 + 白字。 */
    private static JButton createFillButton(String text) {
        JButton button = prepareButton(text);
        button.setBackground(UiTheme.NAVY);
        button.setForeground(UiTheme.SURFACE);
        button.setBorder(BorderFactory.createEmptyBorder());
        return button;
    }

    /** 次要按钮：白底 + 细描边 + 深字。与主按钮同尺寸同形状，避免「一蓝一灰」的跳脱感。 */
    private static JButton createOutlineButton(String text) {
        JButton button = prepareButton(text);
        button.setBackground(UiTheme.SURFACE);
        button.setForeground(UiTheme.TEXT);
        button.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER));
        return button;
    }

    /** 两个动作按钮共用的基础样式：等高、居中、手型。 */
    private static JButton prepareButton(String text) {
        JButton button = new JButton(text);
        button.setUI(new BasicButtonUI());
        button.setOpaque(true);
        button.setFont(UiTheme.font(Font.BOLD, 13F));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setFocusPainted(false);
        button.setHorizontalAlignment(SwingConstants.CENTER);
        button.setPreferredSize(new Dimension(0, 38));
        return button;
    }

    /** 姓名：缺姓名时回退登录名，再缺则占位。 */
    private String displayNameOf(SessionEntry session) {
        if (session == null) {
            return "-";
        }
        String name = session.getDisplayName();
        return blank(name) ? userNameOf(session) : name;
    }

    private String userNameOf(SessionEntry session) {
        return session == null || blank(session.getUsername()) ? "-" : session.getUsername();
    }

    private String roleNameOf(SessionEntry session) {
        Role role = session == null ? null : Role.fromDisplayName(session.getRole());
        return role == null ? "-" : role.getDisplayName();
    }

    private String uuidOf(SessionEntry session) {
        return session == null || blank(session.getUuid()) ? "-" : session.getUuid();
    }

    private boolean blank(String value) {
        return value == null || value.trim().length() == 0;
    }

    /** @return 姓名文本（缺姓名时为登录名） */
    public String getDisplayNameText() {
        return m_identity.displayNameText();
    }

    /** @return 身份文本（如「学生」） */
    public String getRoleText() {
        return m_identity.roleText();
    }

    /** @return 登录名 */
    public String getUserNameText() {
        return m_facts.userNameText();
    }

    /** @return 账户标识 */
    public String getUuidText() {
        return m_facts.uuidText();
    }

    /** @return 修改密码按钮 */
    public JButton getChangePasswordButton() {
        return m_password;
    }

    /** @return 退出登录按钮 */
    public JButton getLogoutButton() {
        return m_logout;
    }
}
