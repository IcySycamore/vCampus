package edu.seu.vcampus.common.shop.dto;

import java.io.Serializable;

/** 修改待支付订单数量的请求。 */
public final class OrderQuantityUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String orderId;
    private final int quantity;

    /**
     * 创建数量修改请求。
     *
     * @param orderId 订单ID
     * @param quantity 新数量；零表示移除该待支付订单
     */
    public OrderQuantityUpdateRequest(String orderId, int quantity) {
        if (orderId == null || orderId.trim().isEmpty()) {
            throw new IllegalArgumentException("订单ID不能为空");
        }
        if (quantity < 0) {
            throw new IllegalArgumentException("商品数量不能小于零");
        }
        this.orderId = orderId;
        this.quantity = quantity;
    }

    /** @return 订单ID */
    public String getOrderId() {
        return orderId;
    }

    /** @return 新数量 */
    public int getQuantity() {
        return quantity;
    }
}
