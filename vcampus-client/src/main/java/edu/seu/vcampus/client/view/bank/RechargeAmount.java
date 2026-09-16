package edu.seu.vcampus.client.view.bank;

import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.common.constant.StatusCode;
import java.math.BigDecimal;

/** 充值输入转换：金额规则与 Swing 控件分离。 */
final class RechargeAmount {
    private RechargeAmount() {
    }

    static BigDecimal parse(String text) {
        String value = text == null ? "" : text.trim();
        if (!value.matches("[0-9]{1,9}(\\.[0-9]{1,2})?")) {
            throw new ApiException(StatusCode.BAD_REQUEST, "请输入金额，最多 9 位整数、2 位小数");
        }
        BigDecimal amount = new BigDecimal(value);
        if (amount.signum() <= 0) {
            throw new ApiException(StatusCode.BAD_REQUEST, "充值金额必须大于 0 元");
        }
        return amount;
    }
}
