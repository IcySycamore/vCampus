package edu.seu.vcampus.client.view.bank;

import edu.seu.vcampus.common.bank.dto.BankAdminAccountView;

import java.math.BigDecimal;
import java.util.List;
import javax.swing.table.DefaultTableModel;

/** 管理端账户表的列定义与单元格格式化。 */
final class BankAdminTableModels {

    /** 表头。 */
    private static final String[] COLUMNS =
            {"登录名", "姓名", "角色", "账户状态", "余额 /元", "账户号"};

    /** 私有构造器，禁止实例化工具类。 */
    private BankAdminTableModels() {
    }

    /** @return 不可编辑的空账户表模型 */
    static DefaultTableModel create() {
        return new DefaultTableModel(COLUMNS, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    /**
     * 用一页账户填充表格。
     *
     * @param model 目标模型
     * @param rows 账户视图
     */
    static void fill(DefaultTableModel model, List<BankAdminAccountView> rows) {
        model.setRowCount(0);
        for (BankAdminAccountView row : rows) {
            model.addRow(new Object[] {row.getUsername(), text(row.getDisplayName()),
                    roleName(row), statusName(row), balance(row), text(row.getAccountId())});
        }
    }

    /** @return 角色显示名；未知角色为破折号 */
    private static String roleName(BankAdminAccountView row) {
        return row.getRole() == null ? "—" : row.getRole().getDisplayName();
    }

    /** @return 账户状态显示名；未开户单独标注 */
    private static String statusName(BankAdminAccountView row) {
        if (!row.isOpened()) {
            return "未开户";
        }
        return row.getStatus() == null ? "—" : row.getStatus().getDisplayName();
    }

    /** @return 余额文本；未开户为破折号 */
    private static String balance(BankAdminAccountView row) {
        BigDecimal amount = row.getBalance();
        return amount == null ? "—" : BankTableModels.money(amount);
    }

    /** @return 空值显示为破折号 */
    private static String text(String value) {
        return value == null ? "—" : value;
    }
}
