package edu.seu.vcampus.common.bank.dto;

import java.io.Serializable;

/** 按用户名指向某个用户银行账户的管理端请求。 */
public final class BankAdminRefRequest implements Serializable {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 目标用户的登录名。 */
    private final String username;

    /**
     * 构造请求。
     *
     * @param username 目标用户登录名，不能为空
     */
    public BankAdminRefRequest(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        this.username = username;
    }

    /** @return 目标用户登录名 */
    public String getUsername() {
        return username;
    }
}
