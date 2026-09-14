package edu.seu.vcampus.common.bank.entity;

/**
 * 银行账户状态。
 */
public enum BankAccountStatus {

    /** 正常，可以进行资金操作。 */
    NORMAL("正常"),

    /** 冻结，不能进行资金操作。 */
    FROZEN("冻结"),

    /** 已注销，不能进行资金操作。 */
    CLOSED("注销");

    private final String displayName;

    /**
     * 创建账户状态。
     *
     * @param displayName 中文显示名
     */
    BankAccountStatus(String displayName) {
        this.displayName = displayName;
    }

    /** @return 中文显示名 */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * 按中文显示名解析账户状态。
     *
     * @param displayName 中文显示名
     * @return 对应状态；未找到时返回 null
     */
    public static BankAccountStatus fromDisplayName(String displayName) {
        for (BankAccountStatus status : values()) {
            if (status.displayName.equals(displayName)) {
                return status;
            }
        }
        return null;
    }
}
