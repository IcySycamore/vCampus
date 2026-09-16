package edu.seu.vcampus.common.bank.security;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** 银行密码派生；固定参数避免客户端控制计算成本。 */
public final class BankPassword {
    private BankPassword() { }
    /** @return 随机 128 位盐 */
    public static byte[] newSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return salt;
    }
    /**
     * @param password 密码
     * @param salt 盐
     * @return 256 位派生摘要 */
    public static byte[] derive(char[] password, byte[] salt) {
        if (password == null || password.length < 8 || password.length > 64
                || salt == null || salt.length != 16) {
            throw new IllegalArgumentException("银行密码须为 8–64 个字符");
        }
        PBEKeySpec spec = new PBEKeySpec(password, salt, 210000, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("无法派生银行密码", e);
        } finally {
            spec.clearPassword();
        }
    }
}
