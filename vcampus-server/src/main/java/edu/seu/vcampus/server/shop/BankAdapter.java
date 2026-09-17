package edu.seu.vcampus.server.shop;

import edu.seu.vcampus.server.bank.BankService;
import edu.seu.vcampus.common.bank.entity.BankTransaction;

import java.math.BigDecimal;

/**
 * 银行服务适配器：为shop模块提供基于UUID的银行操作接口。
 *
 * <p>负责UUID与userId的转换,并封装银行服务的加钱和减钱操作。
 */
public class BankAdapter {

    /** 银行服务实例。 */
    private final BankService bankService;

    /**
     * 使用默认银行服务构造适配器。
     */
    public BankAdapter() {
        this.bankService = new BankService();
    }

    /**
     * 使用指定银行服务构造适配器(用于测试)。
     *
     * @param bankService 银行服务实例
     */
    public BankAdapter(BankService bankService) {
        this.bankService = bankService;
    }

    /**
     * 从用户银行账户扣款(减钱操作)。
     *
     * @param userUuid 用户UUID
     * @param amount 扣款金额(必须为正数)
     * @param orderId 关联订单ID
     * @param remark 交易备注
     * @return 扣款成功返回交易记录,失败返回null
     */
    public BankTransaction deduct(String userUuid, BigDecimal amount, String orderId, String remark) {
        if (userUuid == null || userUuid.trim().isEmpty()) {
            return null;
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        // 将 UUID 转换为 userId（临时方案，待银行模块改为 UUID 后移除）
        Long userId = convertUuidToUserId(userUuid);
        if (userId == null) {
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
     * @param amount 退款金额(必须为正数)
     * @param orderId 关联订单ID
     * @param remark 交易备注
     * @return 退款成功返回交易记录,失败返回null
     */
    public BankTransaction refund(String userUuid, BigDecimal amount, String orderId, String remark) {
        if (userUuid == null || userUuid.trim().isEmpty()) {
            return null;
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        // 将 UUID 转换为 userId（临时方案，待银行模块改为 UUID 后移除）
        Long userId = convertUuidToUserId(userUuid);
        if (userId == null) {
            return null;
        }

        try {
            return bankService.cashback(userUuid, amount, orderId, remark);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 将用户 UUID 转换为数字 ID（临时方案）。
     *
     * <p>注意：这是占位实现，实际需要查询数据库获取映射关系。
     * 待银行模块改为使用 UUID 后可删除此方法。
     *
     * @param userUuid 用户 UUID
     * @return 用户数字 ID，转换失败返回 null
     */
    private Long convertUuidToUserId(String userUuid) {
        // TODO: 从数据库查询 UUID 到 userId 的映射
        // 临时实现：简单的哈希转换（仅用于编译通过，实际不可用）
        try {
            return (long) Math.abs(userUuid.hashCode());
        } catch (Exception e) {
            return null;
        }
    }
}
