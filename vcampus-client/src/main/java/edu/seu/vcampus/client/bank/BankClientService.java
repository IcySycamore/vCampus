package edu.seu.vcampus.client.bank;

import edu.seu.vcampus.common.bank.dto.BankAccountResponse;
import edu.seu.vcampus.common.bank.dto.BankTransactionListResponse;
import edu.seu.vcampus.common.bank.dto.BankTransactionQueryRequest;
import edu.seu.vcampus.common.bank.entity.BankTransaction;

import java.math.BigDecimal;

/**
 * 客户端银行业务接口，供 Bank UI 调用。
 *
 * <p>实现类负责构造统一 {@code Message}、附加当前会话 token、通过
 * {@code ClientSocket} 发送请求，并把服务端响应转换为 Common 中的 DTO。
 * UI 不需要直接依赖命令码、Socket 或服务端 BankService。</p>
 */
public interface BankClientService {

    /**
     * 为当前登录用户开户。
     *
     * @param callback 开户结果回调
     */
    void openAccount(BankClientCallback<BankAccountResponse> callback);

    /**
     * 查询当前登录用户的银行账户。
     *
     * @param callback 查询结果回调
     */
    void queryAccount(BankClientCallback<BankAccountResponse> callback);

    /**
     * 为当前登录用户充值。
     *
     * @param amount 充值金额
     * @param callback 充值结果回调
     */
    void recharge(BigDecimal amount,
            BankClientCallback<BankTransaction> callback);

    /**
     * 查询当前登录用户的资金流水。
     *
     * @param request 分页和交易类型筛选条件；不可为 null 时由实现使用默认条件
     * @param callback 查询结果回调
     */
    void listTransactions(BankTransactionQueryRequest request,
            BankClientCallback<BankTransactionListResponse> callback);

    /**
     * 使用默认分页条件查询流水。
     *
     * @param callback 查询结果回调
     */
    void listTransactions(
            BankClientCallback<BankTransactionListResponse> callback);

}
