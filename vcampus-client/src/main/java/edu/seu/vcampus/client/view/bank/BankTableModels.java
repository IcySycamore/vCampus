package edu.seu.vcampus.client.view.bank;

import edu.seu.vcampus.common.bank.entity.BankTransaction;
import edu.seu.vcampus.common.bank.entity.BankTransactionType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.List;
import javax.swing.table.DefaultTableModel;

/** 银行流水展示转换；格式化和类型映射集中在此。 */
final class BankTableModels {
    private static final String[] COLUMNS = {"交易时间", "类型", "变动金额 / 元", "交易后余额 / 元", "说明"};

    private BankTableModels() {
    }

    static DefaultTableModel create() {
        return new DefaultTableModel(COLUMNS, 0) {
            private static final long serialVersionUID = 1L;
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    static void fill(DefaultTableModel model, List<BankTransaction> rows) {
        model.setRowCount(0);
        SimpleDateFormat date = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        for (BankTransaction row : rows) {
            String sign = row.getType() == BankTransactionType.CONSUMPTION ? "−" : "+";
            model.addRow(new Object[] {date.format(row.getCreatedAt()), typeName(row.getType()),
                    sign + money(row.getAmount()), money(row.getBalanceAfter()),
                    row.getDescription() == null ? "—" : row.getDescription()});
        }
    }

    static String money(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    static String typeName(BankTransactionType type) {
        if (type == BankTransactionType.RECHARGE) {
            return "充值";
        }
        if (type == BankTransactionType.CONSUMPTION) {
            return "消费";
        }
        return "返现";
    }

    static BankTransactionType typeAt(int index) {
        BankTransactionType[] types = {null, BankTransactionType.RECHARGE,
            BankTransactionType.CONSUMPTION, BankTransactionType.CASHBACK};
        return types[index];
    }
}
