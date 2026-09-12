package edu.seu.vcampus.common.student.entity;

/**
 * 人员类别：在册学生与在编教师两类，合称「在校人员」。
 *
 * <p>
 * <b>有意不含管理员</b>：管理员是系统运维角色，不是校园成员，既没有学籍也没有职工档案。
 * 由于本枚举里根本不存在 ADMIN 取值，管理员档案<b>在类型层面就无法表达</b>——不需要在业务
 * 代码里到处写「如果角色是管理员则拒绝」，那种散落的判断正是漏网之鱼的高发地。错误方向由
 * 编译器把关，比运行时校验可靠。
 *
 * <p>
 * 与 {@code common.user.entity.Role} 的关系：Role 是<b>登录权限</b>的划分（含管理员），
 * PersonCategory 是<b>人员档案</b>的划分（只有师生）。两者刻意不合并，否则就得在 Role 里
 * 删掉 ADMIN——那是权限体系需要的取值。
 */
public enum PersonCategory {

    /** 学生（在册）。 */
    STUDENT("学生"),

    /** 教师（在编）。 */
    TEACHER("教师");

    /** 显示名（数据库/界面/协议中使用，避免魔法字符串）。 */
    private final String m_display_name;

    /**
     * 构造人员类别。
     *
     * @param displayName 显示名
     */
    PersonCategory(String displayName) {
        this.m_display_name = displayName;
    }

    /** @return 显示名 */
    public String getDisplayName() {
        return m_display_name;
    }

    /**
     * 按显示名解析类别。
     *
     * @param displayName 显示名
     * @return 对应类别；未找到返回 null
     */
    public static PersonCategory fromDisplayName(String displayName) {
        PersonCategory[] all = values();
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
     * 判断某个角色名下是否应当存在人员档案。
     *
     * <p>
     * 这是给调用方（例如注册流程）用的便捷判断：管理员返回 false，师生返回 true。有了它，
     * 上层就不必自己写「角色等于管理员则跳过建档」这种容易漏的判断。
     *
     * @param roleDisplayName 角色显示名（学生/教师/管理员）
     * @return 应当建档返回 true；管理员或无法识别的角色返回 false
     */
    public static boolean requiresProfile(String roleDisplayName) {
        return fromDisplayName(roleDisplayName) != null;
    }
}
