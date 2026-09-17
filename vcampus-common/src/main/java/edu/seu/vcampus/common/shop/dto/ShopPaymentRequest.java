package edu.seu.vcampus.common.shop.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 商店订单支付请求。
 *
 * <p>银行密码使用字符数组保存，并在客户端发送完成及服务端处理完成后主动清零。
 */
public final class ShopPaymentRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private final List<String> orderIds;
    private final char[] bankPassword;

    /**
     * 创建支付请求。
     *
     * @param orderId 待支付订单ID
     * @param bankPassword 当前用户的银行密码
     */
    public ShopPaymentRequest(String orderId, char[] bankPassword) {
        this(Collections.singletonList(orderId), bankPassword);
    }

    /**
     * 创建多订单结算请求。
     *
     * @param orderIds 待支付订单ID列表
     * @param bankPassword 当前用户的银行密码
     */
    public ShopPaymentRequest(List<String> orderIds, char[] bankPassword) {
        if (orderIds == null || orderIds.isEmpty()) {
            throw new IllegalArgumentException("至少选择一个待支付订单");
        }
        this.orderIds = new ArrayList<String>();
        for (String orderId : orderIds) {
            if (orderId == null || orderId.trim().isEmpty()) {
                throw new IllegalArgumentException("订单ID不能为空");
            }
            this.orderIds.add(orderId);
        }
        if (bankPassword == null || bankPassword.length == 0) {
            throw new IllegalArgumentException("银行密码不能为空");
        }
        this.bankPassword = bankPassword.clone();
    }

    /** @return 第一个待支付订单ID，供单订单兼容调用使用 */
    public String getOrderId() {
        return orderIds.get(0);
    }

    /** @return 待支付订单ID列表副本 */
    public List<String> getOrderIds() {
        return new ArrayList<String>(orderIds);
    }

    /** @return 银行密码副本，调用方使用后必须清零 */
    public char[] getBankPassword() {
        return bankPassword.clone();
    }

    /** 清零请求对象持有的银行密码。 */
    public void clearBankPassword() {
        Arrays.fill(bankPassword, '\0');
    }
}
