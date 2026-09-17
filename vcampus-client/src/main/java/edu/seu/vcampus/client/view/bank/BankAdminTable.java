package edu.seu.vcampus.client.view.bank;

import edu.seu.vcampus.client.view.component.RoundedPanel;
import edu.seu.vcampus.client.view.theme.UiFactory;
import edu.seu.vcampus.client.view.theme.UiTheme;
import edu.seu.vcampus.common.bank.dto.BankAdminAccountView;
import edu.seu.vcampus.common.message.PageResponse;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

/** 管理端账户表格：显示一页账户并暴露当前选中行。 */
final class BankAdminTable extends RoundedPanel {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 表格模型。 */
    private final DefaultTableModel model = BankAdminTableModels.create();

    /** 表格。 */
    private final JTable table = new JTable(model);

    /** 与表格行平行的账户快照。 */
    private final List<BankAdminAccountView> rows = new ArrayList<BankAdminAccountView>();

    /** 创建账户表格。 */
    BankAdminTable() {
        super(new BorderLayout(), 20, UiTheme.SURFACE);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFont(UiTheme.font(Font.PLAIN, 13F));
        table.setRowHeight(28);
        UiFactory.styleTable(table);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER));
        scroll.setPreferredSize(new Dimension(780, 320));
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        add(scroll, BorderLayout.CENTER);
    }

    /**
     * 显示一页账户；null 或空页清空表格。
     *
     * @param page 分页结果
     */
    void show(PageResponse<BankAdminAccountView> page) {
        rows.clear();
        if (page != null) {
            rows.addAll(page.getItems());
        }
        BankAdminTableModels.fill(model, rows);
    }

    /** @return 当前单选的行；未选中返回 null */
    BankAdminAccountView selectedAccount() {
        int index = table.getSelectedRow();
        if (index < 0 || index >= rows.size()) {
            return null;
        }
        return rows.get(index);
    }

    /** @return 表格组件（供测试驱动） */
    JTable table() {
        return table;
    }
}
