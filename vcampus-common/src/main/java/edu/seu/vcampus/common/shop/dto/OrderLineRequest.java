package edu.seu.vcampus.common.shop.dto;

import java.io.Serializable;

/**
 * 下单请求（单个商品）。
 *
 * <p>包含商品ID和购买数量。
 */
public final class OrderLineRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String itemId;
    private final int quantity;

    /**
     * 创建下单请求。
     *
     * @param itemId 商品ID
     * @param quantity 购买数量，必须大于 0
     */
    public OrderLineRequest(String itemId, int quantity) {
        if (itemId == null || itemId.trim().isEmpty()) {
            throw new IllegalArgumentException("itemId cannot be empty");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be greater than zero");
        }
        this.itemId = itemId;
        this.quantity = quantity;
    }

    /** @return 商品ID */
    public String getItemId() {
        return itemId;
    }

    /** @return 购买数量 */
    public int getQuantity() {
        return quantity;
    }
}
