package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.client.view.theme.UiTheme;

import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * 用户管理页脚：批量进度提示 + 分页控件。
 *
 * <p>
 * 从 {@code UserManagePanel} 抽出（原文件破 200 行上限）。分页控件只上报「上一页 / 下一页」意图，
 * 页码状态仍由面板持有——同一份状态只存一处。
 */
class UserPagerPanel extends JPanel {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 进行中提示（批量分片进度显示在这里）。 */
    private final JLabel m_status = new JLabel(" ");

    /** 页码信息。 */
    private final JLabel m_pageLabel = new JLabel(" ");

    /** 上一页动作。 */
    private Runnable m_onPrevious;

    /** 下一页动作。 */
    private Runnable m_onNext;

    /**
     * 创建页脚。
     */
    UserPagerPanel() {
        setLayout(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        setOpaque(false);
        m_status.setForeground(UiTheme.ACCENT_DARK);
        m_status.setFont(UiTheme.font(Font.PLAIN, 12F));
        add(m_status);
        add(createButton("上一页", true));
        m_pageLabel.setForeground(UiTheme.MUTED);
        add(m_pageLabel);
        add(createButton("下一页", false));
    }

    /**
     * 设置上一页动作。
     *
     * @param action 动作
     */
    void setOnPrevious(Runnable action) {
        m_onPrevious = action;
    }

    /**
     * 设置下一页动作。
     *
     * @param action 动作
     */
    void setOnNext(Runnable action) {
        m_onNext = action;
    }

    /**
     * 显示页码信息并清除进度提示。
     *
     * @param pageNumber 当前页码
     * @param totalPages 总页数
     * @param total      总条数
     */
    void showPage(int pageNumber, int totalPages, long total) {
        m_pageLabel.setText("第 " + pageNumber + " / " + Math.max(1, totalPages)
                + " 页    共 " + total + " 条");
        m_status.setText(" ");
    }

    /**
     * 显示进行中提示。
     *
     * @param text 提示文本
     */
    void showStatus(String text) {
        m_status.setText(text);
    }

    private JButton createButton(String text, final boolean previous) {
        JButton button = new JButton(text);
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                Runnable action = previous ? m_onPrevious : m_onNext;
                if (action != null) {
                    action.run();
                }
            }
        });
        return button;
    }
}
