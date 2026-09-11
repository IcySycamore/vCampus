package edu.seu.vcampus.common.bank.entity;

/**
 * 银行流水类型。
 */
public enum BankTransactionType {

    /** 用户充值。 */
    RECHARGE("充值"),

    /** 商城返现。 */
    CASHBACK("返现"),

    /** 校园消费。 */
    CONSUMPTION("消费");

    private final String displayName;

    /**
     * 创建流水类型。
     *
     * @param displayName 中文显示名
     */
    BankTransactionType(String displayName) {
        this.displayName = displayName;
    }

    /** @return 中文显示名 */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * 按中文显示名解析流水类型。
     *
     * @param displayName 中文显示名
     * @return 对应类型；未找到时返回 null
     */
    public static BankTransactionType fromDisplayName(String displayName) {
        for (BankTransactionType type : values()) {
            if (type.displayName.equals(displayName)) {
                return type;
            }
        }
        return null;
    }
}
