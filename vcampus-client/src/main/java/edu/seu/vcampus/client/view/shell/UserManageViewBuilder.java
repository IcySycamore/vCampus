package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.client.view.theme.UiFactory;
import edu.seu.vcampus.client.view.theme.UiTheme;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

/** Builds controls for the user management panel. */
final class UserManageViewBuilder {
    private static final int TOGGLE = 0;
    private static final int RENAME = 1;
    private static final int CREATE = 2;
    private static final int UNREGISTER = 3;
    private static final int BATCH_REGISTER = 4;
    private static final int BATCH_UNREGISTER = 5;
    private static final int PREVIOUS = 6;
    private static final int NEXT = 7;
    private final UserManagePanel panel;
    private final UserManageAccountActions accounts;
    private final UserManageBatchActions batches;

    UserManageViewBuilder(UserManagePanel panel) {
        this.panel = panel;
        accounts = new UserManageAccountActions(panel);
        batches = new UserManageBatchActions(panel);
    }

    JPanel createFilterBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        bar.setOpaque(false);
        bar.add(new JLabel("关键词"));
        bar.add(panel.keyword());
        panel.role().setModel(new DefaultComboBoxModel<String>(new String[] {
                UserManagePanel.ALL_ROLES, "学生", "教师", "管理员"}));
        bar.add(panel.role());
        panel.enabled().setModel(new DefaultComboBoxModel<String>(new String[] {
                UserManagePanel.ANY_STATE, "启用", "禁用"}));
        bar.add(panel.enabled());
        JButton search = UiFactory.primaryButton("查询", "search");
        search.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                panel.searchFirstPage();
            }
        });
        bar.add(search);
        return bar;
    }

    JScrollPane createTableArea() {
        panel.table().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        panel.table().setRowHeight(26);
        panel.table().setFont(UiTheme.font(Font.PLAIN, 13F));
        panel.table().getTableHeader().setFont(UiTheme.font(Font.BOLD, 13F));
        JScrollPane scroll = new JScrollPane(panel.table());
        scroll.setPreferredSize(new Dimension(720, 300));
        scroll.setBorder(BorderFactory.createLineBorder(UiTheme.MUTED));
        return scroll;
    }

    JPanel createActionBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        actions.setOpaque(false);
        actions.add(button("启用 / 禁用", TOGGLE));
        actions.add(button("编辑姓名", RENAME));
        actions.add(button("新建用户", CREATE));
        actions.add(button("注销", UNREGISTER));
        actions.add(button("批量注册(文件)", BATCH_REGISTER));
        actions.add(button("批量注销(选中)", BATCH_UNREGISTER));
        bar.add(actions, BorderLayout.WEST);
        JPanel pager = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        pager.setOpaque(false);
        pager.add(button("上一页", PREVIOUS));
        panel.pageLabel().setForeground(UiTheme.MUTED);
        pager.add(panel.pageLabel());
        pager.add(button("下一页", NEXT));
        bar.add(pager, BorderLayout.EAST);
        return bar;
    }

    private JButton button(String text, final int action) {
        JButton button = new JButton(text);
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                perform(action);
            }
        });
        return button;
    }

    private void perform(int action) {
        switch (action) {
            case TOGGLE:
                accounts.toggleSelected();
                break;
            case RENAME:
                accounts.renameSelected();
                break;
            case CREATE:
                accounts.createUser();
                break;
            case UNREGISTER:
                accounts.unregisterSelected();
                break;
            case BATCH_REGISTER:
                batches.registerFromFile();
                break;
            case BATCH_UNREGISTER:
                batches.unregisterSelected();
                break;
            case PREVIOUS:
                panel.previousPage();
                break;
            case NEXT:
                panel.nextPage();
                break;
            default:
                throw new IllegalArgumentException("unknown user action");
        }
    }
}
