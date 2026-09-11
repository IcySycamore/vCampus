package edu.seu.vcampus.common.bank.entity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** BankAccount 的金额、状态、数据封装与序列化测试。 */
class BankAccountTest {

    @Test
    void defaultsToZeroBalanceAndNormalStatus() {
        BankAccount account = new BankAccount();

        assertEquals(BigDecimal.ZERO, account.getBalance());
        assertEquals(BankAccountStatus.NORMAL, account.getStatus());
        assertEquals("正常", account.getStatus().getDisplayName());
        assertEquals(BankAccountStatus.FROZEN,
                BankAccountStatus.fromDisplayName("冻结"));
        assertTrue(account.isOperational());
        assertNull(account.getCreatedAt());
        assertNull(account.getUpdatedAt());
    }

    @Test
    void depositAndWithdrawUpdateBalanceAndTimestamp() {
        BankAccount account = new BankAccount();

        account.deposit(new BigDecimal("10.50"));
        Date afterDeposit = account.getUpdatedAt();
        account.withdraw(new BigDecimal("3.25"));

        assertEquals(new BigDecimal("7.25"), account.getBalance());
        assertNotNull(afterDeposit);
        assertNotNull(account.getUpdatedAt());
    }

    @Test
    void permitsWithdrawingTheEntireBalance() {
        BankAccount account = new BankAccount();
        account.deposit(BigDecimal.TEN);

        account.withdraw(BigDecimal.TEN);

        assertEquals(BigDecimal.ZERO, account.getBalance());
    }

    @Test
    void rejectsInvalidAmountsAndInsufficientBalance() {
        final BankAccount account = new BankAccount();

        assertOperationRejected(account, null, true);
        assertOperationRejected(account, BigDecimal.ZERO, true);
        assertOperationRejected(account, BigDecimal.ONE.negate(), true);
        assertOperationRejected(account, BigDecimal.ONE, false);
        assertFalse(account.hasSufficientBalance(null));
    }

    @Test
    void rejectsNullAndNegativeBalances() {
        final BankAccount account = new BankAccount();

        assertBalanceRejected(account, null);
        assertBalanceRejected(account, BigDecimal.ONE.negate());
    }

    @Test
    void nonNormalAccountsCannotOperate() {
        final BankAccount frozen = new BankAccount();
        frozen.setStatus(BankAccountStatus.FROZEN);
        final BankAccount closed = new BankAccount();
        closed.setStatus(BankAccountStatus.CLOSED);

        assertFalse(frozen.isOperational());
        assertFalse(closed.isOperational());
        assertStateRejected(frozen);
        assertStateRejected(closed);
    }

    @Test
    void rejectsBlankIdentifiersAndNullStatus() {
        final BankAccount account = new BankAccount();

        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() {
                account.setAccountId("  ");
            }
        });
        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() {
                account.setUserId(null);
            }
        });
        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() {
                account.setStatus(null);
            }
        });
    }

    @Test
    void datesAreDefensivelyCopied() {
        Date created = new Date(1000L);
        BankAccount account = new BankAccount();
        account.setCreatedAt(created);

        created.setTime(2000L);
        Date read = account.getCreatedAt();
        read.setTime(3000L);

        assertNotSame(created, account.getCreatedAt());
        assertEquals(new Date(1000L), account.getCreatedAt());
    }

    @Test
    void serializationRoundTripPreservesAccount() throws Exception {
        Date created = new Date(1000L);
        Date updated = new Date(2000L);
        BankAccount original = new BankAccount("A001", 1L, new BigDecimal("20.50"),
                BankAccountStatus.FROZEN, created, updated);

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ObjectOutputStream output = new ObjectOutputStream(bytes);
        output.writeObject(original);
        output.flush();
        ObjectInputStream input = new ObjectInputStream(
                new ByteArrayInputStream(bytes.toByteArray()));
        BankAccount copy = (BankAccount) input.readObject();

        assertEquals(original.getAccountId(), copy.getAccountId());
        assertEquals(original.getUserId(), copy.getUserId());
        assertEquals(original.getBalance(), copy.getBalance());
        assertEquals(original.getStatus(), copy.getStatus());
        assertEquals(original.getCreatedAt(), copy.getCreatedAt());
        assertEquals(original.getUpdatedAt(), copy.getUpdatedAt());
    }

    private static void assertOperationRejected(final BankAccount account,
            final BigDecimal amount, final boolean deposit) {
        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() {
                if (deposit) {
                    account.deposit(amount);
                } else {
                    account.withdraw(amount);
                }
            }
        });
    }

    private static void assertStateRejected(final BankAccount account) {
        assertThrows(IllegalStateException.class, new Executable() {
            @Override
            public void execute() {
                account.deposit(BigDecimal.ONE);
            }
        });
        assertThrows(IllegalStateException.class, new Executable() {
            @Override
            public void execute() {
                account.withdraw(BigDecimal.ONE);
            }
        });
    }

    private static void assertBalanceRejected(final BankAccount account,
            final BigDecimal balance) {
        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() {
                account.setBalance(balance);
            }
        });
    }
}
