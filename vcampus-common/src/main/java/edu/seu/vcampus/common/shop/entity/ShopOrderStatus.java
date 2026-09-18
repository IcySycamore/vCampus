package edu.seu.vcampus.common.shop.entity;

/**
 * 商店订单状态。
 */
public enum ShopOrderStatus {

    /** 待支付。 */
    UNPAID("待支付"),

    /** 已支付。 */
    PAID("已支付"),

    /** 已发货。 */
    SHIPPED("已发货"),

    /** 已取消。 */
    CANCELLED("已取消"),

    /** 已完成。 */
    COMPLETED("已完成");

    private final String displayName;

    /**
     * 创建订单状态。
     *
     * @param displayName 中文显示名
     */
    ShopOrderStatus(String displayName) {
        this.displayName = displayName;
    }

    /** @return 中文显示名 */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * 按中文显示名解析订单状态。
     *
     * @param displayName 中文显示名
     * @return 对应状态；未找到时返回 null
     */
    public static ShopOrderStatus fromDisplayName(String displayName) {
        for (ShopOrderStatus status : values()) {
            if (status.displayName.equals(displayName)) {
                return status;
            }
        }
        return null;
    }
}
