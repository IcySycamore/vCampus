package edu.seu.vcampus.common.student.entity;

/**
 * 在校状态：描述一名在校人员（学生或教师）当前所处的阶段。
 *
 * <p>
 * <b>为什么叫「在校状态」而不是「学籍状态」</b>：这份档案同时承载学生与教师两类人员（见
 * {@link PersonCategory}），状态字段两边共用。学生侧读作「在读 / 休学 / 退学 / 毕业」，教师侧
 * 读作「在编 / 停职 / 离职 / 退休」——取值是同一套，显示名取两边都能接受的中性表述。
 *
 * <p>
 * 状态与软删除标记（见 {@link StudentProfile#isDeleted()}）是两个正交维度：状态表示人在不在校，
 * 软删除表示这条档案是否已从系统中移除（仅标记，记录仍保留）。
 */
public enum CampusStatus {

    /** 在校（学生在读 / 教师在编）。 */
    ENROLLED("在校"),

    /** 暂离（学生休学 / 教师停职）。 */
    SUSPENDED("暂离"),

    /** 离校（学生退学 / 教师离职）。 */
    WITHDRAWN("离校"),

    /** 毕业。 */
    GRADUATED("毕业"),

    /** 退休（教师）。 */
    RETIRED("退休");

    /** 显示名（数据库/界面/协议中使用，避免魔法字符串）。 */
    private final String m_display_name;

    /**
     * 构造在校状态。
     *
     * @param displayName 显示名
     */
    CampusStatus(String displayName) {
        this.m_display_name = displayName;
    }

    /** @return 显示名 */
    public String getDisplayName() {
        return m_display_name;
    }

    /**
     * 该状态是否意味着人已经不在校（学籍可随之注销）。
     *
     * <p>
     * 判据放在枚举上而不是写在界面里：这是「状态本身含什么语义」的问题，服务端将来做批量
     * 清理时也要用同一份判断，写在界面里就只能被界面用。
     *
     * <p>
     * 「暂离」不算：休学/停职是临时的，人还会回来，档案必须留着；改回「在校」就恢复。
     *
     * @return 离校 / 毕业 / 退休返回 true
     */
    public boolean isDeparted() {
        return this == WITHDRAWN || this == GRADUATED || this == RETIRED;
    }

    /**
     * 按显示名解析状态。
     *
     * @param displayName 显示名
     * @return 对应状态；未找到返回 null
     */
    public static CampusStatus fromDisplayName(String displayName) {
        CampusStatus[] all = values();
        int index = 0;
        while (index < all.length) {
            if (all[index].m_display_name.equals(displayName)) {
                return all[index];
            }
            index = index + 1;
        }
        return null;
    }
}
