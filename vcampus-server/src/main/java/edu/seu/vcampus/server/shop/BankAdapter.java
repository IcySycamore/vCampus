package edu.seu.vcampus.server.shop;

import edu.seu.vcampus.server.bank.BankService;
import edu.seu.vcampus.common.bank.entity.BankTransaction;

import java.math.BigDecimal;

/**
 * 银行服务适配器：为shop模块提供基于UUID的银行操作接口。
 *
 * <p>
 * 负责UUID与userId的转换,并封装银行服务的加钱和减钱操作。
 */
public class BankAdapter {

    /** 银行服务实例。 */
    private final BankService bankService;

    /**
     * 使用新造的银行服务构造适配器。
     *
     * <p>
     * <b>只适合不碰支付的测试</b>：这里另造一个账户池，与银行模块的单例不是同一个，支付时 必然报「未开户」。生产装配请用
     * {@link BankAdapter#BankAdapter(BankService)} 传入银行模块的实例。
     */
    public BankAdapter() {
        this.bankService = new BankService();
    }

    /**
     * 使用指定银行服务构造适配器。
     *
     * @param bankService 与银行模块共享的银行服务实例
     */
    public BankAdapter(BankService bankService) {
        this.bankService = bankService;
    }

    /**
     * 从用户银行账户扣款(减钱操作)。
     *
     * @param userUuid 用户UUID
     * @param amount   扣款金额(必须为正数)
     * @param orderId  关联订单ID
     * @param remark   交易备注
     * @return 扣款成功返回交易记录,失败返回null
     */
    public BankTransaction deduct(String userUuid, BigDecimal amount, String orderId,
            String remark) {
        if (userUuid == null || userUuid.trim().isEmpty()) {
            return null;
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        try {
            return bankService.consume(userUuid, amount, orderId, remark);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 向用户银行账户退款(加钱操作)。
     *
     * @param userUuid 用户UUID
     * @param amount   退款金额(必须为正数)
     * @param orderId  关联订单ID
     * @param remark   交易备注
     * @return 退款成功返回交易记录,失败返回null
     */
    public BankTransaction refund(String userUuid, BigDecimal amount, String orderId,
            String remark) {
        if (userUuid == null || userUuid.trim().isEmpty()) {
            return null;
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        try {
            return bankService.cashback(userUuid, amount, orderId, remark);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
