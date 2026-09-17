package edu.seu.vcampus.server.shop;

import edu.seu.vcampus.common.shop.entity.ShopItem;
import edu.seu.vcampus.common.shop.entity.ShopOrder;
import edu.seu.vcampus.common.shop.entity.ShopOrderStatus;
import java.math.BigDecimal;

/** 待支付订单数量更新的内存存储业务。 */
final class ShopOrderQuantityUpdater {

    private ShopOrderQuantityUpdater() {
    }

    static ShopOrder update(ShopDao dao, String orderId, String userUuid, int quantity) {
        if (isBlank(orderId) || isBlank(userUuid) || quantity < 0
                || !(dao instanceof ShopOrderQuantityStore)) {
            return null;
        }
        ShopOrder order = dao.findOrderById(orderId);
        if (order == null || !userUuid.equals(order.getoUserUuid())
                || order.getoStatus() != ShopOrderStatus.UNPAID) {
            return null;
        }
        ShopOrderQuantityStore store = (ShopOrderQuantityStore) dao;
        if (quantity == 0) {
            ShopOrder removed = store.removeUnpaidOrder(orderId);
            if (removed != null) {
                removed.setoQuantity(Integer.valueOf(0));
                removed.setoTotal(BigDecimal.ZERO);
            }
            return removed;
        }

        ShopItem item = dao.findItemById(order.getoItemId());
        if (item == null || item.getSiPrice() == null || item.getSiStock() == null
                || item.getSiStock().intValue() < quantity) {
            return null;
        }
        BigDecimal total = item.getSiPrice().multiply(BigDecimal.valueOf(quantity));
        return store.updateUnpaidOrderQuantity(orderId, quantity, total)
                ? dao.findOrderById(orderId) : null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
