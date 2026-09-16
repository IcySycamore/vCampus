package edu.seu.vcampus.common.student.entity;

import java.util.ArrayList;
import java.util.List;

/**
 * 学籍修改申请单的可检索 / 可排序字段。
 *
 * <p>
 * 与 {@link StudentField} 同一套思路：一份定义同时充当「按字段搜索」的目标与「点表头排序」的
 * 依据，客户端直接把显示名铺进下拉框、把列头映射回这里的取值。
 *
 * <p>
 * 可搜索与可排序的集合是不重合的，这不是抠细节：{@link #CHANGES}（变更内容）与 {@link #REASON}
 * 是自由文本，搜它们很有用（审核人常常只记得「那条申请休学的」），但拿它们排序毫无意义；
 * 反过来 {@link #STATUS} 与 {@link #APPLIED_AT} 适合排序（按状态归类、按时间倒序），却不适合当
 * 搜索目标——状态已经有专门的下拉框，时间则没人会手打一个时间戳。
 */
public enum RequestField {

    /** 全部字段：搜索时一次比对多个字段；排序没有这个选项。 */
    ALL("全部字段"),

    /** 申请单号。 */
    REQUEST_ID("申请单号"),

    /** 目标学籍主键。 */
    PROFILE_ID("学籍主键"),

    /** 申请人账户 uuid。 */
    APPLICANT_UUID("申请人"),

    /** 变更内容（{@code 字段=值} 的编码原文）。 */
    CHANGES("变更内容"),

    /** 申请理由。 */
    REASON("理由"),

    /** 申请单状态。 */
    STATUS("申请状态"),

    /** 申请提交时间。 */
    APPLIED_AT("申请时间");

    /** 显示名（界面与协议中使用，避免魔法字符串）。 */
    private final String m_display_name;

    /**
     * 构造字段常量。
     *
     * @param displayName 显示名
     */
    RequestField(String displayName) {
        this.m_display_name = displayName;
    }

    /** @return 显示名 */
    public String getDisplayName() {
        return m_display_name;
    }

    /**
     * 能否作为搜索目标。
     *
     * @return 可搜索返回 true
     */
    public boolean isSearchable() {
        return this != STATUS && this != APPLIED_AT;
    }

    /**
     * 能否作为排序字段。
     *
     * @return 可排序返回 true
     */
    public boolean isSortable() {
        return this != ALL && this != CHANGES && this != REASON;
    }

    /**
     * 列出全部可搜索字段，顺序与 {@link #values()} 一致。
     *
     * @return 可搜索字段数组
     */
    public static RequestField[] searchable() {
        return select(true);
    }

    /**
     * 列出全部可排序字段，顺序与 {@link #values()} 一致。
     *
     * @return 可排序字段数组
     */
    public static RequestField[] sortable() {
        return select(false);
    }

    /**
     * 按显示名解析字段。
     *
     * @param displayName 显示名
     * @return 对应字段；未找到返回 null
     */
    public static RequestField fromDisplayName(String displayName) {
        RequestField[] all = values();
        int index = 0;
        while (index < all.length) {
            if (all[index].m_display_name.equals(displayName)) {
                return all[index];
            }
            index = index + 1;
        }
        return null;
    }

    /**
     * 按能力筛出字段子集。
     *
     * @param searchable true 取可搜索的，false 取可排序的
     * @return 字段数组
     */
    private static RequestField[] select(boolean searchable) {
        List<RequestField> picked = new ArrayList<RequestField>();
        RequestField[] all = values();
        int index = 0;
        while (index < all.length) {
            boolean hit = searchable ? all[index].isSearchable() : all[index].isSortable();
            if (hit) {
                picked.add(all[index]);
            }
            index = index + 1;
        }
        return picked.toArray(new RequestField[picked.size()]);
    }
}
