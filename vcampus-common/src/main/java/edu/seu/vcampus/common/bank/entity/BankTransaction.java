package edu.seu.vcampus.common.bank.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 一笔已经完成的银行资金流水。
 *
 * <p>流水创建后不可修改，并保存操作前后的余额快照，便于查询和对账。</p>
 */
public final class BankTransaction implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String transactionId;
    private final String accountId;
    private final BankTransactionType type;
    private final BigDecimal amount;
    private final BigDecimal balanceBefore;
    private final BigDecimal balanceAfter;
    private final String relatedOrderId;
    private final String description;
    private final Date createdAt;

    /**
     * 创建一笔银行流水。
     *
     * @param transactionId 流水编号，不能为空
     * @param accountId 账户编号，不能为空
     * @param type 流水类型，不能为空
     * @param amount 变动金额，必须大于零
     * @param balanceBefore 操作前余额，不能为负
     * @param balanceAfter 操作后余额，不能为负且必须与流水类型一致
     * @param relatedOrderId 关联订单编号，可为空
     * @param description 流水说明，可为空
     * @param createdAt 流水创建时间，不能为空
     */
    public BankTransaction(String transactionId, String accountId,
            BankTransactionType type, BigDecimal amount, BigDecimal balanceBefore,
            BigDecimal balanceAfter, String relatedOrderId, String description,
            Date createdAt) {
        this.transactionId = requireText(transactionId, "transactionId");
        this.accountId = requireText(accountId, "accountId");
        this.type = requireType(type);
        this.amount = requirePositive(amount, "amount");
        this.balanceBefore = requireNonNegative(balanceBefore, "balanceBefore");
        this.balanceAfter = requireNonNegative(balanceAfter, "balanceAfter");
        validateBalanceChange();
        this.relatedOrderId = relatedOrderId;
        this.description = description;
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt must not be null");
        }
        this.createdAt = copyDate(createdAt);
    }

    /** @return 流水编号 */
    public String getTransactionId() {
        return transactionId;
    }

    /** @return 账户编号 */
    public String getAccountId() {
        return accountId;
    }

    /** @return 流水类型 */
    public BankTransactionType getType() {
        return type;
    }

    /** @return 变动金额，始终大于零 */
    public BigDecimal getAmount() {
        return amount;
    }

    /** @return 操作前余额 */
    public BigDecimal getBalanceBefore() {
        return balanceBefore;
    }

    /** @return 操作后余额 */
    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    /** @return 关联订单编号，可能为空 */
    public String getRelatedOrderId() {
        return relatedOrderId;
    }

    /** @return 流水说明，可能为空 */
    public String getDescription() {
        return description;
    }

    /** @return 流水创建时间的副本 */
    public Date getCreatedAt() {
        return copyDate(createdAt);
    }

    private void validateBalanceChange() {
        BigDecimal expected;
        if (BankTransactionType.RECHARGE == type || BankTransactionType.CASHBACK == type) {
            expected = balanceBefore.add(amount);
        } else if (BankTransactionType.CONSUMPTION == type) {
            expected = balanceBefore.subtract(amount);
        } else {
            throw new IllegalArgumentException("unsupported transaction type");
        }
        if (expected.compareTo(balanceAfter) != 0) {
            throw new IllegalArgumentException("balance change does not match transaction type");
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.trim().length() == 0) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    private static BankTransactionType requireType(BankTransactionType type) {
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
        return type;
    }

    private static BigDecimal requirePositive(BigDecimal value, String fieldName) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }

    private static BigDecimal requireNonNegative(BigDecimal value, String fieldName) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(fieldName + " must be non-negative");
        }
        return value;
    }

    private static Date copyDate(Date date) {
        return new Date(date.getTime());
    }
}
