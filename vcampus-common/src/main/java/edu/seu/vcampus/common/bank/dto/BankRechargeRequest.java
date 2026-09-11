package edu.seu.vcampus.common.bank.dto;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 银行账户充值请求。
 *
 * <p>账户归属由服务端根据已认证会话确定，请求中不接受用户或账户编号。</p>
 */
public final class BankRechargeRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private final BigDecimal amount;

    /**
     * 创建充值请求。
     *
     * @param amount 充值金额，必须大于零
     */
    public BankRechargeRequest(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }
        this.amount = amount;
    }

    /** @return 充值金额 */
    public BigDecimal getAmount() {
        return amount;
    }
}
