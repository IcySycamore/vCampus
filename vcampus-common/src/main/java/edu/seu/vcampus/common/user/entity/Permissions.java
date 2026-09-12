package edu.seu.vcampus.common.user.entity;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 能力 × 角色 的权限矩阵：客户端与服务端共用的唯一判定入口。
 *
 * <p>
 * 客户端用它决定控件可见性，服务端用它准入（先 {@code can(...)} 判能力，再按会话把查询范围
 * 收窄到「自己的」）。两侧读同一张表，避免界面显示与后端判定打架。
 *
 * <p>
 * <b>保守原则</b>：未在矩阵中显式授予的能力一律拒绝，因此新增 {@link Capability} 时若忘记登记，
 * 结果是「大家都不能用」而不是「大家都能用」——错误方向安全。
 *
 * <p>
 * 银行的「本人轨」不在矩阵里：任何人（含管理员）都只能查自己的账户与流水，
 * 登录即可，不存在管理能力。
 */
public final class Permissions {

    /** 角色 → 能力集合（构造后只读）。 */
    private static final Map<Role, Set<Capability>> MATRIX = buildMatrix();

    /**
     * 私有构造器，禁止实例化工具类。
     */
    private Permissions() {
    }

    /**
     * 判断角色是否具备某能力。
     *
     * @param role       角色；null 时视为无权限
     * @param capability 能力；null 时视为无权限
     * @return 是否具备
     */
    public static boolean can(Role role, Capability capability) {
        if (role == null || capability == null) {
            return false;
        }
        Set<Capability> granted = MATRIX.get(role);
        return granted != null && granted.contains(capability);
    }

    /**
     * 返回某角色被授予的全部能力（只读）。
     *
     * @param role 角色
     * @return 能力集合；角色为 null 时返回空集合
     */
    public static Set<Capability> of(Role role) {
        Set<Capability> granted = MATRIX.get(role);
        return granted == null ? Collections.<Capability>emptySet() : granted;
    }

    /**
     * 构建权限矩阵（集中一处，便于与设计文档逐条对照）。
     *
     * @return 角色 → 能力集合
     */
    private static Map<Role, Set<Capability>> buildMatrix() {
        Map<Role, Set<Capability>> matrix =
                new EnumMap<Role, Set<Capability>>(Role.class);

        // 学生：选课、借书、下单、提交学籍申请；管理类能力一概没有
        matrix.put(Role.STUDENT, EnumSet.of(
                Capability.STUDENT_MODIFY_APPLY,
                Capability.COURSE_SELECT,
                Capability.LIBRARY_BORROW,
                Capability.SHOP_BUY));

        // 教师：查看学籍与成绩、审核学籍申请、改学籍状态、录成绩；不登记/不注销学籍
        matrix.put(Role.TEACHER, EnumSet.of(
                Capability.STUDENT_VIEW_ALL,
                Capability.STUDENT_MODIFY_AUDIT,
                Capability.STUDENT_CHANGE_STATUS,
                Capability.COURSE_GRADE_VIEW_ALL,
                Capability.COURSE_GRADE_EDIT,
                Capability.LIBRARY_BORROW,
                Capability.SHOP_BUY));

        // 管理员：以上全部（用户管理、学籍登记与注销、课程维护、馆藏维护、商品与订单管理）
        matrix.put(Role.ADMIN, EnumSet.of(
                Capability.USER_MANAGE,
                Capability.STUDENT_VIEW_ALL,
                Capability.STUDENT_MODIFY_AUDIT,
                Capability.STUDENT_REGISTER,
                Capability.STUDENT_DELETE,
                Capability.STUDENT_CHANGE_STATUS,
                Capability.COURSE_GRADE_VIEW_ALL,
                Capability.COURSE_GRADE_EDIT,
                Capability.COURSE_MANAGE,
                Capability.LIBRARY_BORROW,
                Capability.LIBRARY_BORROW_MANAGE,
                Capability.LIBRARY_MANAGE,
                Capability.SHOP_BUY,
                Capability.SHOP_ORDER_MANAGE,
                Capability.SHOP_MANAGE));

        // 冻结为只读，防止调用方拿到内部集合后误改
        Map<Role, Set<Capability>> frozen =
                new EnumMap<Role, Set<Capability>>(Role.class);
        for (Map.Entry<Role, Set<Capability>> entry : matrix.entrySet()) {
            frozen.put(entry.getKey(),
                    Collections.unmodifiableSet(EnumSet.copyOf(entry.getValue())));
        }
        return Collections.unmodifiableMap(frozen);
    }
}
