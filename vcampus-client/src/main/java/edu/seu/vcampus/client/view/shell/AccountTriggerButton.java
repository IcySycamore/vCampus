package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.client.view.component.RoundedPanel;
import edu.seu.vcampus.client.view.theme.UiIcons;
import edu.seu.vcampus.client.view.theme.UiTheme;
import edu.seu.vcampus.common.user.entity.Role;
import edu.seu.vcampus.common.user.entity.SessionEntry;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * 顶栏账户触发按钮：头像 + 姓名 + 「身份 · 登录名」+ 展开箭头，整块都是点击热区。
 *
 * <p>
 * 刻意做成一枚<b>看得出来的按钮</b>：常态白底 + 细边框 + 圆角，悬停时底色变浅蓝、描边加深， 按下时再深一档，右端还有「▾」提示可以展开——否则光看一块静态头像，用户不会知道能点。
 */
public class AccountTriggerButton extends JButton {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 圆角半径。 */
    private static final int ARC = 22;

    /** 悬停底色。 */
    private static final Color HOVER = new Color(233, 242, 249);

    /** 按下底色。 */
    private static final Color PRESSED = new Color(216, 232, 244);

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

        setLayout(new BorderLayout(10, 0));
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 12));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setToolTipText("账户：查看资料、修改密码或退出登录");
        add(createAvatar(), BorderLayout.WEST);
        add(createText(displayName, role, userName), BorderLayout.CENTER);
        add(createArrow(), BorderLayout.EAST);
    }

    /** 画圆角底板 + 描边，再让父类绘制内容（文本/图标）。 */
    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D canvas = (Graphics2D) graphics.create();
        canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        ButtonModel model = getModel();
        Color fill = UiTheme.SURFACE;
        if (model.isPressed()) {
            fill = PRESSED;
        } else if (model.isRollover()) {
            fill = HOVER;
        }
        Color line = model.isRollover() || model.isPressed() ? UiTheme.NAVY_LIGHT : UiTheme.BORDER;
        int width = getWidth() - 1;
        int height = getHeight() - 1;
        canvas.setColor(fill);
        canvas.fillRoundRect(0, 0, width, height, ARC, ARC);
        canvas.setColor(line);
        canvas.drawRoundRect(0, 0, width, height, ARC, ARC);
        canvas.dispose();
        super.paintComponent(graphics);
    }

    private JPanel createAvatar() {
        RoundedPanel avatar = new RoundedPanel(new BorderLayout(), 24, UiTheme.NAVY);
        avatar.setBorder(BorderFactory.createEmptyBorder(6, 7, 6, 7));
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

    /** 右端展开箭头（提示可以点开）。 */
    private JLabel createArrow() {
        JLabel arrow = new JLabel("▾");
        arrow.setForeground(UiTheme.MUTED);
        arrow.setFont(UiTheme.font(Font.BOLD, 13F));
        return arrow;
    }

    private static boolean blank(String value) {
        return value == null || value.trim().length() == 0;
    }

    /** 角色显示名；无角色时用占位符。 */
    private static String roleName(Role role) {
        return role == null ? "-" : role.getDisplayName();
    }
}
