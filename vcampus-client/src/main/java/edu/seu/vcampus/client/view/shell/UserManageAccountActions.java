package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.client.view.UiTasks;
import edu.seu.vcampus.common.user.dto.UserUpdateRequest;
import edu.seu.vcampus.common.user.entity.Role;
import edu.seu.vcampus.common.user.entity.User;

import java.awt.GridLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

/** Handles create, update, enable and unregister actions for one user. */
final class UserManageAccountActions {
    private final UserManagePanel panel;

    UserManageAccountActions(UserManagePanel panel) {
        this.panel = panel;
    }

    void toggleSelected() {
        final User user = selected();
        if (user == null) {
            return;
        }
        final boolean target = !user.isEnabled();
        UiTasks.run(new UiTasks.Task<Void>() {
            @Override
            public Void run() {
                panel.api().toggleUserEnabled(user.getUserName(), target);
                return null;
            }
        }, refreshAfterSuccess());
    }

    void renameSelected() {
        final User user = selected();
        if (user == null) {
            return;
        }
        final String name = JOptionPane.showInputDialog(panel, "新的姓名",
                user.getDisplayName());
        if (name == null || name.trim().length() == 0) {
            return;
        }
        UiTasks.run(new UiTasks.Task<Void>() {
            @Override
            public Void run() {
                panel.api().updateUser(new UserUpdateRequest(user.getUserName(), name));
                return null;
            }
        }, refreshAfterSuccess());
    }

    void unregisterSelected() {
        final User user = selected();
        if (user == null) {
            return;
        }
        int choice = JOptionPane.showConfirmDialog(panel,
                "确定注销账号 " + user.getUserName()
                        + " ？该账号的各模块档案会一并撤销。",
                "确认注销", JOptionPane.OK_CANCEL_OPTION);
        if (choice != JOptionPane.OK_OPTION) {
            return;
        }
        UiTasks.run(new UiTasks.Task<Void>() {
            @Override
            public Void run() {
                panel.api().unregister(user.getUserName());
                return null;
            }
        }, refreshAfterSuccess());
    }

    void createUser() {
        final JTextField login = new JTextField(12);
        final JTextField display = new JTextField(12);
        final JPasswordField password = new JPasswordField(12);
        final JComboBox<String> role = new JComboBox<String>(new String[] {
                "学生", "教师", "管理员"});
        JPanel form = createForm(login, display, role, password);
        int choice = JOptionPane.showConfirmDialog(panel, form, "新建用户",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
            return;
        }
        submitCreate(login, display, role, password);
    }

    private JPanel createForm(JTextField login, JTextField display,
            JComboBox<String> role, JPasswordField password) {
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("登录名"));
        form.add(login);
        form.add(new JLabel("姓名"));
        form.add(display);
        form.add(new JLabel("角色"));
        form.add(role);
        form.add(new JLabel("初始密码"));
        form.add(password);
        return form;
    }

    private void submitCreate(JTextField login, JTextField display,
            JComboBox<String> role, JPasswordField password) {
        final String userName = login.getText().trim();
        final String displayName = display.getText().trim();
        final String plain = new String(password.getPassword());
        final Role selectedRole = Role.fromDisplayName(
                String.valueOf(role.getSelectedItem()));
        if (userName.length() == 0 || plain.length() == 0) {
            panel.warn("登录名与密码不能为空");
            return;
        }
        UiTasks.run(new UiTasks.Task<Void>() {
            @Override
            public Void run() {
                panel.api().register(userName, displayName, selectedRole, plain);
                return null;
            }
        }, new UiTasks.Success<Void>() {
            @Override
            public void accept(Void ignored) {
                JOptionPane.showMessageDialog(panel,
                        "账号 " + userName + " 已创建，档案已同步建立",
                        "新建成功", JOptionPane.INFORMATION_MESSAGE);
                panel.refresh();
            }
        }, new UiTasks.Failure() {
            @Override
            public void accept(ApiException error) {
                JOptionPane.showMessageDialog(panel, error.getMessage(), "操作失败",
                        JOptionPane.WARNING_MESSAGE);
            }
        });
    }

    private User selected() {
        int viewRow = panel.table().getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(panel, "请先在表格中选择一个用户", "提示",
                    JOptionPane.INFORMATION_MESSAGE);
            return null;
        }
        int row = panel.table().convertRowIndexToModel(viewRow);
        if (row >= panel.rows().size()) {
            return null;
        }
        return panel.rows().get(row);
    }

    private UiTasks.Success<Void> refreshAfterSuccess() {
        return new UiTasks.Success<Void>() {
            @Override
            public void accept(Void ignored) {
                panel.refresh();
            }
        };
    }
}
