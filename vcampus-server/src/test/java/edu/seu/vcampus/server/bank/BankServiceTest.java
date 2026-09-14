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
    private static final String OWNER_UUID = "7f4c2a10-94ad-4b42-8cae-51fd93e6a001";
    private static final String OTHER_UUID = "7f4c2a10-94ad-4b42-8cae-51fd93e6a002";
    private final BankService bank = new BankService();

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3})
    void operationsNeverOpenMissingAccount(final int operation) {
        assertThrows(BankAccountNotOpenedException.class, new Executable() {
            @Override
            public void execute() {
                switch (operation) {
                    case 0:
                        bank.queryAccount(OWNER_UUID);
                        break;
                    case 1:
                        bank.recharge(OWNER_UUID, BigDecimal.TEN);
                        break;
                    case 2:
                        bank.consume(OWNER_UUID, BigDecimal.ONE, "order", "消费");
                        break;
                    default:
                        bank.listTransactions(OWNER_UUID, null);
                }
            }
        });
        assertThrows(BankAccountNotOpenedException.class, new Executable() {
            @Override
            public void execute() {
                bank.queryAccount(OWNER_UUID);
            }
        });
        assertEquals(BigDecimal.ZERO, bank.openAccount(OWNER_UUID).getBalance());
        assertEquals(0, bank.listTransactions(OWNER_UUID, null).getTotalCount());
    }

    @Test
    void explicitOpeningAndRetriesPreserveAccountAndFunds() {
        BankAccountResponse opened = bank.openAccount(OWNER_UUID);
        assertEquals(BigDecimal.ZERO, opened.getBalance());
        assertNotNull(opened.getCreatedAt());
        assertEquals(opened.getCreatedAt(), opened.getUpdatedAt());
        bank.recharge(OWNER_UUID, BigDecimal.TEN);
        BankAccountResponse funded = bank.queryAccount(OWNER_UUID);
        BankAccountResponse retried = bank.openAccount(OWNER_UUID);
        assertEquals(opened.getAccountId(), retried.getAccountId());
        assertEquals(opened.getCreatedAt(), retried.getCreatedAt());
        assertEquals(funded.getUpdatedAt(), retried.getUpdatedAt());
        assertEquals(BigDecimal.TEN, retried.getBalance());
        assertEquals(1, bank.listTransactions(OWNER_UUID, null).getTotalCount());
        assertEquals(BigDecimal.ZERO, opened.getBalance());
    }

    @Test
    void differentStableIdsHaveSeparateAccountsAndLedgers() {
        BankAccountResponse first = bank.openAccount(OWNER_UUID);
        BankAccountResponse second = bank.openAccount(OTHER_UUID);
        assertNotEquals(first.getAccountId(), second.getAccountId());
        bank.recharge(OWNER_UUID, BigDecimal.TEN);
        assertEquals(BigDecimal.ZERO, bank.queryAccount(OTHER_UUID).getBalance());
        assertEquals(0, bank.listTransactions(OTHER_UUID, null).getTotalCount());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t\n"})
    void rejectsBlankOwnerUuids(final String ownerUuid) {
        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() {
                bank.openAccount(ownerUuid);
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
        bank.openAccount(OWNER_UUID);
        bank.recharge(OWNER_UUID, new BigDecimal("10.50"));
        BankTransaction debit = bank.consume(OWNER_UUID, new BigDecimal("3.25"), "order-1", "午餐");
        assertEquals(new BigDecimal("10.50"), debit.getBalanceBefore());
        assertEquals(new BigDecimal("7.25"), debit.getBalanceAfter());
        assertEquals("order-1", debit.getRelatedOrderId());
        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() {
                bank.consume(OWNER_UUID, BigDecimal.TEN, "order-2", null);
            }
        });
        assertEquals(new BigDecimal("7.25"), bank.queryAccount(OWNER_UUID).getBalance());
        assertEquals(2, bank.listTransactions(OWNER_UUID, null).getTotalCount());
        bank.consume(OWNER_UUID, new BigDecimal("7.25"), null, null);
        assertEquals(0, bank.queryAccount(OWNER_UUID).getBalance().signum());
    }

    @Test
    void cashbackCreditsAccountAndRecordsRelatedOrder() {
        bank.openAccount(OWNER_UUID);
        bank.recharge(OWNER_UUID, new BigDecimal("10.00"));
        BankTransaction transaction = bank.cashback(OWNER_UUID, new BigDecimal("2.50"),
                "order-1", "订单返现");

        assertEquals(BankTransactionType.CASHBACK, transaction.getType());
        assertEquals(new BigDecimal("10.00"), transaction.getBalanceBefore());
        assertEquals(new BigDecimal("12.50"), transaction.getBalanceAfter());
        assertEquals("order-1", transaction.getRelatedOrderId());
        assertEquals("订单返现", transaction.getDescription());
        assertEquals(new BigDecimal("12.50"), bank.queryAccount(OWNER_UUID).getBalance());
        assertEquals(2, bank.listTransactions(OWNER_UUID, null).getTotalCount());
    }

    @Test
    void paginatesAfterFilteringAndHandlesPagesPastEnd() {
        bank.openAccount(OWNER_UUID);
        BankTransaction first = bank.recharge(OWNER_UUID, BigDecimal.TEN).getTransaction();
        bank.consume(OWNER_UUID, BigDecimal.ONE, null, null);
        BankTransaction second = bank.recharge(OWNER_UUID, BigDecimal.ONE).getTransaction();
        BankTransactionListResponse page = bank.listTransactions(OWNER_UUID,
                new BankTransactionQueryRequest(2, 1, BankTransactionType.RECHARGE));
        assertEquals(2, page.getTotalCount());
        assertEquals(2, page.getTotalPages());
        assertEquals(second.getTransactionId(), page.getTransactions().get(0).getTransactionId());
        assertEquals(first.getTransactionId(), bank.listTransactions(OWNER_UUID, null)
                .getTransactions().get(0).getTransactionId());
        assertTrue(bank.listTransactions(OWNER_UUID,
                new BankTransactionQueryRequest(Integer.MAX_VALUE, 100))
                .getTransactions().isEmpty());
    }
}
