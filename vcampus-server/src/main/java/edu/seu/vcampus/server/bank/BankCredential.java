package edu.seu.vcampus.server.bank;

import edu.seu.vcampus.common.bank.security.BankPassword;
import java.security.MessageDigest;
import java.util.Arrays;

/** 只驻留在银行服务端账户记录中，不进入账户响应。 */
final class BankCredential {
    private int failures;
    private long retryAfter;
    private final byte[] salt;
    private final byte[] hash;

    BankCredential(byte[] salt, byte[] hash) {
        if (salt == null || salt.length != 16 || hash == null || hash.length != 32) {
            throw new IllegalArgumentException("银行密码摘要格式错误");
        }
        this.salt = salt.clone();
        this.hash = hash.clone();
    }

    static BankCredential create(byte[] salt, byte[] hash) {
        return new BankCredential(salt, hash);
    }

    /** @return 盐的副本，供落库使用 */
    byte[] getSalt() {
        return salt.clone();
    }

    /** @return 摘要的副本，供落库使用 */
    byte[] getHash() {
        return hash.clone();
    }

    void verify(char[] password) {
        if (System.nanoTime() < retryAfter) {
            throw new IllegalStateException("银行密码错误次数过多，请一分钟后重试");
        }
        byte[] candidate = BankPassword.derive(password, salt);
        try {
            if (!MessageDigest.isEqual(hash, candidate)) {
                if (++failures >= 5) {
                    retryAfter = System.nanoTime() + 60000000000L;
                    failures = 0;
                }
                throw new IllegalArgumentException("银行密码错误");
            }
            failures = 0;
        } finally {
            Arrays.fill(candidate, (byte) 0);
        }
    }
}
