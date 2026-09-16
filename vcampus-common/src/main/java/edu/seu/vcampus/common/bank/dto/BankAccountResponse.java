package edu.seu.vcampus.common.bank.dto;

import edu.seu.vcampus.common.bank.entity.BankAccount;
import edu.seu.vcampus.common.bank.entity.BankAccountStatus;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 返回给客户端的银行账户只读信息。
 *
 * <p>响应不暴露所属用户编号，账户归属由服务端认证会话确定。</p>
 */
public final class BankAccountResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String accountId;
    private final BigDecimal balance;
    private final BankAccountStatus status;
    private final Date createdAt;
    private final Date updatedAt;

    /**
     * 创建账户响应。
     *
     * @param accountId 银行账户编号，不能为空
     * @param balance 当前余额，不能为负
     * @param status 账户状态，不能为空
     * @param createdAt 开户时间，可为空
     * @param updatedAt 最后更新时间，可为空
     */
    public BankAccountResponse(String accountId, BigDecimal balance,
            BankAccountStatus status, Date createdAt, Date updatedAt) {
        this.accountId = requireText(accountId, "accountId");
        if (balance == null || balance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("balance must be non-negative");
        }
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
        this.balance = balance;
        this.status = status;
        this.createdAt = copyDate(createdAt);
        this.updatedAt = copyDate(updatedAt);
    }

    /**
     * 从银行账户实体创建响应。
     *
     * @param account 银行账户实体，不能为空
     * @return 对应的只读账户响应
     */
    public static BankAccountResponse fromAccount(BankAccount account) {
        if (account == null) {
            throw new IllegalArgumentException("account must not be null");
        }
        return new BankAccountResponse(account.getAccountId(), account.getBalance(),
                account.getStatus(), account.getCreatedAt(), account.getUpdatedAt());
    }

    /** @return 银行账户编号 */
    public String getAccountId() {
        return accountId;
    }

    /** @return 当前余额 */
    public BigDecimal getBalance() {
        return balance;
    }

    /** @return 账户状态 */
    public BankAccountStatus getStatus() {
        return status;
    }

    /** @return 开户时间的副本，可能为空 */
    public Date getCreatedAt() {
        return copyDate(createdAt);
    }

    /** @return 最后更新时间的副本，可能为空 */
    public Date getUpdatedAt() {
        return copyDate(updatedAt);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.trim().length() == 0) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    private static Date copyDate(Date date) {
        return date == null ? null : new Date(date.getTime());
    }
}
