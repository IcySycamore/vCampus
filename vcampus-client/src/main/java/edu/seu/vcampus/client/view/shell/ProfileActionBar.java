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
 * 档案页底部操作条：查看态是「刷新 / 申请修改」，修改态换成「提交申请 / 取消」， 右侧那条就地提示用来报校验原因和提交结果——全程不弹对话框。
 *
 * <p>
 * 按钮按 {@link Permissions} 决定出不出现；动作一律转发给宿主页面，这里不碰业务。
 */
final class ProfileActionBar extends JPanel {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 查看态：进入修改。 */
    private final JButton m_apply;

    /** 查看态：进入自助填写（仅学籍尚未填写时露出）。 */
    private final JButton m_enroll;

    /** 修改态：提交申请。 */
    private final JButton m_submit;

    /** 修改态：放弃这次修改。 */
    private final JButton m_cancel;

    /** 就地提示。 */
    private final JLabel m_status = new JLabel(" ");

    /** 本角色是否有权申请修改；决定切回查看态时「申请修改」要不要露脸。 */
    private final boolean m_canApply;

    /** 学籍是否尚未填写（由宿主页面按档案内容设置）；决定「填写学籍信息」要不要露脸。 */
    private boolean m_canEnroll;

    /** 当前角色是否学生本人：学籍自助申请与填写只属于学生本人页。 */
    private final boolean m_isStudent;

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
        m_isStudent = role == Role.STUDENT;
        // 管理员在能力表里是「全部能力」，但学籍自助修改是学生本人的事，
        // 管理员看自己的档案页时不该出现这个入口。
        m_canApply = m_isStudent && Permissions.can(role, Capability.STUDENT_MODIFY_APPLY);
        m_apply.setVisible(m_canApply);
        add(m_apply);
        // 「填写」与「申请修改」是两件不同的事，各占一个按钮（前者立即生效，后者要等审核）。
        // 能否填写要看档案是不是还空着，而这个答案要等 201 回来才知道，所以先藏起来，
        // 由页面装载完成后调 setEnrollAvailable。
        m_enroll = UiFactory.primaryButton("填写学籍信息", "edit");
        m_enroll.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                page.startEnroll();
            }
        });
        m_enroll.setVisible(false);
        add(m_enroll);
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
     * 两个「进入」按钮的可见性都必须由权限 / 档案状态（而不是当前可见状态）推出来：否则切回查看态时 它们会因为「当时不可见」而被永久藏掉。
     *
     * @param editing   是否处于填写 / 修改态
     * @param enrolling true 表示这次是自助填写，提交按钮文案随之变化
     */
    void setEditing(boolean editing, boolean enrolling) {
        m_apply.setVisible(!editing && m_canApply);
        m_enroll.setVisible(!editing && m_canEnroll);
        m_submit.setVisible(editing);
        m_submit.setText(enrolling ? "提交填写" : "提交申请");
        m_cancel.setVisible(editing);
    }

    /**
     * 学籍还没填写过时把「填写学籍信息」露出来。
     *
     * <p>
     * 由宿主页面在档案装载完成后（以及装载失败时）调用：只有档案为空才该出现这个入口， 而档案内容只有 201 回来才知道。
     *
     * @param available 是否可自助填写
     */
    void setEnrollAvailable(boolean available) {
        // 教师/管理员看自己的档案页时，档案当然是空的，但不能因此给出「填写学籍信息」
        m_canEnroll = m_isStudent && available;
        m_enroll.setVisible(available && !m_submit.isVisible());
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
