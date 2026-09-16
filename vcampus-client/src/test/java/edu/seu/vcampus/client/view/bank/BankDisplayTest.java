package edu.seu.vcampus.client.view.bank;

import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.common.bank.entity.BankTransaction;
import edu.seu.vcampus.common.bank.entity.BankTransactionType;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import javax.swing.table.DefaultTableModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** 金额输入及流水格式的纯逻辑测试，不启动窗口。 */
class BankDisplayTest {
    @Test
    void acceptsExactDecimalAndRejectsInvalidInputs() {
        assertEquals(new BigDecimal("10.50"), RechargeAmount.parse(" 10.50 "));
        for (final String value : Arrays.asList("", "0", "-1", "1.001", "1e2", "NaN")) {
            assertThrows(ApiException.class, new Executable() {
                @Override
                public void execute() { RechargeAmount.parse(value); }
            });
        }
    }

    @Test
    void rendersSignedTransactionsAndClearsOldRows() {
        DefaultTableModel model = BankTableModels.create();
        BankTransaction expense = new BankTransaction("T-1", "A-1",
                BankTransactionType.CONSUMPTION, new BigDecimal("12.50"),
                new BigDecimal("100"), new BigDecimal("87.50"), null, "校园消费", new Date());
        BankTableModels.fill(model, Collections.singletonList(expense));
        assertEquals("消费", model.getValueAt(0, 1));
        assertEquals("−12.50", model.getValueAt(0, 2));
        assertEquals("87.50", model.getValueAt(0, 3));
        assertFalse(model.isCellEditable(0, 2));
        BankTableModels.fill(model, Collections.<BankTransaction>emptyList());
        assertEquals(0, model.getRowCount());
        assertEquals(BankTransactionType.CASHBACK, BankTableModels.typeAt(3));
    }
}
