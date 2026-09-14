package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.client.view.theme.UiFactory;
import edu.seu.vcampus.client.view.theme.UiTheme;
import edu.seu.vcampus.common.user.entity.Role;
import edu.seu.vcampus.common.user.entity.SessionEntry;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * 右上角账户弹窗的内容面板：**只做展示与回调**，不含任何网络调用。
 *
 * <p>
 * 原来「用户中心」页承担的资料展示、修改密码、退出登录三件事都收在这里：资料只读（身份以服务端签发的
 * 会话为准），两个动作通过构造时传入的 {@link Runnable} 交回给宿主窗口，因此本面板可独立单测。
 *
 * <p>
 * 姓名缺失时回退显示登录名——管理员没有姓名概念，天然走这条路径，界面不会出现空白。
 */
public class AccountPopupPanel extends JPanel {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 姓名（或回退后的登录名）。 */
    private final JLabel m_display_name = new JLabel();

    /** 登录名。 */
    private final JLabel m_user_name = new JLabel();

    /** 身份。 */
    private final JLabel m_role = new JLabel();

    /** 账户标识。 */
    private final JLabel m_uuid = new JLabel();

    /** 修改密码按钮。 */
    private final JButton m_password = UiFactory.primaryButton("修改密码", "lock");

    /** 退出登录按钮。 */
    private final JButton m_logout = new JButton("退出登录");

    /**
     * 构造账户弹窗内容。
     *
     * @param session          当前会话；null 表示未登录，全部字段显示占位
     * @param onChangePassword 点击「修改密码」的回调；null 表示该按钮不响应
     * @param onLogout         点击「退出登录」的回调；null 表示该按钮不响应
     */
    public AccountPopupPanel(SessionEntry session, final Runnable onChangePassword,
            final Runnable onLogout) {
        setLayout(new BorderLayout(0, 10));
        setBackground(UiTheme.SURFACE);
        setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        setPreferredSize(new Dimension(268, 200));
        add(createIdentity(session), BorderLayout.CENTER);
        add(createActions(onChangePassword, onLogout), BorderLayout.SOUTH);
    }

    /** 创建身份区：姓名（主） + 登录名 / 身份 / 标识（次）。 */
    private JPanel createIdentity(SessionEntry session) {
        String userName = session == null ? "-" : textOf(session.getUsername());
        String displayName = session == null ? "-" : textOf(session.getDisplayName());
        if ("-".equals(displayName)) {
            displayName = userName;// 姓名未采集（如管理员）时回退登录名
        }
        Role role = session == null ? null : Role.fromDisplayName(session.getRole());
        m_display_name.setText(displayName);
        m_display_name.setForeground(UiTheme.TEXT);
        m_display_name.setFont(UiTheme.font(Font.BOLD, 17F));
        m_user_name.setText("登录名：" + userName);
        m_role.setText("身份：" + (role == null ? "-" : role.getDisplayName()));
        m_uuid.setText("标识：" + (session == null ? "-" : textOf(session.getUuid())));
        style(m_user_name);
        style(m_role);
        style(m_uuid);

        JPanel rows = new JPanel(new GridLayout(3, 1, 0, 4));
        rows.setOpaque(false);
        rows.add(m_user_name);
        rows.add(m_role);
        rows.add(m_uuid);
        JPanel identity = new JPanel(new BorderLayout(0, 8));
        identity.setOpaque(false);
        identity.add(m_display_name, BorderLayout.NORTH);
        identity.add(rows, BorderLayout.CENTER);
        return identity;
    }

    /** 创建动作区：修改密码 / 退出登录。 */
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
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        actions.add(m_password);
        actions.add(m_logout);
        return actions;
    }

    /** 统一次字段样式。 */
    private void style(JLabel label) {
        label.setForeground(UiTheme.MUTED);
        label.setFont(UiTheme.font(Font.PLAIN, 12F));
    }

    /** 空值统一显示为占位符。 */
    private String textOf(String value) {
        return value == null || value.trim().length() == 0 ? "-" : value;
    }

    /** @return 姓名文本（缺姓名时为登录名） */
    public String getDisplayNameText() {
        return m_display_name.getText();
    }

    /** @return 登录名行文本 */
    public String getUserNameText() {
        return m_user_name.getText();
    }

    /** @return 身份行文本 */
    public String getRoleText() {
        return m_role.getText();
    }

    /** @return 标识行文本 */
    public String getUuidText() {
        return m_uuid.getText();
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
