package edu.seu.vcampus.common.user;

/**
 * 用户角色枚举。
 *
 * <p>统一管理角色显示名（数据库/UI/协议中使用），避免魔法字符串。
 */
public enum Role {
    /** 学生。 */
    STUDENT("学生"),
    /** 教师。 */
    TEACHER("教师"),
    /** 管理员。 */
    ADMIN("管理员");

    /** 显示名。 */
    private final String displayName;

    /**
     * 构造角色。
     *
     * @param displayName 显示名
     */
    Role(String displayName) {
        this.displayName = displayName;
    }

    /** @return 显示名 */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * 按显示名解析角色。
     *
     * @param displayName 显示名（学生/教师/管理员）
     * @return 对应角色；未找到返回 null
     */
    public static Role fromDisplayName(String displayName) {
        for (Role role : values()) {
            if (role.displayName.equals(displayName)) {
                return role;
            }
        }
        return null;
    }
}