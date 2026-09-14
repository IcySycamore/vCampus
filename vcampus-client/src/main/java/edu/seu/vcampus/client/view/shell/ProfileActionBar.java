package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.client.view.theme.UiFactory;
import edu.seu.vcampus.client.view.theme.UiTheme;
import edu.seu.vcampus.common.user.entity.Capability;
import edu.seu.vcampus.common.user.entity.Permissions;
import edu.seu.vcampus.common.user.entity.Role;

import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * 档案页底部操作条：查看态是「刷新 / 申请修改」，修改态换成「提交申请 / 取消」，
 * 右侧那条就地提示用来报校验原因和提交结果——全程不弹对话框。
 *
 * <p>
 * 按钮按 {@link Permissions} 决定出不出现；动作一律转发给宿主页面，这里不碰业务。
 */
final class ProfileActionBar extends JPanel {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 查看态：进入修改。 */
    private final JButton m_apply;

    /** 修改态：提交申请。 */
    private final JButton m_submit;

    /** 修改态：放弃这次修改。 */
    private final JButton m_cancel;

    /** 就地提示。 */
    private final JLabel m_status = new JLabel(" ");

    /** 本角色是否有权申请修改；决定切回查看态时「申请修改」要不要露脸。 */
    private final boolean m_canApply;

    /**
     * 创建操作条。
     *
     * @param page 宿主档案页
     * @param role 当前登录角色；null 视为无权限
     */
    ProfileActionBar(final ProfileDetailPanel page, Role role) {
        setLayout(new FlowLayout(FlowLayout.LEFT, 10, 4));
        setOpaque(false);
        JButton reload = new JButton("刷新");
        reload.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                page.reload();
            }
        });
        add(reload);
        m_apply = UiFactory.primaryButton("申请修改", "edit");
        m_apply.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                page.startEdit();
            }
        });
        m_canApply = Permissions.can(role, Capability.STUDENT_MODIFY_APPLY);
        m_apply.setVisible(m_canApply);
        add(m_apply);
        m_submit = UiFactory.primaryButton("提交申请", "edit");
        m_submit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                page.submit();
            }
        });
        m_submit.setVisible(false);
        add(m_submit);
        m_cancel = new JButton("取消");
        m_cancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                page.cancelEdit();
            }
        });
        m_cancel.setVisible(false);
        add(m_cancel);
        m_status.setForeground(UiTheme.MUTED);
        m_status.setFont(UiTheme.font(Font.PLAIN, 13F));
        add(m_status);
    }

    /**
     * 切换查看态 / 修改态的按钮组合。
     *
     * <p>
     * 「申请修改」的可见性必须由是否有权限（而不是当前可见状态）推出来：否则切回查看态时它会
     * 因为「当时不可见」而被永久藏掉。
     *
     * @param editing 是否处于修改态
     */
    void setEditing(boolean editing) {
        m_apply.setVisible(!editing && m_canApply);
        m_submit.setVisible(editing);
        m_cancel.setVisible(editing);
    }

    /**
     * 写就地提示。
     *
     * @param text 提示文本；空串表示清空
     */
    void setStatus(String text) {
        m_status.setText(text == null || text.length() == 0 ? " " : text);
    }
}
