package edu.seu.vcampus.common.library;

import edu.seu.vcampus.common.user.entity.Capability;
import edu.seu.vcampus.common.user.entity.Permissions;
import edu.seu.vcampus.common.user.entity.Role;

/** 图书馆双端共用的角色解析、借阅额度与馆藏权限。 */
public final class LibraryPolicy {
    private LibraryPolicy() {
    }

    /**
     * 解析中文显示名或英文枚举名，未知角色返回 null。
     * @param value 会话中的角色
     * @return 标准角色
     */
    public static Role role(String value) {
        if (value != null) {
            for (Role role : Role.values()) {
                if (role.getDisplayName().equals(value.trim())
                        || role.name().equalsIgnoreCase(value.trim())) {
                    return role;
                }
            }
        }
        return null;
    }

    /**
     * 返回未归还上限；未知或无借阅权限的角色为零。
     * @param value 会话角色
     * @return 借阅上限
     */
    public static int borrowLimit(String value) {
        Role role = role(value);
        if (!Permissions.can(role, Capability.LIBRARY_BORROW)) {
            return 0;
        }
        return role == Role.STUDENT ? 3 : role == Role.TEACHER ? 5 : 10;
    }

    /**
     * 判断是否有馆藏维护权限。
     * @param value 会话角色
     * @return 是否允许
     */
    public static boolean canManage(String value) {
        return Permissions.can(role(value), Capability.LIBRARY_MANAGE);
    }
}
