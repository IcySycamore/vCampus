package edu.seu.vcampus.common.shop.entity;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 订单明细实体，对应表 tblOrderItem。
 *
 * <p>本类由客户端与服务器端共享，必须实现 {@link java.io.Serializable}。
 *
 * <p>一个订单可以包含多个商品，每个商品对应一条订单明细记录。
 */
public class OrderItem implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 订单明细ID */
    private String oiId;

    /** 所属订单ID */
    private String oiOrderId;

    /** 商品ID */
    private String oiItemId;

    /** 购买数量 */
    private Integer oiQuantity;

    /** 下单时单价（快照，防止商品改价后历史订单金额错误） */
    private BigDecimal oiPrice;

    /** 小计金额 */
    private BigDecimal oiSubtotal;

    public OrderItem() {
    }

    public OrderItem(String oiId, String oiOrderId, String oiItemId,
                     Integer oiQuantity, BigDecimal oiPrice, BigDecimal oiSubtotal) {
        this.oiId = oiId;
        this.oiOrderId = oiOrderId;
        this.oiItemId = oiItemId;
        this.oiQuantity = oiQuantity;
        this.oiPrice = oiPrice;
        this.oiSubtotal = oiSubtotal;
    }

    public String getOiId() {
        return oiId;
    }

    public void setOiId(String oiId) {
        this.oiId = oiId;
    }

    public String getOiOrderId() {
        return oiOrderId;
    }

    public void setOiOrderId(String oiOrderId) {
        this.oiOrderId = oiOrderId;
    }

    public String getOiItemId() {
        return oiItemId;
    }

    public void setOiItemId(String oiItemId) {
        this.oiItemId = oiItemId;
    }

    public Integer getOiQuantity() {
        return oiQuantity;
    }

    public void setOiQuantity(Integer oiQuantity) {
        this.oiQuantity = oiQuantity;
    }

    public BigDecimal getOiPrice() {
        return oiPrice;
    }

    public void setOiPrice(BigDecimal oiPrice) {
        this.oiPrice = oiPrice;
    }

    public BigDecimal getOiSubtotal() {
        return oiSubtotal;
    }

    public void setOiSubtotal(BigDecimal oiSubtotal) {
        this.oiSubtotal = oiSubtotal;
    }
}
