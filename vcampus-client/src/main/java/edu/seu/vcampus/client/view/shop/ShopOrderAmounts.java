package edu.seu.vcampus.client.view.shop;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** 订单数量编辑期间的金额计算。 */
final class ShopOrderAmounts {

    private ShopOrderAmounts() {
    }

    static BigDecimal estimate(BigDecimal previousTotal, int previousQuantity,
            int quantity) {
        if (previousTotal == null || previousQuantity <= 0 || quantity < 0) {
            return null;
        }
        BigDecimal unitPrice = previousTotal.divide(
                BigDecimal.valueOf(previousQuantity), 2, RoundingMode.HALF_UP);
        return unitPrice.multiply(BigDecimal.valueOf(quantity))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
