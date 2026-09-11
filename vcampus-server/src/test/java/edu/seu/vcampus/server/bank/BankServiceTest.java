package edu.seu.vcampus.server.bank;

import edu.seu.vcampus.common.bank.dto.BankAccountResponse;
import edu.seu.vcampus.common.bank.dto.BankTransactionListResponse;
import edu.seu.vcampus.common.bank.dto.BankTransactionQueryRequest;
import edu.seu.vcampus.common.bank.entity.BankTransaction;
import edu.seu.vcampus.common.bank.entity.BankTransactionType;
import edu.seu.vcampus.common.bank.exception.BankAccountNotOpenedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 开户生命周期、账户隔离、资金与流水一致性测试。 */
class BankServiceTest {
    private final BankService bank = new BankService();

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3})
    void operationsNeverOpenMissingAccount(final int operation) {
        assertThrows(BankAccountNotOpenedException.class, new Executable() {
            @Override
            public void execute() {
                switch (operation) {
                    case 0:
                        bank.queryAccount(1L);
                        break;
                    case 1:
                        bank.recharge(1L, BigDecimal.TEN);
                        break;
                    case 2:
                        bank.consume(1L, BigDecimal.ONE, "order", "消费");
                        break;
                    default:
                        bank.listTransactions(1L, null);
                }
            }
        });
        assertThrows(BankAccountNotOpenedException.class, new Executable() {
            @Override
            public void execute() {
                bank.queryAccount(1L);
            }
        });
        assertEquals(BigDecimal.ZERO, bank.openAccount(1L).getBalance());
        assertEquals(0, bank.listTransactions(1L, null).getTotalCount());
    }

    @Test
    void explicitOpeningAndRetriesPreserveAccountAndFunds() {
        BankAccountResponse opened = bank.openAccount(1L);
        assertEquals(BigDecimal.ZERO, opened.getBalance());
        assertNotNull(opened.getCreatedAt());
        assertEquals(opened.getCreatedAt(), opened.getUpdatedAt());
        bank.recharge(1L, BigDecimal.TEN);
        BankAccountResponse funded = bank.queryAccount(1L);
        BankAccountResponse retried = bank.openAccount(1L);
        assertEquals(opened.getAccountId(), retried.getAccountId());
        assertEquals(opened.getCreatedAt(), retried.getCreatedAt());
        assertEquals(funded.getUpdatedAt(), retried.getUpdatedAt());
        assertEquals(BigDecimal.TEN, retried.getBalance());
        assertEquals(1, bank.listTransactions(1L, null).getTotalCount());
        assertEquals(BigDecimal.ZERO, opened.getBalance());
    }

    @Test
    void differentStableIdsHaveSeparateAccountsAndLedgers() {
        BankAccountResponse first = bank.openAccount(1001L);
        BankAccountResponse second = bank.openAccount(1002L);
        assertNotEquals(first.getAccountId(), second.getAccountId());
        bank.recharge(1001L, BigDecimal.TEN);
        assertEquals(BigDecimal.ZERO, bank.queryAccount(1002L).getBalance());
        assertEquals(0, bank.listTransactions(1002L, null).getTotalCount());
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1})
    void rejectsNonPositiveIds(final long userId) {
        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() {
                bank.openAccount(userId);
            }
        });
    }

    @Test
    void rejectsMissingId() {
        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() {
                bank.openAccount(null);
            }
        });
    }

    @Test
    void balanceAndLedgerAgreeAndFailedDebitHasNoEffect() {
        bank.openAccount(1L);
        bank.recharge(1L, new BigDecimal("10.50"));
        BankTransaction debit = bank.consume(1L, new BigDecimal("3.25"), "order-1", "午餐");
        assertEquals(new BigDecimal("10.50"), debit.getBalanceBefore());
        assertEquals(new BigDecimal("7.25"), debit.getBalanceAfter());
        assertEquals("order-1", debit.getRelatedOrderId());
        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() {
                bank.consume(1L, BigDecimal.TEN, "order-2", null);
            }
        });
        assertEquals(new BigDecimal("7.25"), bank.queryAccount(1L).getBalance());
        assertEquals(2, bank.listTransactions(1L, null).getTotalCount());
        bank.consume(1L, new BigDecimal("7.25"), null, null);
        assertEquals(0, bank.queryAccount(1L).getBalance().signum());
    }

    @Test
    void cashbackCreditsAccountAndRecordsRelatedOrder() {
        bank.openAccount(1L);
        bank.recharge(1L, new BigDecimal("10.00"));
        BankTransaction transaction = bank.cashback(1L, new BigDecimal("2.50"),
                "order-1", "订单返现");

        assertEquals(BankTransactionType.CASHBACK, transaction.getType());
        assertEquals(new BigDecimal("10.00"), transaction.getBalanceBefore());
        assertEquals(new BigDecimal("12.50"), transaction.getBalanceAfter());
        assertEquals("order-1", transaction.getRelatedOrderId());
        assertEquals("订单返现", transaction.getDescription());
        assertEquals(new BigDecimal("12.50"), bank.queryAccount(1L).getBalance());
        assertEquals(2, bank.listTransactions(1L, null).getTotalCount());
    }

    @Test
    void paginatesAfterFilteringAndHandlesPagesPastEnd() {
        bank.openAccount(1L);
        BankTransaction first = bank.recharge(1L, BigDecimal.TEN).getTransaction();
        bank.consume(1L, BigDecimal.ONE, null, null);
        BankTransaction second = bank.recharge(1L, BigDecimal.ONE).getTransaction();
        BankTransactionListResponse page = bank.listTransactions(1L,
                new BankTransactionQueryRequest(2, 1, BankTransactionType.RECHARGE));
        assertEquals(2, page.getTotalCount());
        assertEquals(2, page.getTotalPages());
        assertEquals(second.getTransactionId(), page.getTransactions().get(0).getTransactionId());
        assertEquals(first.getTransactionId(), bank.listTransactions(1L, null)
                .getTransactions().get(0).getTransactionId());
        assertTrue(bank.listTransactions(1L,
                new BankTransactionQueryRequest(Integer.MAX_VALUE, 100))
                .getTransactions().isEmpty());
    }
}
