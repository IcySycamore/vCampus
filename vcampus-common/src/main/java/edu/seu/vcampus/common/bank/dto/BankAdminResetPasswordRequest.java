package edu.seu.vcampus.common.bank.dto;

import java.io.Serializable;

/**
 * 管理端重置他人银行密码的请求。
 *
 * <p>新密码明文留在管理员客户端，只上传盐与加盐摘要，服务端不接触明文，也不需要目标用户的旧密码。</p>
 */
public final class BankAdminResetPasswordRequest implements Serializable {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 目标用户的登录名。 */
    private final String username;

    private final byte[] salt;
    private final byte[] hash;

    /**
     * 构造请求。
     *
     * @param username 目标用户登录名，不能为空
     * @param salt 盐，长度必须为16
     * @param hash 加盐摘要，长度必须为32
     */
    public BankAdminResetPasswordRequest(String username, byte[] salt, byte[] hash) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (salt == null || salt.length != 16 || hash == null || hash.length != 32) {
            throw new IllegalArgumentException("密码参数无效");
        }
        this.username = username;
        this.salt = salt.clone();
        this.hash = hash.clone();
    }

    /** @return 目标用户登录名 */
    public String getUsername() {
        return username;
    }

    /** @return 盐的副本 */
    public byte[] getSalt() {
        return salt.clone();
    }

    /** @return 加盐摘要的副本 */
    public byte[] getHash() {
        return hash.clone();
    }
}
