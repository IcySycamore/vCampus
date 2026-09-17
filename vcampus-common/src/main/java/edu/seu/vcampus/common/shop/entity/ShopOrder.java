package edu.seu.vcampus.common.shop.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 订单实体，对应表 tblOrder。
 *
 * <p>一条订单记录表示某用户购买某商品若干件。下单时由服务器端按"单价 × 数量"
 * 计算总价并落库，客户端不参与金额计算。
 *
 * <p>本类由客户端与服务器端共享，必须实现 {@link java.io.Serializable}（见 ADR-0006）。
 */
public class ShopOrder implements Serializable {

    /** 序列化版本号（协议兼容依据，字段变更时谨慎修改）。 */
    private static final long serialVersionUID = 1L;

    /** 订单ID。 */
    private String oId;

    /** 下单用户的全局 UUID（关联 tblUser.uUuid）。 */
    private String oUserUuid;

    /** 所属商店ID */
    private String oShopId;

    /** 商品ID（关联 tblShopItem.siId）。已废弃，使用 OrderItem 明细表 */
    @Deprecated
    private String oItemId;

    /** 购买数量。已废弃，使用 OrderItem 明细表 */
    @Deprecated
    private Integer oQuantity;

    /** 订单总价（元）。 */
    private BigDecimal oTotal;

    /** 下单时间。 */
    private Date oTime;

    /** 订单状态。 */
    private ShopOrderStatus oStatus;

    /**
     * 构造一个空订单。
     */
    public ShopOrder() {
    }

    /**
     * 构造一个完整订单（兼容旧版）。
     *
     * @param oId 订单ID
     * @param oUserUuid 下单用户的全局 UUID
     * @param oItemId 商品ID（已废弃）
     * @param oQuantity 购买数量（已废弃）
     * @param oTotal 订单总价
     * @param oTime 下单时间
     * @param oStatus 订单状态
     */
    @Deprecated
    public ShopOrder(String oId, String oUserUuid, String oItemId, Integer oQuantity,
            BigDecimal oTotal, Date oTime, ShopOrderStatus oStatus) {
        this.oId = oId;
        this.oUserUuid = oUserUuid;
        this.oItemId = oItemId;
        this.oQuantity = oQuantity;
        this.oTotal = oTotal;
        this.oTime = oTime;
        this.oStatus = oStatus;
    }

    /**
     * 构造一个新版订单（支持多商品）。
     *
     * @param oId 订单ID
     * @param oUserUuid 用户UUID
     * @param oShopId 商店ID
     * @param oTotal 订单总价
     * @param oTime 下单时间
     * @param oStatus 订单状态
     */
    public ShopOrder(String oId, String oUserUuid, String oShopId,
            BigDecimal oTotal, Date oTime, ShopOrderStatus oStatus) {
        this.oId = oId;
        this.oUserUuid = oUserUuid;
        this.oShopId = oShopId;
        this.oTotal = oTotal;
        this.oTime = oTime;
        this.oStatus = oStatus;
    }

    /** @return 订单ID */
    public String getoId() {
        return oId;
    }

    /** @param oId 订单ID */
    public void setoId(String oId) {
        this.oId = oId;
    }

    /** @return 下单用户的全局 UUID */
    public String getoUserUuid() {
        return oUserUuid;
    }

    /** @param oUserUuid 下单用户的全局 UUID */
    public void setoUserUuid(String oUserUuid) {
        this.oUserUuid = oUserUuid;
    }

    /** @return 所属商店ID */
    public String getoShopId() {
        return oShopId;
    }

    /** @param oShopId 所属商店ID */
    public void setoShopId(String oShopId) {
        this.oShopId = oShopId;
    }

    /** @return 商品ID（已废弃） */
    @Deprecated
    public String getoItemId() {
        return oItemId;
    }

    /** @param oItemId 商品ID（已废弃） */
    @Deprecated
    public void setoItemId(String oItemId) {
        this.oItemId = oItemId;
    }

    /** @return 购买数量（已废弃） */
    @Deprecated
    public Integer getoQuantity() {
        return oQuantity;
    }

    /** @param oQuantity 购买数量（已废弃） */
    @Deprecated
    public void setoQuantity(Integer oQuantity) {
        this.oQuantity = oQuantity;
    }

    /** @return 订单总价 */
    public BigDecimal getoTotal() {
        return oTotal;
    }

    /** @param oTotal 订单总价 */
    public void setoTotal(BigDecimal oTotal) {
        this.oTotal = oTotal;
    }

    /** @return 下单时间 */
    public Date getoTime() {
        return oTime;
    }

    /** @param oTime 下单时间 */
    public void setoTime(Date oTime) {
        this.oTime = oTime;
    }

    /** @return 订单状态 */
    public ShopOrderStatus getoStatus() {
        return oStatus;
    }

    /** @param oStatus 订单状态 */
    public void setoStatus(ShopOrderStatus oStatus) {
        this.oStatus = oStatus;
    }
}
