package edu.seu.vcampus.common.entity;

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
public class Order implements Serializable {

    /** 序列化版本号（协议兼容依据，字段变更时谨慎修改）。 */
    private static final long serialVersionUID = 1L;

    /** 订单ID。 */
    private String oId;

    /** 下单用户的登录ID（关联 tblUser.uId）。 */
    private String oUserId;

    /** 商品ID（关联 tblShopItem.siId）。 */
    private String oItemId;

    /** 购买数量。 */
    private Integer oQuantity;

    /** 订单总价（元）。 */
    private BigDecimal oTotal;

    /** 下单时间。 */
    private Date oTime;

    /** 订单状态：待支付/已支付/已取消。 */
    private String oStatus;

    /**
     * 构造一个空订单。
     */
    public Order() {
    }

    /**
     * 构造一个完整订单。
     *
     * @param oId 订单ID
     * @param oUserId 下单用户的登录ID
     * @param oItemId 商品ID
     * @param oQuantity 购买数量
     * @param oTotal 订单总价
     * @param oTime 下单时间
     * @param oStatus 订单状态
     */
    public Order(String oId, String oUserId, String oItemId, Integer oQuantity,
            BigDecimal oTotal, Date oTime, String oStatus) {
        this.oId = oId;
        this.oUserId = oUserId;
        this.oItemId = oItemId;
        this.oQuantity = oQuantity;
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

    /** @return 下单用户的登录ID */
    public String getoUserId() {
        return oUserId;
    }

    /** @param oUserId 下单用户的登录ID */
    public void setoUserId(String oUserId) {
        this.oUserId = oUserId;
    }

    /** @return 商品ID */
    public String getoItemId() {
        return oItemId;
    }

    /** @param oItemId 商品ID */
    public void setoItemId(String oItemId) {
        this.oItemId = oItemId;
    }

    /** @return 购买数量 */
    public Integer getoQuantity() {
        return oQuantity;
    }

    /** @param oQuantity 购买数量 */
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
    public String getoStatus() {
        return oStatus;
    }

    /** @param oStatus 订单状态 */
    public void setoStatus(String oStatus) {
        this.oStatus = oStatus;
    }
}
