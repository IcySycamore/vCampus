package edu.seu.vcampus.common.shop.entity;

import java.io.Serializable;

/**
 * 商店实体，对应表 tblShop。
 *
 * <p>本类由客户端与服务器端共享，必须实现 {@link java.io.Serializable}。
 *
 * <p>商店是商品和订单的容器，每个商品属于一个商店。
 */
public class Shop implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 商店ID */
    private String shopId;

    /** 商店名称 */
    private String shopName;

    /** 商店描述 */
    private String shopDescription;

    /** 商店所有者UUID */
    private String shopOwnerUuid;

    /** 是否启用 */
    private Boolean shopEnabled;

    public Shop() {
    }

    public Shop(String shopId, String shopName, String shopDescription,
                String shopOwnerUuid, Boolean shopEnabled) {
        this.shopId = shopId;
        this.shopName = shopName;
        this.shopDescription = shopDescription;
        this.shopOwnerUuid = shopOwnerUuid;
        this.shopEnabled = shopEnabled;
    }

    public String getShopId() {
        return shopId;
    }

    public void setShopId(String shopId) {
        this.shopId = shopId;
    }

    public String getShopName() {
        return shopName;
    }

    public void setShopName(String shopName) {
        this.shopName = shopName;
    }

    public String getShopDescription() {
        return shopDescription;
    }

    public void setShopDescription(String shopDescription) {
        this.shopDescription = shopDescription;
    }

    public String getShopOwnerUuid() {
        return shopOwnerUuid;
    }

    public void setShopOwnerUuid(String shopOwnerUuid) {
        this.shopOwnerUuid = shopOwnerUuid;
    }

    public Boolean getShopEnabled() {
        return shopEnabled;
    }

    public void setShopEnabled(Boolean shopEnabled) {
        this.shopEnabled = shopEnabled;
    }
}
