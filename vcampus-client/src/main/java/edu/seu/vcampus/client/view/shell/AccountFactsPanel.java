package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.client.view.theme.UiTheme;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * 账户弹窗中部的键值区：登录名 / 账户标识两行，上方带一条分隔线。
 *
 * <p>
 * 从 {@code AccountPopupPanel} 抽出（原文件破 200 行上限）。两列布局（灰标签 + 深色值）在这里统一， 弹窗本体只负责把值传进来。
 */
class AccountFactsPanel extends JPanel {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 登录名值标签。 */
    private final JLabel m_userName = new JLabel();

    /** 账户标识值标签。 */
    private final JLabel m_uuid = new JLabel();

    /**
     * 构造键值区。
     *
     * @param userName 登录名
     * @param uuid     账户标识
     */
    AccountFactsPanel(String userName, String uuid) {
        setLayout(new BorderLayout(0, 12));
        setOpaque(false);
        add(createDivider(), BorderLayout.NORTH);
        add(createRows(userName, uuid), BorderLayout.CENTER);
    }

    /** 两行键值。 */
    private JPanel createRows(String userName, String uuid) {
        JPanel rows = new JPanel(new GridLayout(2, 2, 10, 8));
        rows.setOpaque(false);
        rows.add(keyLabel("登录名"));
        rows.add(valueLabel(m_userName, userName));
        rows.add(keyLabel("账户标识"));
        rows.add(valueLabel(m_uuid, uuid));
        return rows;
    }

    /** 1 像素分隔线。 */
    private JPanel createDivider() {
        JPanel line = new JPanel();
        line.setBackground(UiTheme.BORDER);
        line.setPreferredSize(new Dimension(0, 1));
        return line;
    }

    private JLabel keyLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(UiTheme.MUTED);
        label.setFont(UiTheme.font(Font.PLAIN, 12F));
        return label;
    }

    private JLabel valueLabel(JLabel target, String text) {
        target.setText(text);
        target.setForeground(UiTheme.TEXT);
        target.setFont(UiTheme.font(Font.PLAIN, 13F));
        return target;
    }

    /** @return 登录名 */
    String userNameText() {
        return m_userName.getText();
    }

    /** @return 账户标识 */
    String uuidText() {
        return m_uuid.getText();
    }

    /**
     * 设置账户标识的悬浮提示（完整值，便于标识过长时的查看）。
     *
     * @param text 提示文本
     */
    void uuidTooltip(String text) {
        m_uuid.setToolTipText(text);
    }
}
