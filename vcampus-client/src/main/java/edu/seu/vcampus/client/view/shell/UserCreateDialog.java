package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.client.user.UserAdminService;
import edu.seu.vcampus.client.view.UiTasks;
import edu.seu.vcampus.common.user.entity.Role;

import java.awt.Component;
import java.awt.GridLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

/**
 * 新建用户对话框：收集登录名 / 姓名 / 身份 / 初始密码 → 本地校验 → 后台调用注册命令。
 *
 * <p>
 * 从 {@code UserManageActions} 抽出（原文件破 200 行上限）。校验是纯函数 {@link #validate}，可单测；
 * 注册成功后服务器会同步建立该账号在各模块的档案，因此这里不需要额外动作。
 */
final class UserCreateDialog {

    /**
     * 私有构造器，禁止实例化工具类。
     */
    private UserCreateDialog() {
    }

    /**
     * 弹出新建用户对话框并提交。
     *
     * @param parent 父组件
     * @param api    用户管理 API
     * @param onDone 提交完成后的回调（无论成败都刷新列表）；可为 null
     */
    static void show(final Component parent, final UserAdminService api, final Runnable onDone) {
        JTextField userName = new JTextField(14);
        JTextField displayName = new JTextField(14);
        JComboBox<String> role = new JComboBox<String>(new String[] { "学生", "教师", "管理员" });
        JPasswordField password = new JPasswordField(14);
        JPasswordField confirmation = new JPasswordField(14);
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("登录名"));
        form.add(userName);
        form.add(new JLabel("姓名"));
        form.add(displayName);
        form.add(new JLabel("身份"));
        form.add(role);
        form.add(new JLabel("初始密码"));
        form.add(password);
        form.add(new JLabel("确认密码"));
        form.add(confirmation);
        if (JOptionPane.showConfirmDialog(parent, form, "新建用户", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
            return;
        }
        final String name = userName.getText().trim();
        final String shown = displayName.getText().trim();
        final String secret = new String(password.getPassword());
        String error = validate(name, secret, new String(confirmation.getPassword()));
        if (error != null) {
            JOptionPane.showMessageDialog(parent, error, "无法继续", JOptionPane.WARNING_MESSAGE);
            return;
        }
        final String finalShown = shown.length() == 0 ? name : shown;
        final Role selected = Role.fromDisplayName(String.valueOf(role.getSelectedItem()));
        UiTasks.run(new UiTasks.Task<Void>() {
            @Override
            public Void run() {
                api.register(name, finalShown, selected, secret);
                return null;
            }
        }, new UiTasks.Success<Void>() {
            @Override
            public void accept(Void ignored) {
                notifyDone(onDone);
            }
        }, new UiTasks.Failure() {
            @Override
            public void accept(ApiException error) {
                notifyDone(onDone);
            }
        });
    }

    /**
     * 校验新建用户的输入（纯函数，便于单测）。
     *
     * @param userName     登录名
     * @param password     初始密码
     * @param confirmation 确认密码
     * @return 错误文案；通过返回 null
     */
    static String validate(String userName, String password, String confirmation) {
        if (userName == null || userName.length() == 0) {
            return "请填写登录名";
        }
        if (password == null || password.length() == 0) {
            return "请填写初始密码";
        }
        if (!password.equals(confirmation)) {
            return "两次输入的密码不一致";
        }
        return null;
    }

    private static void notifyDone(Runnable onDone) {
        if (onDone != null) {
            onDone.run();
        }
    }
}
