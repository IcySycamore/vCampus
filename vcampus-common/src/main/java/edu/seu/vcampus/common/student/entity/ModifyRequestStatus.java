package edu.seu.vcampus.common.student.entity;

/**
 * 学籍修改申请单的状态。
 *
 * <p>流转：{@link #PENDING} ——（教务审核）——→ {@link #APPROVED} 或 {@link #REJECTED}。
 * 只有 PENDING 的申请单出现在待审列表中；审核完成后不可再次审核。
 *
 * <p>展示名供界面直接使用（如状态徽章文本），避免各页面各写一套中文。
 */
public enum ModifyRequestStatus {

    /** 待审核。 */
    PENDING("待审核"),

    /** 已通过：变更已应用到学籍记录。 */
    APPROVED("已通过"),

    /** 已驳回：学籍记录未改动。 */
    REJECTED("已驳回");

    /** 显示名。 */
    private final String m_display_name;

    /**
     * 构造状态。
     *
     * @param displayName 显示名
     */
    ModifyRequestStatus(String displayName) {
        this.m_display_name = displayName;
    }

    /** @return 显示名 */
    public String getDisplayName() {
        return m_display_name;
    }

    /**
     * 按显示名解析状态。
     *
     * @param displayName 显示名
     * @return 对应状态；未找到返回 null
     */
    public static ModifyRequestStatus fromDisplayName(String displayName) {
        for (ModifyRequestStatus status : values()) {
            if (status.m_display_name.equals(displayName)) {
                return status;
            }
        }
        return null;
    }
}
