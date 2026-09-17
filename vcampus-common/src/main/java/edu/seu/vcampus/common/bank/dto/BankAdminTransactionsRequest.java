package edu.seu.vcampus.common.bank.dto;

import java.io.Serializable;

/** 管理端按用户名查询指定账户资金流水的请求。 */
public final class BankAdminTransactionsRequest implements Serializable {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 目标用户的登录名。 */
    private final String username;

    /** 类型与分页条件。 */
    private final BankTransactionQueryRequest query;

    /**
     * 构造请求。
     *
     * @param username 目标用户登录名，不能为空
     * @param query 分页与类型条件；null 表示使用默认条件
     */
    public BankAdminTransactionsRequest(String username, BankTransactionQueryRequest query) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        this.username = username;
        this.query = query == null ? new BankTransactionQueryRequest() : query;
    }

    /** @return 目标用户登录名 */
    public String getUsername() {
        return username;
    }

    /** @return 分页与类型条件；永不为 null */
    public BankTransactionQueryRequest getQuery() {
        return query;
    }
}
