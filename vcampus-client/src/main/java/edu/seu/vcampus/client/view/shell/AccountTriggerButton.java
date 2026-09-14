package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.client.view.component.RoundedPanel;
import edu.seu.vcampus.client.view.theme.UiIcons;
import edu.seu.vcampus.client.view.theme.UiTheme;
import edu.seu.vcampus.common.user.entity.Role;
import edu.seu.vcampus.common.user.entity.SessionEntry;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * 顶栏账户触发按钮：头像 + 姓名 + 「身份 · 登录名」，整块都是点击热区。
 *
 * <p>
 * 从 {@code MainHeaderPanel} 抽出（原文件破 200 行上限）。按钮<b>只负责外观</b>，点击后弹什么由宿主决定，
 * 因此它可以在测试里独立断言「有手型光标、文本取自会话」。
 */
public class AccountTriggerButton extends JButton {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /**
     * 创建账户触发按钮。
     *
     * @param session 当前会话；null 表示未登录
     */
    public AccountTriggerButton(SessionEntry session) {
        String userName = blank(session == null ? null : session.getUsername()) ? "-"
                : session.getUsername();
        String displayName = blank(session == null ? null : session.getDisplayName()) ? userName
                : session.getDisplayName();
        Role role = session == null ? null : Role.fromDisplayName(session.getRole());

        setLayout(new BorderLayout(8, 0));
        setOpaque(false);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setToolTipText("账户：查看资料、修改密码或退出登录");
        add(createAvatar(), BorderLayout.WEST);
        add(createText(displayName, role, userName), BorderLayout.CENTER);
    }

    private JPanel createAvatar() {
        RoundedPanel avatar = new RoundedPanel(new BorderLayout(), 24, UiTheme.NAVY);
        avatar.setBorder(BorderFactory.createEmptyBorder(7, 8, 7, 8));
        avatar.add(new JLabel(UiIcons.load("user-light", 18)));
        return avatar;
    }

    private JPanel createText(String displayName, Role role, String userName) {
        JPanel text = new JPanel(new GridLayout(2, 1, 0, 1));
        text.setOpaque(false);
        JLabel name = new JLabel(displayName);
        name.setForeground(UiTheme.TEXT);
        name.setFont(UiTheme.font(Font.BOLD, 13F));
        JLabel identity = new JLabel(roleName(role) + " · " + userName);
        identity.setForeground(UiTheme.MUTED);
        identity.setFont(UiTheme.font(Font.PLAIN, 11F));
        text.add(name);
        text.add(identity);
        return text;
    }

    private static boolean blank(String value) {
        return value == null || value.trim().length() == 0;
    }

    /** 角色显示名；无角色时用占位符。 */
    private static String roleName(Role role) {
        return role == null ? "-" : role.getDisplayName();
    }
}
