package edu.seu.vcampus.server.shop;

import edu.seu.vcampus.common.shop.entity.ShopOrder;
import java.math.BigDecimal;

/** 仅由支持购物车数量编辑的 Shop 存储实现提供。 */
interface ShopOrderQuantityStore {

    /**
     * 更新待支付订单的数量和服务端重算金额。
     *
     * @param orderId 订单ID
     * @param quantity 新数量
     * @param total 新总价
     * @return 更新成功返回 true
     */
    boolean updateUnpaidOrderQuantity(String orderId, int quantity, BigDecimal total);

    /**
     * 移除一条待支付订单。
     *
     * @param orderId 订单ID
     * @return 被移除的订单；不存在或不是待支付状态时返回 null
     */
    ShopOrder removeUnpaidOrder(String orderId);
}
