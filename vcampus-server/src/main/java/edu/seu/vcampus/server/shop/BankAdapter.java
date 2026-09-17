package edu.seu.vcampus.server.shop;

import edu.seu.vcampus.server.bank.BankService;
import edu.seu.vcampus.common.bank.entity.BankTransaction;
import edu.seu.vcampus.common.bank.exception.BankAccountNotOpenedException;
import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.StatusCode;

import java.math.BigDecimal;
import java.util.Arrays;

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
     * 使用指定银行服务构造适配器。
     *
     * <p>
     * 银行服务必须由调用方传入：早先还有一个无参构造器自己 {@code new BankService()} 造一个 新账户池，于是商店在别人的池子里找账户 ——
     * 用户在界面上开的户与扣款时找的户不是同一个， 支付必然失败，而且失败得安静（{@code payOrder} 只返回 false）。
     *
     * @param bankService 与银行模块共享的银行服务实例，不能为 null
     * @throws IllegalArgumentException bankService 为 null
     */
    public BankAdapter(BankService bankService) {
        if (bankService == null) {
            throw new IllegalArgumentException("bankService must not be null");
        }
        this.bankService = bankService;
    }

    /**
     * 从用户银行账户扣款(减钱操作)。
     *
     * @param userUuid 用户UUID
     * @param bankPassword 银行密码
     * @param amount   扣款金额(必须为正数)
     * @param orderId  关联订单ID
     * @param remark   交易备注
     * @return 扣款成功返回交易记录
     * @throws ShopPaymentException 未开户、密码错误、余额不足或账户不可用
     */
    public BankTransaction deduct(String userUuid, char[] bankPassword, BigDecimal amount,
            String orderId, String remark) {
        if (userUuid == null || userUuid.trim().isEmpty()) {
            throw new ShopPaymentException(StatusCode.UNAUTHORIZED, "登录状态已失效");
        }
        if (bankPassword == null || bankPassword.length == 0) {
            throw new ShopPaymentException(StatusCode.BANK_PASSWORD_INVALID, "请输入银行密码");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ShopPaymentException(StatusCode.BAD_REQUEST, "订单金额无效");
        }

        char[] password = bankPassword.clone();
        try {
            return bankService.consumeWithPassword(userUuid, password, amount,
                    orderId, remark);
        } catch (BankAccountNotOpenedException e) {
            throw new ShopPaymentException(Command.BANK_ACCOUNT_NOT_OPENED,
                    "请先开通校园银行账户并充值");
        } catch (IllegalArgumentException e) {
            if ("银行密码错误".equals(e.getMessage())) {
                throw new ShopPaymentException(StatusCode.BANK_PASSWORD_INVALID,
                        "当前银行密码不正确，请重新输入");
            }
            if ("insufficient balance".equals(e.getMessage())) {
                throw new ShopPaymentException(StatusCode.BAD_REQUEST,
                        "银行账户余额不足，请先充值");
            }
            throw new ShopPaymentException(StatusCode.BAD_REQUEST, "支付参数无效");
        } catch (IllegalStateException e) {
            if (e.getMessage() != null && e.getMessage().contains("密码错误次数过多")) {
                throw new ShopPaymentException(StatusCode.BANK_PASSWORD_LOCKED,
                        "银行密码错误次数过多，请一分钟后重试");
            }
            throw new ShopPaymentException(StatusCode.FORBIDDEN, "银行账户当前不可用");
        } finally {
            Arrays.fill(password, '\0');
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
        } catch (RuntimeException e) {
            return null;
        }
    }
}
