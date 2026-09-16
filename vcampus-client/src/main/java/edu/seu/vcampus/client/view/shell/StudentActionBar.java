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
        final long profileId = target.getId() == null ? -1L : target.getId().longValue();
        if (profileId < 0L) {
            warn("该行没有主键（档案尚未落库）");
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
                m_panel.refresh();
            }
        });
    }

    private void deleteSelected() {
        final StudentProfile target = requireSelected();
        if (target == null) {
            return;
        }
        final long profileId = target.getId() == null ? -1L : target.getId().longValue();
        if (profileId < 0L) {
            warn("该行没有主键（档案尚未落库）");
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this,
                "确定注销学籍 #" + profileId + "（" + nameOf(target) + "）？\n"
                        + "注销是软删除，档案保留但不再出现在列表里。",
                "确认注销", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
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
