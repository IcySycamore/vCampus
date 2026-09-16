package edu.seu.vcampus.server.bank;

import edu.seu.vcampus.common.bank.security.BankPassword;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** 银行密码验证必须先于余额变更，重试不得重复扣款。 */
class BankPasswordTest {
    @Test
    void paymentChecksPasswordAndCannotBeBypassed() {
        final BankService bank = new BankService();
        final char[] password = "bank12345".toCharArray();
        byte[] salt = BankPassword.newSalt();
        bank.openAccount("owner", salt, BankPassword.derive(password, salt));
        bank.recharge("owner", BigDecimal.TEN);
        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override public void execute() {
                bank.consumeWithPassword("owner", "wrong123".toCharArray(),
                        BigDecimal.ONE, "order", "购物");
            }
        });
        assertThrows(IllegalStateException.class, new Executable() {
            @Override public void execute() {
                bank.consume("owner", BigDecimal.ONE, "order", "绕过密码");
            }
        });
        assertEquals(BigDecimal.TEN, bank.queryAccount("owner").getBalance());
        assertEquals(1, bank.listTransactions("owner", null).getTotalCount());
        bank.consumeWithPassword("owner", password, BigDecimal.ONE, "order", "购物");
        bank.consumeWithPassword("owner", password, BigDecimal.ONE, "order", "重试");
        assertEquals(new BigDecimal("9"), bank.queryAccount("owner").getBalance());
        assertEquals(2, bank.listTransactions("owner", null).getTotalCount());
        byte[] otherSalt = BankPassword.newSalt();
        bank.openAccount("owner", otherSalt,
                BankPassword.derive("replacement".toCharArray(), otherSalt));
        bank.consumeWithPassword("owner", password, BigDecimal.ONE, "order2", "原密码仍有效");
        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override public void execute() {
                bank.consumeWithPassword("owner", password, BigDecimal.TEN, "order", "金额改变");
            }
        });
    }
    @Test
    void fiveWrongPasswordsTemporarilyBlockFurtherAttempts() {
        byte[] salt = BankPassword.newSalt();
        final BankCredential credential = new BankCredential(salt,
                BankPassword.derive("bank12345".toCharArray(), salt));
        for (int i = 0; i < 5; i++) {
            assertThrows(IllegalArgumentException.class, new Executable() {
                @Override public void execute() { credential.verify("wrong123".toCharArray()); }
            });
        }
        assertThrows(IllegalStateException.class, new Executable() {
            @Override public void execute() { credential.verify("bank12345".toCharArray()); }
        });
    }

}
