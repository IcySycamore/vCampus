package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.client.student.StudentService;
import edu.seu.vcampus.client.user.UserAdminService;
import edu.seu.vcampus.client.view.component.ModernTabbedPaneUI;
import edu.seu.vcampus.common.user.entity.Capability;
import edu.seu.vcampus.common.user.entity.Permissions;
import edu.seu.vcampus.common.user.entity.Role;

import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

/**
 * 管理控制台（仅管理员）：把「用户管理」与「学籍管理」收进同一张卡片的两个页签。
 *
 * <p>
 * 组长反馈「管理员既有用户管理又有学籍管理，功能重复」——原先两者分别挂在用户中心与个人信息页，
 * 管理员因此有两个互不相干的管理入口。现在管理员的学籍管理统一收在这里，
 * {@link ProfilePanel} 只对没有用户管理权限的角色（教师）保留学籍页签。
 *
 * <p>
 * 本类只负责按能力拼装页签，面板实现仍各只有一份（{@link UserManagePage}、
 * {@link StudentManagePanel}），与 ADR-0009 D6「按 {@link Permissions} 逐项决定页签出不出现」
 * 是同一套做法。客户端判定只管显示，服务端 403 才是最终防线。
 *
 * <p>
 * 本类原先挂在「用户中心」页上；该页下线（用户管理升格为独立页）后改挂在用户管理页上，
 * 页签组合与语义不变。
 */
class AdminConsolePanel extends JPanel {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /**
     * 构造管理控制台。
     *
     * @param userAdminApi 用户管理 API
     * @param studentApi 学籍 API；未装配时可为 null（此时不挂学籍页签）
     * @param role 当前身份，决定学籍页签出不出现
     */
    AdminConsolePanel(UserAdminService userAdminApi, StudentService studentApi, Role role) {
        setLayout(new BorderLayout());
        setOpaque(false);
        JTabbedPane tabs = new JTabbedPane();
        tabs.setUI(new ModernTabbedPaneUI());
        tabs.addTab("用户管理", new UserManagePage(userAdminApi));
        if (studentApi != null && Permissions.can(role, Capability.STUDENT_VIEW_ALL)) {
            tabs.addTab("学籍管理", new StudentManagePanel(studentApi, role));
        }
        add(tabs, BorderLayout.CENTER);
    }
}
