package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.client.user.UserImportFile;
import edu.seu.vcampus.client.user.UserService;
import edu.seu.vcampus.client.view.UiTasks;
import edu.seu.vcampus.client.view.theme.UiFactory;
import edu.seu.vcampus.client.view.theme.UiTheme;
import edu.seu.vcampus.common.message.PageResponse;
import edu.seu.vcampus.common.user.dto.BatchResult;
import edu.seu.vcampus.common.user.dto.RegisterRequest;
import edu.seu.vcampus.common.user.dto.UserQuery;
import edu.seu.vcampus.common.user.dto.UserUpdateRequest;
import edu.seu.vcampus.common.user.entity.Role;
import edu.seu.vcampus.common.user.entity.User;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

/**
 * 用户管理面板（管理轨，需 {@code USER_MANAGE}）：分页查询 + 启停 + 编辑姓名 + 新建 + 注销。
 *
 * <p>
 * 页面只做「控件 → 调 API → 回填」，判断逻辑都在服务端与 {@code UiTasks}/{@code ApiErrors} 里 （见 ADR-0009 D11）。所有网络调用都经
 * {@link UiTasks}，因此不会卡住事件线程。
 */
public class UserManagePanel extends JPanel {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 角色下拉项。 */
    private static final String ALL_ROLES = "全部角色";

    /** 状态下拉项。 */
    private static final String ANY_STATE = "全部状态";

    /** 用户管理 API。 */
    private final UserService m_api;

    /** 关键词输入框。 */
    private final JTextField m_keyword = new JTextField(12);

    /** 角色过滤。 */
    private final JComboBox<String> m_role = new JComboBox<String>();

    /** 状态过滤。 */
    private final JComboBox<String> m_enabled = new JComboBox<String>();

