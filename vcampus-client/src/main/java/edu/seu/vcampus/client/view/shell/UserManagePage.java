package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.client.user.UserAdminService;
import edu.seu.vcampus.client.view.theme.UiTheme;

import java.awt.BorderLayout;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * 用户管理页（仅管理员可注册，由 {@code MainContentPanel} 按 {@code Permissions.can(role, USER_MANAGE)} 决定）。
 *
 * <p>
 * 原来这套能力塞在「用户中心」页里，管理员与非管理员看到的是同一页的不同分支；现在拆成两个东西： 账户弹窗（人人可用）与用户管理页（仅管理员），各自的入口与职责都清楚。
 */
public class UserManagePage extends JPanel {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /**
     * 构造用户管理页。
     *
     * @param api 用户管理 API
     * @throws IllegalArgumentException api 为 null
     */
    public UserManagePage(UserAdminService api) {
        if (api == null) {
            throw new IllegalArgumentException("api must not be null");
        }
        setLayout(new BorderLayout(0, 14));
        setBackground(UiTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(22, 26, 22, 26));
        add(createHeader(), BorderLayout.NORTH);
        add(new UserManagePanel(api), BorderLayout.CENTER);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout(0, 4));
        header.setOpaque(false);
        JLabel title = new JLabel("用户管理");
        title.setForeground(UiTheme.TEXT);
        title.setFont(UiTheme.font(Font.BOLD, 22F));
        JLabel subtitle = new JLabel("账号由管理员统一分配；新建或批量导入时会同步建立各模块档案");
        subtitle.setForeground(UiTheme.MUTED);
        subtitle.setFont(UiTheme.font(Font.PLAIN, 12F));
        header.add(title, BorderLayout.NORTH);
        header.add(subtitle, BorderLayout.SOUTH);
        return header;
    }
}
