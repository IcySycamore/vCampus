package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.client.VCampusClientApp;
import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.client.user.UserService;
import edu.seu.vcampus.client.view.UiTasks;
import edu.seu.vcampus.client.view.component.RoundedPanel;
import edu.seu.vcampus.client.view.component.StatCardPanel;
import edu.seu.vcampus.client.view.theme.UiFactory;
import edu.seu.vcampus.client.view.theme.UiTheme;
import edu.seu.vcampus.common.user.entity.Capability;
import edu.seu.vcampus.common.user.entity.Permissions;
import edu.seu.vcampus.common.user.entity.Role;
import edu.seu.vcampus.common.user.entity.SessionEntry;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.SwingUtilities;

/**
 * 用户中心页：我的资料（只读）、修改密码、退出登录；管理员额外看到用户管理面板。
 *
 * <p>
 * 身份<b>只</b>来自服务端下发的会话记录（{@link UserService#currentSession()}），不采信界面入口的选择；
 * 管理区是否显示由共享的 {@link Permissions} 判定（客户端判定只用于显示，服务端 403 才是最终防线）。
 */
public class UserCenterPanel extends JPanel {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 用户管理 API。 */
    private final UserService m_api;

    /**
     * 构造用户中心页。
     *
     * @param api 用户管理 API
     * @throws IllegalArgumentException api 为 null
     */
    public UserCenterPanel(UserService api) {
        if (api == null) {
            throw new IllegalArgumentException("api must not be null");
        }
        this.m_api = api;
        setLayout(new BorderLayout(0, 16));
        setBackground(UiTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(28, 32, 28, 32));
        add(createProfileCard(), BorderLayout.NORTH);
        add(createBody(), BorderLayout.CENTER);
    }

    private JPanel createProfileCard() {
        SessionEntry entry = m_api.currentSession();
        // 主位显示姓名（服务端签发的会话保证姓名非空），登录名退到次要位置：
        // 组长反馈的「登录之后显示的都是用户名」，根因就是这里拿 getUsername() 当主显示。
        String displayName = entry == null ? "未登录" : entry.getDisplayName();
        String roleName = entry == null || Role.fromDisplayName(entry.getRole()) == null ? "-"
                : Role.fromDisplayName(entry.getRole()).getDisplayName();
        String userName = entry == null ? "-" : entry.getUsername();

        JPanel row = new JPanel(new GridLayout(1, 3, 14, 0));
        row.setOpaque(false);
        row.add(new StatCardPanel("当前用户", displayName, "user", UiTheme.ACCENT));
        row.add(new StatCardPanel("身份", roleName, "student", UiTheme.ACCENT_DARK));
        row.add(new StatCardPanel("登录名", userName, "library", UiTheme.MUTED));

        JPanel card = new JPanel(new BorderLayout(0, 12));
        card.setOpaque(false);
        JLabel heading = new JLabel("用户中心");
        heading.setForeground(UiTheme.TEXT);
        heading.setFont(UiTheme.font(Font.BOLD, 22F));
        card.add(heading, BorderLayout.NORTH);
        card.add(row, BorderLayout.CENTER);
        card.add(createAccountButtons(), BorderLayout.SOUTH);
        return card;
    }

    private JPanel createAccountButtons() {
        RoundedPanel bar = new RoundedPanel(
                new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 10, 8), 16, UiTheme.SURFACE);
        bar.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
        JButton password = UiFactory.primaryButton("修改密码", "lock");
        password.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                changePassword();
            }
        });
        bar.add(password);
        JButton logout = new JButton("退出登录");
        logout.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                logout();
            }
        });
        bar.add(logout);
        return bar;
    }

    private JPanel createBody() {
        SessionEntry entry = m_api.currentSession();
        Role role = entry == null ? null : Role.fromDisplayName(entry.getRole());
        if (!Permissions.can(role, Capability.USER_MANAGE)) {
            RoundedPanel hint = new RoundedPanel(new BorderLayout(), 18, UiTheme.SURFACE);
            hint.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
            JLabel text = new JLabel("普通账号可使用上方功能；用户管理仅对管理员开放。");
            text.setForeground(UiTheme.MUTED);
            hint.add(text, BorderLayout.CENTER);
            return hint;
        }
        RoundedPanel wrapper = new RoundedPanel(new BorderLayout(0, 10), 18, UiTheme.SURFACE);
        wrapper.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        JLabel title = new JLabel("用户管理");
        title.setForeground(UiTheme.TEXT);
        title.setFont(UiTheme.font(Font.BOLD, 16F));
        wrapper.add(title, BorderLayout.NORTH);
        wrapper.add(new UserManagePanel(m_api), BorderLayout.CENTER);
        return wrapper;
    }

    private void changePassword() {
        JPasswordField oldPassword = new JPasswordField(12);
        JPasswordField newPassword = new JPasswordField(12);
        JPasswordField confirmation = new JPasswordField(12);
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("原密码"));
        form.add(oldPassword);
        form.add(new JLabel("新密码"));
        form.add(newPassword);
        form.add(new JLabel("确认新密码"));
        form.add(confirmation);

        int choice = JOptionPane.showConfirmDialog(this, form, "修改密码", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
            return;
        }
        final String oldValue = new String(oldPassword.getPassword());
        final String newValue = new String(newPassword.getPassword());
        String confirmationValue = new String(confirmation.getPassword());
        if (oldValue.length() == 0 || newValue.length() == 0) {
            warn("请完整填写密码");
            return;
        }
        if (!newValue.equals(confirmationValue)) {
            warn("两次输入的新密码不一致");
            return;
        }
        UiTasks.run(new UiTasks.Task<Void>() {
            @Override
            public Void run() {
                m_api.changePassword(oldValue, newValue);
                return null;
            }
        }, new UiTasks.Success<Void>() {
            @Override
            public void accept(Void ignored) {
                JOptionPane.showMessageDialog(UserCenterPanel.this, "密码已修改，下次请使用新密码登录", "修改成功",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        });
    }

    private void logout() {
        UiTasks.run(new UiTasks.Task<Void>() {
            @Override
            public Void run() {
                m_api.logout();
                return null;
            }
        }, new UiTasks.Success<Void>() {
            @Override
            public void accept(Void ignored) {
                backToLogin();
            }
        }, new UiTasks.Failure() {
            @Override
            public void accept(ApiException error) {
                // 服务器不可达时本地会话也已清空，照常回登录页
                backToLogin();
            }
        });
    }

    /** 关闭连接、销毁主窗口并回到登录页（断线/登出的统一收尾）。 */
    private void backToLogin() {
        VCampusClientApp.stopQuietly();
        Window owner = SwingUtilities.getWindowAncestor(this);
        if (owner != null) {
            owner.dispose();
        }
        new LoginFrame().setVisible(true);
    }

    private void warn(String message) {
        JOptionPane.showMessageDialog(this, message, "无法修改", JOptionPane.WARNING_MESSAGE);
    }
}
