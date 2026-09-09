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
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** BankTransaction 的约束、数据封装与序列化测试。 */
class BankTransactionTest {

    @Test
    void createsValidRechargeTransaction() {
        BankTransaction transaction = rechargeTransaction();

        assertEquals("T001", transaction.getTransactionId());
        assertEquals("A001", transaction.getAccountId());
        assertEquals(BankTransactionType.RECHARGE, transaction.getType());
        assertEquals("充值", transaction.getType().getDisplayName());
        assertEquals(BankTransactionType.CONSUMPTION,
                BankTransactionType.fromDisplayName("消费"));
        assertEquals(new BigDecimal("20.00"), transaction.getAmount());
        assertEquals(new BigDecimal("10.00"), transaction.getBalanceBefore());
        assertEquals(new BigDecimal("30.00"), transaction.getBalanceAfter());
        assertEquals("", transaction.getRelatedOrderId());
        assertEquals("校园卡充值", transaction.getDescription());
        assertEquals(new Date(1000L), transaction.getCreatedAt());
    }

    @Test
    void createsValidConsumptionTransaction() {
        BankTransaction transaction = new BankTransaction("T002", "A001",
                BankTransactionType.CONSUMPTION, new BigDecimal("4.50"),
                new BigDecimal("10.00"), new BigDecimal("5.50"), "O001", "商店消费",
                new Date(2000L));

        assertEquals(BankTransactionType.CONSUMPTION, transaction.getType());
        assertEquals(new BigDecimal("5.50"), transaction.getBalanceAfter());
        assertEquals("O001", transaction.getRelatedOrderId());
    }

    @Test
    void rejectsMissingRequiredFields() {
        assertInvalidTransaction(null, "A001", BankTransactionType.RECHARGE,
                new BigDecimal("20.00"), new BigDecimal("10.00"),
                new BigDecimal("30.00"), new Date());
        assertInvalidTransaction("T001", " ", BankTransactionType.RECHARGE,
                new BigDecimal("20.00"), new BigDecimal("10.00"),
                new BigDecimal("30.00"), new Date());
        assertInvalidTransaction("T001", "A001", null, new BigDecimal("20.00"),
                new BigDecimal("10.00"), new BigDecimal("30.00"), new Date());
        assertInvalidTransaction("T001", "A001", BankTransactionType.RECHARGE,
                new BigDecimal("20.00"), new BigDecimal("10.00"),
                new BigDecimal("30.00"), null);
    }

    @Test
    void rejectsInvalidAmountsAndBalances() {
        assertInvalidTransaction("T001", "A001", BankTransactionType.RECHARGE,
                null, BigDecimal.ZERO, BigDecimal.ZERO, new Date());
        assertInvalidTransaction("T001", "A001", BankTransactionType.RECHARGE,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, new Date());
        assertInvalidTransaction("T001", "A001", BankTransactionType.RECHARGE,
                BigDecimal.ONE, null, BigDecimal.ONE, new Date());
        assertInvalidTransaction("T001", "A001", BankTransactionType.RECHARGE,
                BigDecimal.ONE, BigDecimal.ZERO, null, new Date());
        assertInvalidTransaction("T001", "A001", BankTransactionType.RECHARGE,
                BigDecimal.ONE, BigDecimal.ONE.negate(), BigDecimal.ZERO, new Date());
        assertInvalidTransaction("T001", "A001", BankTransactionType.CONSUMPTION,
                new BigDecimal("2.00"), BigDecimal.ONE, BigDecimal.ONE.negate(), new Date());
    }

    @Test
    void rejectsBalanceChangesInconsistentWithType() {
        assertInvalidTransaction("T001", "A001", BankTransactionType.RECHARGE,
                new BigDecimal("10.00"), new BigDecimal("20.00"),
                new BigDecimal("25.00"), new Date());
        assertInvalidTransaction("T002", "A001", BankTransactionType.CONSUMPTION,
                new BigDecimal("10.00"), new BigDecimal("20.00"),
                new BigDecimal("15.00"), new Date());
    }

    @Test
    void createdAtIsDefensivelyCopied() {
        Date source = new Date(1000L);
        BankTransaction transaction = new BankTransaction("T001", "A001",
                BankTransactionType.RECHARGE, new BigDecimal("20.00"),
                new BigDecimal("10.00"), new BigDecimal("30.00"), null, null, source);

        source.setTime(2000L);
        Date read = transaction.getCreatedAt();
        read.setTime(3000L);

        assertNotSame(source, transaction.getCreatedAt());
        assertEquals(new Date(1000L), transaction.getCreatedAt());
    }

    @Test
    void serializationRoundTripPreservesTransaction() throws Exception {
        BankTransaction original = rechargeTransaction();

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ObjectOutputStream output = new ObjectOutputStream(bytes);
        output.writeObject(original);
        output.flush();
        ObjectInputStream input = new ObjectInputStream(
                new ByteArrayInputStream(bytes.toByteArray()));
        BankTransaction copy = (BankTransaction) input.readObject();

        assertEquals(original.getTransactionId(), copy.getTransactionId());
        assertEquals(original.getAccountId(), copy.getAccountId());
        assertEquals(original.getType(), copy.getType());
        assertEquals(original.getAmount(), copy.getAmount());
        assertEquals(original.getBalanceBefore(), copy.getBalanceBefore());
        assertEquals(original.getBalanceAfter(), copy.getBalanceAfter());
        assertEquals(original.getRelatedOrderId(), copy.getRelatedOrderId());
        assertEquals(original.getDescription(), copy.getDescription());
        assertEquals(original.getCreatedAt(), copy.getCreatedAt());
    }

    private static BankTransaction rechargeTransaction() {
        return new BankTransaction("T001", "A001", BankTransactionType.RECHARGE,
                new BigDecimal("20.00"), new BigDecimal("10.00"),
                new BigDecimal("30.00"), "", "校园卡充值", new Date(1000L));
    }

    private static void assertInvalidTransaction(final String transactionId,
            final String accountId, final BankTransactionType type, final BigDecimal amount,
            final BigDecimal balanceBefore, final BigDecimal balanceAfter,
            final Date createdAt) {
        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() {
                new BankTransaction(transactionId, accountId, type, amount, balanceBefore,
                        balanceAfter, null, null, createdAt);
            }
        });
    }
}
