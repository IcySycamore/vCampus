package edu.seu.vcampus.common.bank.dto;

import java.io.Serializable;

/** 银行密码修改请求，携带当前密码和新密码摘要。 */
public final class BankPasswordChangeRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String username;
    private final String verificationToken;
    private final char[] currentPassword;
    private final byte[] salt;
    private final byte[] hash;

    public BankPasswordChangeRequest(String username, String verificationToken,
            char[] currentPassword, byte[] salt, byte[] hash) {
        if (currentPassword == null || currentPassword.length == 0
                || salt == null || salt.length != 16 || hash == null || hash.length != 32) {
            throw new IllegalArgumentException("密码参数无效");
        }
        this.username = username;
        this.verificationToken = verificationToken;
        this.currentPassword = currentPassword.clone();
        this.salt = salt.clone();
        this.hash = hash.clone();
    }

    /**
     * 兼容旧的内部调用；服务端会拒绝缺少校园验证 token 的请求。
     *
     * @param currentPassword 当前银行密码
     * @param salt 新银行密码盐
     * @param hash 新银行密码摘要
     */
    public BankPasswordChangeRequest(char[] currentPassword, byte[] salt, byte[] hash) {
        this("", "", currentPassword, salt, hash);
    }

    public String getUsername() {
        return username;
    }

    public String getVerificationToken() {
        return verificationToken;
    }

    public char[] getCurrentPassword() {
        return currentPassword.clone();
    }

    public byte[] getSalt() {
        return salt.clone();
    }

    public byte[] getHash() {
        return hash.clone();
    }
}
