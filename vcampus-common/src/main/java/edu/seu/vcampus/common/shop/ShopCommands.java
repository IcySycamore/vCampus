package edu.seu.vcampus.common.shop;

/**
 * 商店模块补充命令。
 *
 * <p>该命令仅服务于 Shop 客户端和服务端，避免改动共享的全局命令定义。
 */
public final class ShopCommands {

    /** 修改当前用户待支付订单的商品数量。 */
    public static final int ORDER_QUANTITY_UPDATE = 511;

    private ShopCommands() {
    }
}
