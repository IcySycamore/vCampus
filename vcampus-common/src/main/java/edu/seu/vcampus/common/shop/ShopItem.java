package edu.seu.vcampus.common.shop;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 商品实体，对应表 tblShopItem。
 *
 * <p>本类由客户端与服务器端共享：商品列表会放入 {@code Message.data} 传输，
 * 因此必须实现 {@link java.io.Serializable}（见 ADR-0006）。
 *
 * <p>金额使用 {@link java.math.BigDecimal} 表示，避免浮点数精度误差。
 */
public class ShopItem implements Serializable {

    /** 序列化版本号（协议兼容依据，字段变更时谨慎修改）。 */
    private static final long serialVersionUID = 1L;

    /** 商品 UUID，作为内部唯一标识。 */
    private String siUuid;

    /** 商品业务 ID，供业务展示和查询。 */
    private String siId;

    /** 商品名称。 */
    private String siName;

    /** 单价（元）。 */
    private BigDecimal siPrice;

    /** 库存数量。 */
    private Integer siStock;

    /** 商品描述。 */
    private String siDesc;

    /**
     * 构造一个空商品。
     */
    public ShopItem() {
    }

    /**
     * 构造一个带 UUID 的完整商品。
     *
     * @param siUuid 商品 UUID
     * @param siId 商品业务 ID
     * @param siName 商品名称
     * @param siPrice 单价
     * @param siStock 库存数量
     * @param siDesc 商品描述
     */
    public ShopItem(String siUuid, String siId, String siName, BigDecimal siPrice,
            Integer siStock, String siDesc) {
        this.siUuid = siUuid;
        this.siId = siId;
        this.siName = siName;
        this.siPrice = siPrice;
        this.siStock = siStock;
        this.siDesc = siDesc;
    }

    /**
     * 构造一个完整商品。
     *
     * @param siId 商品ID
     * @param siName 商品名称
     * @param siPrice 单价
     * @param siStock 库存数量
     * @param siDesc 商品描述
     */
    public ShopItem(String siId, String siName, BigDecimal siPrice, Integer siStock,
            String siDesc) {
        this.siId = siId;
        this.siName = siName;
        this.siPrice = siPrice;
        this.siStock = siStock;
        this.siDesc = siDesc;
    }

    /** @return 商品 UUID */
    public String getSiUuid() {
        return siUuid;
    }

    /** @param siUuid 商品 UUID */
    public void setSiUuid(String siUuid) {
        this.siUuid = siUuid;
    }

    /** @return 商品ID */
    public String getSiId() {
        return siId;
    }

    /** @param siId 商品ID */
    public void setSiId(String siId) {
        this.siId = siId;
    }

    /** @return 商品名称 */
    public String getSiName() {
        return siName;
    }

    /** @param siName 商品名称 */
    public void setSiName(String siName) {
        this.siName = siName;
    }

    /** @return 单价 */
    public BigDecimal getSiPrice() {
        return siPrice;
    }

    /** @param siPrice 单价 */
    public void setSiPrice(BigDecimal siPrice) {
        this.siPrice = siPrice;
    }

    /** @return 库存数量 */
    public Integer getSiStock() {
        return siStock;
    }

    /** @param siStock 库存数量 */
    public void setSiStock(Integer siStock) {
        this.siStock = siStock;
    }

    /** @return 商品描述 */
    public String getSiDesc() {
        return siDesc;
    }

    /** @param siDesc 商品描述 */
    public void setSiDesc(String siDesc) {
        this.siDesc = siDesc;
    }
}
