package edu.seu.vcampus.common.bank.dto;

import edu.seu.vcampus.common.bank.entity.BankAccountStatus;
import edu.seu.vcampus.common.bank.entity.BankTransaction;
import edu.seu.vcampus.common.bank.entity.BankTransactionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.math.BigDecimal;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** BankRechargeResponse 的完整性校验和序列化测试。 */
class BankRechargeResponseTest {

    @Test
    void containsAndSerializesUpdatedAccountAndTransaction() throws Exception {
        BankRechargeResponse response = new BankRechargeResponse(account(), transaction());

        BankRechargeResponse copy = BankDtoTestSupport.roundTrip(response);

        assertEquals(new BigDecimal("15.00"), copy.getAccount().getBalance());
        assertEquals("T001", copy.getTransaction().getTransactionId());
        assertEquals(BankTransactionType.RECHARGE, copy.getTransaction().getType());
    }

    @Test
    void rejectsMissingAccountOrTransaction() {
        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() {
                new BankRechargeResponse(null, transaction());
            }
        });
        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() {
                new BankRechargeResponse(account(), null);
            }
        });
    }

    private static BankAccountResponse account() {
        return new BankAccountResponse("A001", new BigDecimal("15.00"),
                BankAccountStatus.NORMAL, new Date(1000L), new Date(2000L));
    }

    private static BankTransaction transaction() {
        return new BankTransaction("T001", "A001", BankTransactionType.RECHARGE,
                new BigDecimal("5.00"), new BigDecimal("10.00"),
                new BigDecimal("15.00"), null, "账户充值", new Date(2000L));
    }
}
