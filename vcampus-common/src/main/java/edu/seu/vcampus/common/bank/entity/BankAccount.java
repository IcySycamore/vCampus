package edu.seu.vcampus.common.bank.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 校园用户银行账户实体。
 *
 * <p>本类只保存账户数据并提供账户自身的基础金额和状态校验，不包含数据库、网络或界面逻辑。</p>
 */
public class BankAccount implements Serializable {

    private static final long serialVersionUID = 3L;

    private String accountId;   /* 银行账户编号 */
    private Long userId;        /* 所属用户的稳定主键，对应 User.userId */
    private BigDecimal balance; /* 当前余额 */
    private BankAccountStatus status; /* 账户状态 */
    private Date createdAt;     /* 开户时间 */
    private Date updatedAt;     /* 最后更新时间 */

    /** 创建一个空账户对象，便于序列化框架或 DAO 填充字段。 */
    public BankAccount() {
        this.balance = BigDecimal.ZERO;
        this.status = BankAccountStatus.NORMAL;
    }

    /**
     * 创建账户对象。
     *
     * @param accountId 账户编号
     * @param userId 所属用户编号
     * @param balance 初始余额
     * @param status 账户状态
     * @param createdAt 开户时间
     * @param updatedAt 最后更新时间
     */
    public BankAccount(String accountId, Long userId, BigDecimal balance,
            BankAccountStatus status, Date createdAt, Date updatedAt) {
        setAccountId(accountId);
        setUserId(userId);
        setBalance(balance);
        setStatus(status);
        setCreatedAt(createdAt);
        setUpdatedAt(updatedAt);
    }

    /** @return 账户编号 */
    public String getAccountId() {
        return accountId;
    }

    /** @param accountId 账户编号 */
    public void setAccountId(String accountId) {
        requireText(accountId, "accountId");
        this.accountId = accountId;
    }

    /** @return 所属用户编号 */
    public Long getUserId() {
        return userId;
    }

    /** @param userId 正数用户主键；已绑定的账户不能更换归属 */
    public void setUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId must be positive");
        }
        if (this.userId != null && !this.userId.equals(userId)) {
            throw new IllegalStateException("account owner cannot be changed");
        }
        this.userId = userId;
    }

    /** @return 当前余额 */
    public BigDecimal getBalance() {
        return balance;
    }

    /** @param balance 当前余额，不能为 null 或负数 */
    public void setBalance(BigDecimal balance) {
        if (balance == null || balance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("balance must be non-negative");
        }
        this.balance = balance;
    }

    /** @return 账户状态 */
    public BankAccountStatus getStatus() {
        return status;
    }

    /** @param status 账户状态，不能为 null */
    public void setStatus(BankAccountStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
        this.status = status;
    }

    /** @return 开户时间的副本 */
    public Date getCreatedAt() {
        return copyDate(createdAt);
    }

    /** @param createdAt 开户时间 */
    public void setCreatedAt(Date createdAt) {
        this.createdAt = copyDate(createdAt);
    }

    /** @return 最后更新时间的副本 */
    public Date getUpdatedAt() {
        return copyDate(updatedAt);
    }

    /** @param updatedAt 最后更新时间 */
    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = copyDate(updatedAt);
    }

    /** @return 当前账户是否允许进行资金操作 */
    public boolean isOperational() {
        return BankAccountStatus.NORMAL == status;
    }

    /**
     * 判断金额是否为有效的正数。
     *
     * @param amount 待校验金额
     * @return 金额非 null 且大于零时返回 true
     */
    public boolean isValidAmount(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 判断账户余额是否足够。
     *
     * @param amount 待扣减金额
     * @return 余额不少于金额时返回 true
     */
    public boolean hasSufficientBalance(BigDecimal amount) {
        return isValidAmount(amount) && balance.compareTo(amount) >= 0;
    }

    /**
     * 增加账户余额。
     *
     * @param amount 充值金额，必须大于零
     * @throws IllegalArgumentException 金额无效
     * @throws IllegalStateException 账户不是正常状态
     */
    public void deposit(BigDecimal amount) {
        checkOperation(amount);
        balance = balance.add(amount);
        touchUpdatedAt();
    }

    /**
     * 扣减账户余额。
     *
     * @param amount 扣减金额，必须大于零且不超过余额
     * @throws IllegalArgumentException 金额无效或余额不足
     * @throws IllegalStateException 账户不是正常状态
     */
    public void withdraw(BigDecimal amount) {
        checkOperation(amount);
        if (!hasSufficientBalance(amount)) {
            throw new IllegalArgumentException("insufficient balance");
        }
        balance = balance.subtract(amount);
        touchUpdatedAt();
    }

    private void checkOperation(BigDecimal amount) {
        if (!isValidAmount(amount)) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }
        if (!isOperational()) {
            throw new IllegalStateException("account is not operational");
        }
    }

    private void touchUpdatedAt() {
        updatedAt = new Date();
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.trim().length() == 0) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }

    private static Date copyDate(Date date) {
        return date == null ? null : new Date(date.getTime());
    }
}
