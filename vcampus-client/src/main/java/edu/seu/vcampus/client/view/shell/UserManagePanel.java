package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.client.user.UserService;
import edu.seu.vcampus.client.view.UiTasks;
import edu.seu.vcampus.client.view.theme.UiTheme;
import edu.seu.vcampus.common.message.PageResponse;
import edu.seu.vcampus.common.user.dto.UserQuery;
import edu.seu.vcampus.common.user.entity.Role;
import edu.seu.vcampus.common.user.entity.User;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

/** Administrator panel for querying and maintaining user accounts. */
public class UserManagePanel extends JPanel {
    private static final long serialVersionUID = 1L;
    static final String ALL_ROLES = "全部角色";
    static final String ANY_STATE = "全部状态";
    private final UserService api;
    private final JTextField keyword = new JTextField(12);
    private final JComboBox<String> role = new JComboBox<String>();
    private final JComboBox<String> enabled = new JComboBox<String>();
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[] {"登录名", "姓名", "角色", "状态"}, 0) {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private final JLabel pageLabel = new JLabel(" ");
    private final List<User> rows = new ArrayList<User>();
    private int pageNumber = 1;
    private int pageSize = 20;

    /**
     * Creates the user management panel.
     * @param api user API
     */
    public UserManagePanel(UserService api) {
        if (api == null) {
            throw new IllegalArgumentException("api must not be null");
        }
        this.api = api;
        UserManageViewBuilder view = new UserManageViewBuilder(this);
        setLayout(new BorderLayout(0, 12));
        setBackground(UiTheme.BACKGROUND);
        add(view.createFilterBar(), BorderLayout.NORTH);
        add(view.createTableArea(), BorderLayout.CENTER);
        add(view.createActionBar(), BorderLayout.SOUTH);
        refresh();
    }

    /** Reloads the current page and filters. */
    public final void refresh() {
        final UserQuery query = currentQuery();
        UiTasks.run(new UiTasks.Task<PageResponse<User>>() {
            @Override
            public PageResponse<User> run() {
                return api.listUsers(query);
            }
        }, new UiTasks.Success<PageResponse<User>>() {
            @Override
            public void accept(PageResponse<User> page) {
                fill(page);
            }
        });
    }

    void searchFirstPage() {
        pageNumber = 1;
        refresh();
    }

    void previousPage() {
        if (pageNumber > 1) {
            pageNumber--;
            refresh();
        }
    }

    void nextPage() {
        pageNumber++;
        refresh();
    }

    UserService api() {
        return api;
    }

    JTextField keyword() {
        return keyword;
    }

    JComboBox<String> role() {
        return role;
    }

    JComboBox<String> enabled() {
        return enabled;
    }

    JTable table() {
        return table;
    }

    JLabel pageLabel() {
        return pageLabel;
    }

    List<User> rows() {
        return rows;
    }

    void warn(String message) {
        javax.swing.JOptionPane.showMessageDialog(this, message, "提示",
                javax.swing.JOptionPane.WARNING_MESSAGE);
    }

    private UserQuery currentQuery() {
        Role selectedRole = null;
        Object roleValue = role.getSelectedItem();
        if (roleValue != null && !ALL_ROLES.equals(roleValue)) {
            selectedRole = Role.fromDisplayName(String.valueOf(roleValue));
        }
        Boolean selectedState = null;
        if ("启用".equals(enabled.getSelectedItem())) {
            selectedState = Boolean.TRUE;
        } else if ("禁用".equals(enabled.getSelectedItem())) {
            selectedState = Boolean.FALSE;
        }
        return new UserQuery(keyword.getText(), selectedRole, selectedState,
                pageNumber, pageSize);
    }

    private void fill(PageResponse<User> page) {
        rows.clear();
        rows.addAll(page.getItems());
        model.setRowCount(0);
        for (User user : page.getItems()) {
            model.addRow(new Object[] {user.getUserName(), user.getDisplayName(),
                    user.getRole() == null ? "-" : user.getRole().getDisplayName(),
                    user.isEnabled() ? "启用" : "禁用"});
        }
        pageNumber = page.getPageNumber();
        pageSize = page.getPageSize();
        pageLabel.setText("第 " + pageNumber + " / "
                + Math.max(1, page.getTotalPages()) + " 页    共 "
                + page.getTotal() + " 条");
    }
}
