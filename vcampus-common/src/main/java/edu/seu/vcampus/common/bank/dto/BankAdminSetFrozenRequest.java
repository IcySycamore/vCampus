package edu.seu.vcampus.common.bank.dto;

import java.io.Serializable;

/** 管理端冻结或解冻指定用户账户的请求，无需目标用户的银行密码。 */
public final class BankAdminSetFrozenRequest implements Serializable {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 目标用户的登录名。 */
    private final String username;

    /** true 表示冻结，false 表示解冻。 */
    private final boolean frozen;

    /**
     * 构造请求。
     *
     * @param username 目标用户登录名，不能为空
     * @param frozen true 冻结、false 解冻
     */
    public BankAdminSetFrozenRequest(String username, boolean frozen) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        this.username = username;
        this.frozen = frozen;
    }

    /** @return 目标用户登录名 */
    public String getUsername() {
        return username;
    }

    /** @return true 表示冻结 */
    public boolean isFrozen() {
        return frozen;
    }
}
