package edu.seu.vcampus.common.bank.dto;

import edu.seu.vcampus.common.bank.entity.BankTransaction;

import java.io.Serializable;

/**
 * 银行账户充值成功响应。
 */
public final class BankRechargeResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private final BankAccountResponse account;
    private final BankTransaction transaction;

    /**
     * 创建充值成功响应。
     *
     * @param account 充值后的账户信息，不能为空
     * @param transaction 本次充值流水，不能为空
     */
    public BankRechargeResponse(BankAccountResponse account,
            BankTransaction transaction) {
        if (account == null) {
            throw new IllegalArgumentException("account must not be null");
        }
        if (transaction == null) {
            throw new IllegalArgumentException("transaction must not be null");
        }
        this.account = account;
        this.transaction = transaction;
    }

    /** @return 充值后的账户信息 */
    public BankAccountResponse getAccount() {
        return account;
    }

    /** @return 本次充值流水 */
    public BankTransaction getTransaction() {
        return transaction;
    }
}
