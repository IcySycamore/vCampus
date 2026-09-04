package edu.seu.vcampus.common.bank.entity;

import java.math.BigDecimal;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** BankAccount 的金额、状态和数据封装测试。 */
class BankAccountTest {

    @Test
    void depositAndWithdrawUpdateBalance() {
        final BankAccount account = new BankAccount();
        account.deposit(new BigDecimal("10.50"));
        account.withdraw(new BigDecimal("3.25"));
        assertEquals(new BigDecimal("7.25"), account.getBalance());
    }

    @Test
    void rejectsInvalidAmountAndInsufficientBalance() {
        final BankAccount account = new BankAccount();
        assertThrows(IllegalArgumentException.class,
                new Executable() {
                    @Override
                    public void execute() {
                        account.deposit(BigDecimal.ZERO);
                    }
                });
        assertThrows(IllegalArgumentException.class,
                new Executable() {
                    @Override
                    public void execute() {
                        account.withdraw(new BigDecimal("0.01"));
                    }
                });
    }

    @Test
    void frozenAccountCannotOperate() {
        final BankAccount account = new BankAccount();
        account.setStatus(BankAccount.STATUS_FROZEN);
        assertFalse(account.isOperational());
        assertThrows(IllegalStateException.class,
                new Executable() {
                    @Override
                    public void execute() {
                        account.deposit(BigDecimal.ONE);
                    }
                });
    }

    @Test
    void datesAreDefensivelyCopied() {
        Date created = new Date();
        BankAccount account = new BankAccount();
        account.setCreatedAt(created);
        Date read = account.getCreatedAt();
        assertNotSame(created, read);
        assertEquals(created, read);
    }
}