    /** 表格模型。 */
    private final DefaultTableModel m_model = new DefaultTableModel(
            new Object[] { "登录名", "姓名", "角色", "状态" }, 0) {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    /** 用户表格。 */
    private final JTable m_table = new JTable(m_model);

    /** 分页信息。 */
    private final JLabel m_page_label = new JLabel(" ");

    /** 当前页数据（与表格行一一对应）。 */
    private final List<User> m_rows = new ArrayList<User>();

    /** 当前页码。 */
    private int m_page_number = 1;

    /** 每页记录数。 */
    private int m_page_size = 20;

    /**
     * 构造用户管理面板。
     *
     * @param api 用户管理 API
     * @throws IllegalArgumentException api 为 null
     */
    public UserManagePanel(UserService api) {
        if (api == null) {
            throw new IllegalArgumentException("api must not be null");
        }
        this.m_api = api;
        setLayout(new BorderLayout(0, 12));
        setBackground(UiTheme.BACKGROUND);
        add(createFilterBar(), BorderLayout.NORTH);
        add(createTableArea(), BorderLayout.CENTER);
        add(createActionBar(), BorderLayout.SOUTH);
        refresh();
    }

    /** 按当前条件重新查询并回填表格。 */
    public final void refresh() {
        final UserQuery query = currentQuery();
        UiTasks.run(new UiTasks.Task<PageResponse<User>>() {
            @Override
            public PageResponse<User> run() {
                return m_api.listUsers(query);
            }
        }, new UiTasks.Success<PageResponse<User>>() {
            @Override
            public void accept(PageResponse<User> page) {
                fill(page);
            }
        });
    }

    private UserQuery currentQuery() {
        Role role = null;
        Object selectedRole = m_role.getSelectedItem();
        if (selectedRole != null && !ALL_ROLES.equals(selectedRole)) {
            role = Role.fromDisplayName(String.valueOf(selectedRole));
        }
        Boolean enabled = null;
        Object selectedState = m_enabled.getSelectedItem();
        if ("启用".equals(selectedState)) {
            enabled = Boolean.TRUE;
        } else if ("禁用".equals(selectedState)) {
            enabled = Boolean.FALSE;
        }
        return new UserQuery(m_keyword.getText(), role, enabled, m_page_number, m_page_size);
    }

    private void fill(PageResponse<User> page) {
        m_rows.clear();
        m_rows.addAll(page.getItems());
        m_model.setRowCount(0);
        for (User user : page.getItems()) {
            m_model.addRow(new Object[] { user.getUserName(), user.getDisplayName(),
                    user.getRole() == null ? "-" : user.getRole().getDisplayName(),
                    user.isEnabled() ? "启用" : "禁用" });
        }
        m_page_number = page.getPageNumber();
        m_page_size = page.getPageSize();
        m_page_label.setText("第 " + m_page_number + " / " + Math.max(1, page.getTotalPages())
                + " 页    共 " + page.getTotal() + " 条");
    }

    private JPanel createFilterBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        bar.setOpaque(false);
        bar.add(new JLabel("关键词"));
        bar.add(m_keyword);
        m_role.setModel(
                new DefaultComboBoxModel<String>(new String[] { ALL_ROLES, "学生", "教师", "管理员" }));
        bar.add(m_role);
        m_enabled
                .setModel(new DefaultComboBoxModel<String>(new String[] { ANY_STATE, "启用", "禁用" }));
        bar.add(m_enabled);
        JButton search = UiFactory.primaryButton("查询", "search");
        search.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                m_page_number = 1;
                refresh();
            }
        });
        bar.add(search);
        return bar;
    }

    private JScrollPane createTableArea() {
        m_table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        m_table.setRowHeight(26);
        m_table.setFont(UiTheme.font(Font.PLAIN, 13F));
        m_table.getTableHeader().setFont(UiTheme.font(Font.BOLD, 13F));
        JScrollPane scroll = new JScrollPane(m_table);
        scroll.setPreferredSize(new Dimension(720, 300));
        scroll.setBorder(BorderFactory.createLineBorder(UiTheme.MUTED));
        return scroll;
    }

    private JPanel createActionBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        actions.setOpaque(false);
        actions.add(button("启用 / 禁用", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                toggleSelected();
            }
        }));
        actions.add(button("编辑姓名", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                renameSelected();
            }
        }));
        actions.add(button("新建用户", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                createUser();
            }
        }));
        actions.add(button("注销", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                unregisterSelected();
            }
        }));
        actions.add(button("批量注册(文件)", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                batchRegisterFromFile();
            }
        }));
        actions.add(button("批量注销(选中)", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                batchUnregisterSelected();
            }
        }));
        bar.add(actions, BorderLayout.WEST);
        JPanel pager = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        pager.setOpaque(false);
        pager.add(button("上一页", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (m_page_number > 1) {
                    m_page_number--;
                    refresh();
                }
            }
        }));
        m_page_label.setForeground(UiTheme.MUTED);
        pager.add(m_page_label);
        pager.add(button("下一页", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                m_page_number++;
                refresh();
            }
        }));
        bar.add(pager, BorderLayout.EAST);
        return bar;
    }

    private JButton button(String text, ActionListener listener) {
        JButton button = new JButton(text);
        button.addActionListener(listener);
        return button;
    }

    /** 从文件批量注册（命令 103）：格式为 登录名,姓名,角色,口令，逐行解析。 */
    private void batchRegisterFromFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("选择批量注册文件（登录名,姓名,角色,口令）");
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        final File file = chooser.getSelectedFile();
        final List<RegisterRequest> requests;
        try {
            requests = UserImportFile.parse(file);
        } catch (IOException e) {
            warn("读取文件失败：" + e.getMessage());
            return;
        } catch (IllegalArgumentException e) {
            warn(e.getMessage());
            return;
        }
        if (requests.isEmpty()) {
            warn("文件中没有可导入的账号");
            return;
        }
        if (JOptionPane.showConfirmDialog(this,
                "共解析到 " + requests.size() + " 个账号，确认导入？已存在的登录名会被跳过并记入失败明细。", "批量注册",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
            return;
        }
        UiTasks.run(new UiTasks.Task<BatchResult>() {
            @Override
            public BatchResult run() {
                return m_api.batchRegister(requests);
            }
        }, new UiTasks.Success<BatchResult>() {
            @Override
            public void accept(BatchResult result) {
                showBatchResult("批量注册结果", result);
                refresh();
            }
        });
    }

    /** 批量注销选中的多个账号（命令 105）。 */
    private void batchUnregisterSelected() {
        int[] rows = m_table.getSelectedRows();
        if (rows == null || rows.length == 0) {
            warn("请先在表格中选中要注销的账号（可按 Ctrl/Shift 多选）");
            return;
        }
        final List<String> names = new ArrayList<String>();
        StringBuilder preview = new StringBuilder();
        for (int row : rows) {
            if (row >= 0 && row < m_rows.size()) {
                names.add(m_rows.get(row).getUserName());
                preview.append(m_rows.get(row).getUserName()).append("  ");
            }
        }
        if (JOptionPane.showConfirmDialog(this,
                "确认注销以下 " + names.size() + " 个账号？各模块档案会一并撤销。\n\n" + preview, "批量注销",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
            return;
        }
        UiTasks.run(new UiTasks.Task<BatchResult>() {
            @Override
            public BatchResult run() {
                return m_api.batchUnregister(names);
            }
        }, new UiTasks.Success<BatchResult>() {
            @Override
            public void accept(BatchResult result) {
                showBatchResult("批量注销结果", result);
                refresh();
            }
        });
    }

    private void showBatchResult(String title, BatchResult result) {
        StringBuilder text = new StringBuilder(result.summary());
        for (BatchResult.Failure failure : result.getFailures()) {
            text.append("\n").append(failure);
        }
        JOptionPane.showMessageDialog(this, text.toString(), title,
                result.isAllSucceeded() ? JOptionPane.INFORMATION_MESSAGE
                        : JOptionPane.WARNING_MESSAGE);
    }

    private void warn(String message) {
        JOptionPane.showMessageDialog(this, message, "提示", JOptionPane.WARNING_MESSAGE);
    }

    private User selected() {
        int row = m_table.getSelectedRow();
        if (row < 0 || row >= m_rows.size()) {
            JOptionPane.showMessageDialog(this, "请先在表格中选择一个用户", "提示",
                    JOptionPane.INFORMATION_MESSAGE);
            return null;
        }
        return m_rows.get(row);
    }

    private void toggleSelected() {
        final User user = selected();
        if (user == null) {
            return;
        }
        final boolean target = !user.isEnabled();
        UiTasks.run(new UiTasks.Task<Void>() {
            @Override
            public Void run() {
                m_api.toggleUserEnabled(user.getUserName(), target);
                return null;
            }
        }, new UiTasks.Success<Void>() {
            @Override
            public void accept(Void ignored) {
                refresh();
            }
        });
    }

    private void renameSelected() {
        final User user = selected();
        if (user == null) {
            return;
        }
        final String displayName = JOptionPane.showInputDialog(this, "新的姓名", user.getDisplayName());
        if (displayName == null || displayName.trim().length() == 0) {
            return;
        }
        UiTasks.run(new UiTasks.Task<Void>() {
            @Override
            public Void run() {
                m_api.updateUser(new UserUpdateRequest(user.getUserName(), displayName));
                return null;
            }
        }, new UiTasks.Success<Void>() {
            @Override
            public void accept(Void ignored) {
                refresh();
            }
        });
    }

    private void unregisterSelected() {
        final User user = selected();
        if (user == null) {
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this,
                "确定注销账号 " + user.getUserName() + " ？该账号的各模块档案会一并撤销。", "确认注销",
                JOptionPane.OK_CANCEL_OPTION);
        if (choice != JOptionPane.OK_OPTION) {
            return;
        }
        UiTasks.run(new UiTasks.Task<Void>() {
            @Override
            public Void run() {
                m_api.unregister(user.getUserName());
                return null;
            }
        }, new UiTasks.Success<Void>() {
            @Override
            public void accept(Void ignored) {
                refresh();
            }
        });
    }

    private void createUser() {
        JTextField name = new JTextField(12);
        JTextField display = new JTextField(12);
        JPasswordField password = new JPasswordField(12);
        JComboBox<String> role = new JComboBox<String>(new String[] { "学生", "教师", "管理员" });
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("登录名"));
        form.add(name);
        form.add(new JLabel("姓名"));
        form.add(display);
        form.add(new JLabel("角色"));
        form.add(role);
        form.add(new JLabel("初始密码"));
        form.add(password);
        int choice = JOptionPane.showConfirmDialog(this, form, "新建用户", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
            return;
        }
        final String userName = name.getText().trim();
        final String displayName = display.getText().trim();
        final String plain = new String(password.getPassword());
        final Role selectedRole = Role.fromDisplayName(String.valueOf(role.getSelectedItem()));
        if (userName.length() == 0 || plain.length() == 0) {
            JOptionPane.showMessageDialog(this, "登录名与密码不能为空", "无法新建", JOptionPane.WARNING_MESSAGE);
            return;
        }
        UiTasks.run(new UiTasks.Task<Void>() {
            @Override
            public Void run() {
                m_api.register(userName, displayName, selectedRole, plain);
                return null;
            }
        }, new UiTasks.Success<Void>() {
            @Override
            public void accept(Void ignored) {
                JOptionPane.showMessageDialog(UserManagePanel.this,
                        "账号 " + userName + " 已创建，档案已同步建立", "新建成功", JOptionPane.INFORMATION_MESSAGE);
                refresh();
            }
        }, new UiTasks.Failure() {
            @Override
            public void accept(ApiException error) {
                JOptionPane.showMessageDialog(UserManagePanel.this, error.getMessage(), "操作失败",
                        JOptionPane.WARNING_MESSAGE);
            }
        });
    }
}
