package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.client.student.StudentService;
import edu.seu.vcampus.client.view.UiTasks;
import edu.seu.vcampus.client.view.theme.UiTheme;
import edu.seu.vcampus.common.student.entity.CampusStatus;
import edu.seu.vcampus.common.student.entity.StudentProfile;
import edu.seu.vcampus.common.user.entity.Capability;
import edu.seu.vcampus.common.user.entity.Permissions;
import edu.seu.vcampus.common.user.entity.Role;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/** 学籍管理页的操作栏：新生登记、修改状态和注销。 */
final class StudentActionBar extends JPanel {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 学籍 API。 */
    private final StudentService m_api;

    /** 列表页；用来取选中行与在操作成功后重查。 */
    private final StudentManagePanel m_panel;

    /** 当前身份（决定能否在改状态后连带注销）。 */
    private final Role m_role;

    /** 「改成」的状态下拉。 */
    private final JComboBox<String> m_new_status = new JComboBox<String>();

    /**
     * 构造操作栏，按能力添加按钮。
     *
     * @param api 学籍 API
     * @param role 当前身份；null 视为无任何能力
     * @param panel 列表页
     */
    StudentActionBar(StudentService api, Role role, StudentManagePanel panel) {
        this.m_api = api;
        this.m_panel = panel;
        this.m_role = role;
        setLayout(new FlowLayout(FlowLayout.LEFT, 8, 4));
        setOpaque(false);
        if (Permissions.can(role, Capability.STUDENT_REGISTER)) {
            add(button("新生登记", new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    registerNew();
                }
            }));
        }
        if (Permissions.can(role, Capability.STUDENT_CHANGE_STATUS)) {
            JLabel label = new JLabel("改为");
            label.setForeground(UiTheme.MUTED);
            add(label);
            m_new_status.setModel(new DefaultComboBoxModel<String>(statusNames()));
            add(m_new_status);
            add(button("修改状态", new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    changeStatus();
                }
            }));
        }
        if (Permissions.can(role, Capability.STUDENT_DELETE)) {
            add(button("注销", new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    deleteSelected();
                }
            }));
        }
    }

    private void registerNew() {
        new StudentRegisterDialog(m_api, m_panel).setVisible(true);
    }

    private void changeStatus() {
        final StudentProfile target = requireSelected();
        if (target == null) {
            return;
        }
        final CampusStatus status =
                CampusStatus.fromDisplayName(String.valueOf(m_new_status.getSelectedItem()));
        if (status == null) {
            warn("请先选择要改成的状态");
            return;
        }
        final long profileId = idOf(target);
        if (profileId < 0L) {
            return;
        }
        if (!StudentConfirmDialogs.confirmStatusChange(this, target, status)) {
            return;
        }
        UiTasks.run(new UiTasks.Task<Void>() {
            @Override
            public Void run() {
                m_api.changeStatus(profileId, status);
                return null;
            }
        }, new UiTasks.Success<Void>() {
            @Override
            public void accept(Void ignored) {
                offerCascadeDelete(target, profileId, status);
            }
        });
    }

    /**
     * 状态改成「已离校」类的值之后，问一句要不要把学籍也注销掉。
     *
     * <p>
     * 离校 / 毕业 / 退休三种状态意味着这个人不再回校，留着学籍会在列表里多出一批永远不会再变的
     * 行；但到底要不要清理属于教务的判断（毕业生的档案常常还要留），所以只问不自动做。
     *
     * <p>
     * 没有 {@code STUDENT_DELETE} 能力的人（教师）不会看到这个问题：问了他也做不成，
     * 服务端会回 403。无论用户选什么，最后都要重查一次列表——状态已经改了，界面得跟上。
     *
     * @param target 目标学籍
     * @param profileId 目标学籍主键
     * @param status 刚改成的状态
     */
    private void offerCascadeDelete(final StudentProfile target, final long profileId,
            CampusStatus status) {
        if (!status.isDeparted() || !Permissions.can(m_role, Capability.STUDENT_DELETE)
                || !StudentConfirmDialogs.confirmCascadeDelete(this, target, status)) {
            m_panel.refresh();
            return;
        }
        UiTasks.run(new UiTasks.Task<Void>() {
            @Override
            public Void run() {
                m_api.deleteStudent(profileId);
                return null;
            }
        }, new UiTasks.Success<Void>() {
            @Override
            public void accept(Void ignored) {
                m_panel.refresh();
            }
        });
    }

    private void deleteSelected() {
        final StudentProfile target = requireSelected();
        if (target == null) {
            return;
        }
        final long profileId = idOf(target);
        if (profileId < 0L) {
            return;
        }
        if (!StudentConfirmDialogs.confirmDelete(this, target)) {
            return;
        }
        UiTasks.run(new UiTasks.Task<Void>() {
            @Override
            public Void run() {
                m_api.deleteStudent(profileId);
                return null;
            }
        }, new UiTasks.Success<Void>() {
            @Override
            public void accept(Void ignored) {
                m_panel.refresh();
            }
        });
    }

    private StudentProfile requireSelected() {
        StudentProfile target = m_panel.selected();
        if (target == null) {
            warn("请先在表格里选中一行学籍");
        }
        return target;
    }

    private static String nameOf(StudentProfile profile) {
        String name = profile.getRealName();
        return name == null || name.trim().length() == 0 ? "未登记姓名" : name;
    }

    private static String[] statusNames() {
        CampusStatus[] all = CampusStatus.values();
        String[] names = new String[all.length];
        int index = 0;
        while (index < all.length) {
            names[index] = all[index].getDisplayName();
            index = index + 1;
        }
        return names;
    }

    private static JButton button(String text, ActionListener listener) {
        JButton button = new JButton(text);
        button.addActionListener(listener);
        return button;
    }

    private void warn(String message) {
        JOptionPane.showMessageDialog(this, message, "提示", JOptionPane.WARNING_MESSAGE);
    }
}
