package edu.seu.vcampus.server.library;

import java.math.BigDecimal;

/** 图书馆向校园银行发起滞纳金扣款的服务端接口。 */
public interface LibraryFinePayment {
    /**
     * 幂等扣款；相同 reference 的重试必须返回同一流水且不再次扣款。
     * @param userId 用户 UUID
     * @param amount 扣款金额
     * @param reference 图书馆侧唯一业务号
     * @return 银行流水号
     * @throws LibraryException 未开户、余额不足或账户不可用
     */
    String pay(String userId, BigDecimal amount, String reference)
            throws LibraryException;
}
