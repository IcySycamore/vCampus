package edu.seu.vcampus.common.student.entity;

import java.util.ArrayList;
import java.util.List;

/**
 * 学籍的可检索 / 可排序字段。
 *
 * <p>
 * 界面上「按字段搜索」与「点表头排序」是同一个概念的两面：都是「用户指定了哪一列」。所以本枚举
 * 一份定义同时充当两种角色，客户端直接把它的显示名铺进下拉框、把列头映射回这里的取值，不必
 * 维护「搜索字段表」和「排序字段表」两份常量——两份表迟早会走散。
 *
 * <p>
 * 两个能力是分开的：{@link #ALL} 只能用于搜索（它是「多个字段一起比对」的意思，没有对应的排序
 * 方式），{@link #STATUS} 与 {@link #CATEGORY} 只能用于排序（它们是枚举，用下拉框筛选比在文本框
 * 里手打枚举名合理）。谁可以做什么由 {@link #isSearchable()} / {@link #isSortable()} 回答。
 */
public enum StudentField {

    /** 全部字段：搜索时一次比对多个字段；排序没有这个选项。 */
    ALL("全部字段"),

    /** 学籍记录主键。 */
    PROFILE_ID("主键"),

    /** 学号。 */
    STUDENT_NO("学号"),

    /** 姓名（联查用户模块得到，不落在学籍表里）。 */
    REAL_NAME("姓名"),

    /** 账户 uuid。 */
    USER_UUID("账户"),

    /** 学术方向（学生专业 / 教师研究方向）。 */
    FIELD("专业·研究方向"),

    /** 入校年份。 */
    JOIN_YEAR("入校年份"),

    /** 在校状态。 */
    STATUS("在校状态"),

    /** 人员类别（学生 / 教师）。 */
    CATEGORY("人员类别");

    /** 显示名（界面与协议中使用，避免魔法字符串）。 */
    private final String m_display_name;

    /**
     * 构造字段常量。
     *
     * @param displayName 显示名
     */
    StudentField(String displayName) {
        this.m_display_name = displayName;
    }

    /** @return 显示名 */
    public String getDisplayName() {
        return m_display_name;
    }

    /**
     * 能否作为搜索目标。
     *
     * <p>
     * 主键不作为搜索目标：它是个内部编号，让用户在一个文本框里手打主键，既难记又与「学号」重复。
     * 主键仍然可以用来排序（「最新的在最前」其实就是按主键倒序）。
     *
     * @return 可搜索返回 true
     */
    public boolean isSearchable() {
        return this != PROFILE_ID && this != STATUS && this != CATEGORY;
    }

    /**
     * 能否作为排序字段。
     *
     * @return 可排序返回 true
     */
    public boolean isSortable() {
        return this != ALL;
    }

    /**
     * 列出全部可搜索字段，顺序与 {@link #values()} 一致。
     *
     * @return 可搜索字段数组
     */
    public static StudentField[] searchable() {
        return select(true);
    }

    /**
     * 列出全部可排序字段，顺序与 {@link #values()} 一致。
     *
     * @return 可排序字段数组
     */
    public static StudentField[] sortable() {
        return select(false);
    }

    /**
     * 按显示名解析字段。
     *
     * @param displayName 显示名
     * @return 对应字段；未找到返回 null
     */
    public static StudentField fromDisplayName(String displayName) {
        StudentField[] all = values();
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
    private static StudentField[] select(boolean searchable) {
        List<StudentField> picked = new ArrayList<StudentField>();
        StudentField[] all = values();
        int index = 0;
        while (index < all.length) {
            boolean hit = searchable ? all[index].isSearchable() : all[index].isSortable();
            if (hit) {
                picked.add(all[index]);
            }
            index = index + 1;
        }
        return picked.toArray(new StudentField[picked.size()]);
    }
}
