package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.client.user.UserAdminService;
import edu.seu.vcampus.client.view.UiTasks;
import edu.seu.vcampus.common.user.dto.UserUpdateRequest;
import edu.seu.vcampus.common.user.entity.User;

import java.awt.Component;
import java.awt.GridLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * 用户管理「单条动作」：启用/禁用、编辑姓名、新建用户、注销。
 *
 * <p>
 * 与界面控件解耦（只依赖 {@link UserService} 和一个用于挂对话框的父组件），因此表单校验这段纯逻辑
 * 可以单测：{@link #validateNewUser(String, String, String, String)}。
 */
public final class UserManageActions {

    /** 管理轨 API。 */
    private final UserAdminService m_api;

    /** 对话框父组件。 */
    private final Component m_parent;

    /** 动作完成后的刷新回调；可为 null。 */
    private final Runnable m_onChanged;

    /**
     * 构造单条动作执行器。
     *
     * @param api       用户管理 API
     * @param parent    对话框父组件
     * @param onChanged 动作完成后的刷新回调；可为 null
     */
    public UserManageActions(UserAdminService api, Component parent, Runnable onChanged) {
        this.m_api = api;
        this.m_parent = parent;
        this.m_onChanged = onChanged;
    }

    /**
     * 切换启用状态（命令 108）。
     *
     * @param user 目标用户；null 表示未选中
     */
    public void toggleEnabled(final User user) {
        if (user == null) {
            warn("请先在表格中选中一个账号");
            return;
        }
        final boolean target = !user.isEnabled();
        final String userName = user.getUserName();
        call(new Runnable() {
            @Override
            public void run() {
                m_api.toggleUserEnabled(userName, target);
            }
        });
    }

    /**
     * 编辑姓名（命令 107）。姓名对应 {@code tblUser.uName}，与各模块档案里的展示名同源。
     *
     * @param user 目标用户；null 表示未选中
     */
    public void rename(final User user) {
        if (user == null) {
            warn("请先在表格中选中一个账号");
            return;
        }
        JTextField name = new JTextField(user.getDisplayName() == null ? "" : user.getDisplayName(),
                14);
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("登录名"));
        form.add(new JLabel(user.getUserName()));
        form.add(new JLabel("姓名"));
        form.add(name);
        if (JOptionPane.showConfirmDialog(m_parent, form, "编辑姓名", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
            return;
        }
        final String value = name.getText().trim();
        if (value.length() == 0) {
            warn("姓名不能为空");
            return;
        }
        final String userName = user.getUserName();
        call(new Runnable() {
            @Override
            public void run() {
                m_api.updateUser(new UserUpdateRequest(userName, value));
            }
        });
    }

    /** 新建单个账号（命令 102）：与批量导入共用同一条注册链路，因此也会同步建立各模块档案。 */
    public void create() {
        UserCreateDialog.show(m_parent, m_api, m_onChanged);
    }

    /**
     * 注销账号（命令 104）；服务器会同时撤销该账号在各模块的档案。
     *
     * @param user 目标用户；null 表示未选中
     */
    public void unregister(final User user) {
        if (user == null) {
            warn("请先在表格中选中一个账号");
            return;
        }
        if (JOptionPane.showConfirmDialog(m_parent,
                "确认注销账号「" + user.getUserName() + "」？该账号在各模块的档案会一并撤销。", "注销账号",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
            return;
        }
        final String userName = user.getUserName();
        call(new Runnable() {
            @Override
            public void run() {
                m_api.unregister(userName);
            }
        });
    }

    /**
     * 校验新建用户的输入（纯函数，便于单测）。
     *
     * @param userName     登录名
     * @param displayName  姓名
     * @param password     初始密码
     * @param confirmation 确认密码
     * @return 错误文案；通过返回 null
     */
    public static String validateNewUser(String userName, String displayName, String password,
            String confirmation) {
        return UserCreateDialog.validate(userName, password, confirmation);
    }

    /** 后台执行动作；无论成败都刷新列表（失败文案由 UiTasks 统一弹出）。 */
    private void call(final Runnable action) {
        UiTasks.run(new UiTasks.Task<Void>() {
            @Override
            public Void run() {
                action.run();
                return null;
            }
        }, new UiTasks.Success<Void>() {
            @Override
            public void accept(Void ignored) {
                refresh();
            }
        }, new UiTasks.Failure() {
            @Override
            public void accept(ApiException error) {
                refresh();
            }
        });
    }

    /** 通知宿主刷新列表。 */
    private void refresh() {
        if (m_onChanged != null) {
            m_onChanged.run();
        }
    }

    private void warn(String message) {
        JOptionPane.showMessageDialog(m_parent, message, "无法继续", JOptionPane.WARNING_MESSAGE);
    }
}
