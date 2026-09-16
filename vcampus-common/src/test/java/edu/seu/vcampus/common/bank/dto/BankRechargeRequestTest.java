package edu.seu.vcampus.common.bank.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** BankRechargeRequest 的金额校验和序列化测试。 */
class BankRechargeRequestTest {

    @Test
    void acceptsAndSerializesPositiveAmount() throws Exception {
        BankRechargeRequest request = new BankRechargeRequest(new BigDecimal("12.50"));

        BankRechargeRequest copy = BankDtoTestSupport.roundTrip(request);

        assertEquals(new BigDecimal("12.50"), copy.getAmount());
    }

    @Test
    void rejectsNonPositiveOrMissingAmount() {
        assertInvalidAmount(null);
        assertInvalidAmount(BigDecimal.ZERO);
        assertInvalidAmount(BigDecimal.ONE.negate());
    }

    private static void assertInvalidAmount(final BigDecimal amount) {
        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() {
                new BankRechargeRequest(amount);
            }
        });
    }
}
