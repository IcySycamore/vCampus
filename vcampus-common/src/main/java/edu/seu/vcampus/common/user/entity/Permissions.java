package edu.seu.vcampus.common.user.entity;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 角色 → 能力映射表，客户端与服务端共享（见 ADR-0009 D6）。
 *
 * <p>
 * 服务端在命令处理器里用它做准入判定（无权限回 403），客户端用它决定控件可见性。
 * 两者是同一份代码，避免「界面能点但服务端拒绝」与「服务端允许但界面藏起来」的长期漂移。
 *
 * <p>
 * 缺省语义是<b>拒绝</b>：未知角色、未知能力或未登记的映射一律返回 false。
 */
public final class Permissions {

    /** 角色 → 能力集合。 */
    private static final Map<Role, Set<Capability>> GRANTS = new EnumMap<Role, Set<Capability>>(
            Role.class);

    static {
        EnumSet<Capability> student = EnumSet.of(Capability.STUDENT_MODIFY_APPLY,
                Capability.COURSE_SELECT, Capability.LIBRARY_BORROW, Capability.SHOP_BUY);
        EnumSet<Capability> teacher = EnumSet.of(Capability.STUDENT_VIEW_ALL,
                Capability.STUDENT_MODIFY_AUDIT, Capability.STUDENT_CHANGE_STATUS,
                Capability.COURSE_GRADE_VIEW_ALL, Capability.COURSE_GRADE_EDIT,
                Capability.LIBRARY_BORROW, Capability.SHOP_BUY);
        GRANTS.put(Role.STUDENT, student);
        GRANTS.put(Role.TEACHER, teacher);
        GRANTS.put(Role.ADMIN, EnumSet.allOf(Capability.class));
    }

    /** 私有构造器，禁止实例化映射表。 */
    private Permissions() {
    }

    /**
     * 判断角色是否具备某能力。
     *
     * @param role 角色；null 视为无权限
     * @param capability 能力；null 视为无权限
     * @return 是否允许
     */
    public static boolean can(Role role, Capability capability) {
        if (role == null || capability == null) {
            return false;
        }
        Set<Capability> granted = GRANTS.get(role);
        return granted != null && granted.contains(capability);
    }
}
