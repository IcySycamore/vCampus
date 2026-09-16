package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.client.view.theme.UiTheme;
import edu.seu.vcampus.common.message.PageResponse;
import edu.seu.vcampus.common.user.entity.User;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

/**
 * 用户管理表格：只负责「表格 ↔ 用户列表」的映射与选中查询，不含任何网络调用。
 *
 * <p>
 * 从原 486 行的 {@code UserManagePanel} 里拆出（超过 200 行上限），同时让「选中了什么」这件事有单测落点。
 */
public class UserManageTable extends JPanel {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 表格模型（不可编辑）。 */
    private final DefaultTableModel m_model = new DefaultTableModel(
            new Object[] { "登录名", "姓名", "角色", "状态" }, 0) {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    /** 表格组件。 */
    private final JTable m_table = new JTable(m_model);

    /** 当前页记录（与表格行一一对应）。 */
    private final List<User> m_rows = new ArrayList<User>();

    /**
     * 创建表格区域。
     */
    public UserManageTable() {
        setLayout(new BorderLayout());
        setOpaque(false);
        m_table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        m_table.setRowHeight(26);
        m_table.setFont(UiTheme.font(Font.PLAIN, 13F));
        m_table.getTableHeader().setFont(UiTheme.font(Font.BOLD, 13F));
        JScrollPane scroll = new JScrollPane(m_table);
        scroll.setPreferredSize(new Dimension(720, 300));
        scroll.setBorder(BorderFactory.createLineBorder(UiTheme.MUTED));
        add(scroll, BorderLayout.CENTER);
    }

    /**
     * 用一页数据回填表格。
     *
     * @param page 分页结果
     */
    public void show(PageResponse<User> page) {
        m_rows.clear();
        m_model.setRowCount(0);
        if (page == null) {
            return;
        }
        m_rows.addAll(page.getItems());
        for (User user : page.getItems()) {
            m_model.addRow(new Object[] { user.getUserName(), user.getDisplayName(),
                    user.getRole() == null ? "-" : user.getRole().getDisplayName(),
                    user.isEnabled() ? "启用" : "禁用" });
        }
    }

    /**
     * 返回选中的登录名（按表格顺序）。
     *
     * @return 登录名列表；无选中返回空列表
     */
    public List<String> selectedUserNames() {
        List<String> names = new ArrayList<String>();
        for (int row : m_table.getSelectedRows()) {
            if (row >= 0 && row < m_rows.size()) {
                names.add(m_rows.get(row).getUserName());
            }
        }
        return names;
    }

    /**
     * 返回唯一选中的用户。
     *
     * @return 恰好选中一行时返回该用户；否则返回 null
     */
    public User selectedUser() {
        int[] rows = m_table.getSelectedRows();
        if (rows.length != 1 || rows[0] < 0 || rows[0] >= m_rows.size()) {
            return null;
        }
        return m_rows.get(rows[0]);
    }

    /** @return 当前页记录数 */
    public int rowCount() {
        return m_rows.size();
    }

    /** @return 内部表格组件（供宿主挂载或测试断言） */
    public JTable table() {
        return m_table;
    }
}
