package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.client.view.component.RoundedPanel;
import edu.seu.vcampus.client.view.theme.UiIcons;
import edu.seu.vcampus.client.view.theme.UiTheme;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * 账户弹窗顶部的身份卡：头像 + 姓名 + 身份标签。
 *
 * <p>
 * 从 {@code AccountPopupPanel} 抽出（原文件破 200 行上限）。姓名与身份由调用方解析好后传入， 这里只负责呈现，因此文本可以单独断言。
 */
class AccountIdentityPanel extends JPanel {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 姓名标签。 */
    private final JLabel m_displayName = new JLabel();

    /** 身份标签。 */
    private final JLabel m_role = new JLabel();

    /**
     * 构造身份卡。
     *
     * @param displayName 姓名（调用方已保证非空，缺姓名时应传登录名）
     * @param roleName    身份显示名（如「学生」）
     */
    AccountIdentityPanel(String displayName, String roleName) {
        setLayout(new BorderLayout(12, 0));
        setOpaque(false);
        add(createAvatar(), BorderLayout.WEST);
        add(createTexts(displayName, roleName), BorderLayout.CENTER);
    }

    /** 头像圆牌。 */
    private JPanel createAvatar() {
        RoundedPanel avatar = new RoundedPanel(new BorderLayout(), 24, UiTheme.NAVY);
        avatar.setPreferredSize(new Dimension(44, 44));
        avatar.add(new JLabel(UiIcons.load("user-light", 22)), BorderLayout.CENTER);
        return avatar;
    }

    /** 姓名 + 身份标签。 */
    private JPanel createTexts(String displayName, String roleName) {
        m_displayName.setText(displayName);
        m_displayName.setForeground(UiTheme.TEXT);
        m_displayName.setFont(UiTheme.font(Font.BOLD, 17F));
        m_role.setText(roleName);
        m_role.setForeground(UiTheme.SURFACE);
        m_role.setFont(UiTheme.font(Font.BOLD, 11F));
        m_role.setOpaque(true);
        m_role.setBackground(UiTheme.NAVY_LIGHT);
        m_role.setBorder(BorderFactory.createEmptyBorder(3, 9, 3, 9));
        JPanel roleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        roleRow.setOpaque(false);
        roleRow.add(m_role);
        JPanel texts = new JPanel(new BorderLayout(0, 6));
        texts.setOpaque(false);
        texts.add(m_displayName, BorderLayout.NORTH);
        texts.add(roleRow, BorderLayout.CENTER);
        return texts;
    }

    /** @return 姓名文本 */
    String displayNameText() {
        return m_displayName.getText();
    }

    /** @return 身份文本 */
    String roleText() {
        return m_role.getText();
    }
}
