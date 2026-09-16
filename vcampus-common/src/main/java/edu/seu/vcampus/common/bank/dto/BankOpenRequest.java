package edu.seu.vcampus.common.bank.dto;

import java.io.Serializable;

/** 开户资料；临时校园会话证明身份，不传校园密码和银行明文密码。 */
public final class BankOpenRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String username;
    private final String verificationToken;
    private final byte[] salt;
    private final byte[] hash;
    /**
     * @param username 校园账号
     * @param verificationToken 独立验证会话
     *
     * @param salt 银行盐
     * @param hash 银行密码派生值 */
    public BankOpenRequest(String username, String verificationToken, byte[] salt, byte[] hash) {
        this.username = username;
        this.verificationToken = verificationToken;
        this.salt = salt == null ? null : salt.clone();
        this.hash = hash == null ? null : hash.clone();
    }
    /** @return 校园账号 */
    public String getUsername() { return username; }
    /** @return 验证令牌 */
    public String getVerificationToken() { return verificationToken; }
    /** @return 盐副本 */
    public byte[] getSalt() { return salt == null ? null : salt.clone(); }
    /** @return 摘要副本 */
    public byte[] getHash() { return hash == null ? null : hash.clone(); }
}
