package edu.seu.vcampus.server.bank;

import edu.seu.vcampus.common.bank.dto.BankTransactionQueryRequest;
import edu.seu.vcampus.common.bank.entity.BankTransaction;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 并发开户唯一性、余额和流水一致性以及禁止透支测试。 */
class BankConcurrencyTest {
    private static final String OWNER_UUID = "7f4c2a10-94ad-4b42-8cae-51fd93e6a001";
    private final BankService bank = new BankService();

    @Test
    void simultaneousOpeningReturnsOneAccount() throws Exception {
        List<String> ids = runConcurrently(new Callable<String>() {
            @Override
            public String call() {
                return bank.openAccount(OWNER_UUID).getAccountId();
            }
        });
        assertEquals(1, new HashSet<String>(ids).size());
        assertEquals(0, bank.listTransactions(OWNER_UUID, null).getTotalCount());
    }

    @Test
    void openingRetriesAndCreditsDoNotResetOrLoseFunds() throws Exception {
        bank.openAccount(OWNER_UUID);
        bank.recharge(OWNER_UUID, BigDecimal.TEN);
        runConcurrently(new Callable<Boolean>() {
            @Override
            public Boolean call() {
                bank.openAccount(OWNER_UUID);
                bank.recharge(OWNER_UUID, BigDecimal.ONE);
                return true;
            }
        });
        assertEquals(new BigDecimal("50"), bank.queryAccount(OWNER_UUID).getBalance());
        List<BankTransaction> ledger = bank.listTransactions(OWNER_UUID,
                new BankTransactionQueryRequest(1, 100)).getTransactions();
        assertEquals(41, ledger.size());
        Set<String> transactionIds = new HashSet<String>();
        BigDecimal balance = BigDecimal.ZERO;
        for (BankTransaction transaction : ledger) {
            assertEquals(balance, transaction.getBalanceBefore());
            balance = balance.add(transaction.getAmount());
            assertEquals(balance, transaction.getBalanceAfter());
            assertTrue(transactionIds.add(transaction.getTransactionId()));
        }
        assertEquals(bank.queryAccount(OWNER_UUID).getBalance(), balance);
    }

    @Test
    void simultaneousDebitsNeverOverdrawOrRecordFailedPayments() throws Exception {
        bank.openAccount(OWNER_UUID);
        bank.recharge(OWNER_UUID, BigDecimal.TEN);
        List<Boolean> results = runConcurrently(new Callable<Boolean>() {
            @Override
            public Boolean call() {
                try {
                    bank.consume(OWNER_UUID, BigDecimal.ONE, null, null);
                    return true;
                } catch (IllegalArgumentException insufficientBalance) {
                    return false;
                }
            }
        });
        int successful = 0;
        for (Boolean result : results) {
            if (result) {
                successful++;
            }
        }
        assertEquals(10, successful);
        assertEquals(BigDecimal.ZERO, bank.queryAccount(OWNER_UUID).getBalance());
        assertEquals(11, bank.listTransactions(OWNER_UUID, null).getTotalCount());
    }

    private <T> List<T> runConcurrently(final Callable<T> action) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(8);
        final CountDownLatch start = new CountDownLatch(1);
        List<Future<T>> futures = new ArrayList<Future<T>>();
        try {
            for (int i = 0; i < 40; i++) {
                futures.add(pool.submit(new Callable<T>() {
                    @Override
                    public T call() throws Exception {
                        start.await();
                        return action.call();
                    }
                }));
            }
            start.countDown();
            List<T> results = new ArrayList<T>();
            for (Future<T> future : futures) {
                results.add(future.get(10, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        }
    }
}
