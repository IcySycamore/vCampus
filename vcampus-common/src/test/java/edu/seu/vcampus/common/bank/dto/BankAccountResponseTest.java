package edu.seu.vcampus.common.bank.dto;

import edu.seu.vcampus.common.bank.entity.BankAccount;
import edu.seu.vcampus.common.bank.entity.BankAccountStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.math.BigDecimal;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** BankAccountResponse 的实体映射、边界校验和序列化测试。 */
class BankAccountResponseTest {

    @Test
    void mapsAccountWithoutExposingMutableDates() {
        Date createdAt = new Date(1000L);
        Date updatedAt = new Date(2000L);
        BankAccount account = new BankAccount("A001", 1L, new BigDecimal("30.00"),
                BankAccountStatus.NORMAL, createdAt, updatedAt);

        BankAccountResponse response = BankAccountResponse.fromAccount(account);
        Date returnedCreatedAt = response.getCreatedAt();
        returnedCreatedAt.setTime(9000L);

        assertEquals("A001", response.getAccountId());
        assertEquals(new BigDecimal("30.00"), response.getBalance());
        assertEquals(BankAccountStatus.NORMAL, response.getStatus());
        assertEquals(new Date(1000L), response.getCreatedAt());
        assertEquals(new Date(2000L), response.getUpdatedAt());
        assertNotSame(createdAt, response.getCreatedAt());
    }

    @Test
    void serializesAllResponseFields() throws Exception {
        BankAccountResponse response = new BankAccountResponse("A002",
                new BigDecimal("8.50"), BankAccountStatus.FROZEN,
                new Date(1000L), new Date(2000L));

        BankAccountResponse copy = BankDtoTestSupport.roundTrip(response);

        assertEquals(response.getAccountId(), copy.getAccountId());
        assertEquals(response.getBalance(), copy.getBalance());
        assertEquals(response.getStatus(), copy.getStatus());
        assertEquals(response.getCreatedAt(), copy.getCreatedAt());
        assertEquals(response.getUpdatedAt(), copy.getUpdatedAt());
    }

    @Test
    void rejectsInvalidRequiredFields() {
        assertInvalidResponse(" ", BigDecimal.ZERO, BankAccountStatus.NORMAL);
        assertInvalidResponse("A001", null, BankAccountStatus.NORMAL);
        assertInvalidResponse("A001", BigDecimal.ONE.negate(), BankAccountStatus.NORMAL);
        assertInvalidResponse("A001", BigDecimal.ZERO, null);
        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() {
                BankAccountResponse.fromAccount(null);
            }
        });
    }

    private static void assertInvalidResponse(final String accountId,
            final BigDecimal balance, final BankAccountStatus status) {
        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() {
                new BankAccountResponse(accountId, balance, status, null, null);
            }
        });
    }
}
