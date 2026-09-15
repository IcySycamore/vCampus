package edu.seu.vcampus.common.user.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Permissions 角色-能力矩阵测试（ADR-0009 D6 / 报告 §2.3）。
 *
 * <p>
 * 缺省语义是拒绝：新增能力若忘记登记，测试应立刻失败。
 */
class PermissionsTest {

    /** 学生只拥有我的轨能力，没有管理能力。 */
    @Test
    void studentGrantsOwnTrackOnly() {
        assertTrue(Permissions.can(Role.STUDENT, Capability.STUDENT_MODIFY_APPLY));
        assertTrue(Permissions.can(Role.STUDENT, Capability.COURSE_SELECT));
        assertTrue(Permissions.can(Role.STUDENT, Capability.LIBRARY_BORROW));
        assertTrue(Permissions.can(Role.STUDENT, Capability.SHOP_BUY));

        assertFalse(Permissions.can(Role.STUDENT, Capability.USER_MANAGE));
        assertFalse(Permissions.can(Role.STUDENT, Capability.STUDENT_VIEW_ALL));
        assertFalse(Permissions.can(Role.STUDENT, Capability.STUDENT_MODIFY_AUDIT));
        assertFalse(Permissions.can(Role.STUDENT, Capability.LIBRARY_BORROW_MANAGE));
    }

    /** 教师可看全部学籍、录入成绩，但不能管理用户与课程。 */
    @Test
    void teacherGrantsTeachingCapabilities() {
        assertTrue(Permissions.can(Role.TEACHER, Capability.STUDENT_VIEW_ALL));
        assertTrue(Permissions.can(Role.TEACHER, Capability.STUDENT_MODIFY_AUDIT));
        assertTrue(Permissions.can(Role.TEACHER, Capability.COURSE_GRADE_EDIT));
        assertTrue(Permissions.can(Role.TEACHER, Capability.COURSE_GRADE_VIEW_ALL));

        assertFalse(Permissions.can(Role.TEACHER, Capability.USER_MANAGE));
        assertFalse(Permissions.can(Role.TEACHER, Capability.COURSE_MANAGE));
        assertFalse(Permissions.can(Role.TEACHER, Capability.STUDENT_REGISTER));
    }

    /** 管理员拥有全部能力（含未来新增的：忘记登记会在此暴露）。 */
    @Test
    void adminGrantsEverything() {
        for (Capability capability : Capability.values()) {
            assertTrue(Permissions.can(Role.ADMIN, capability), "缺少能力: " + capability);
        }
    }

    /** 银行没有管理能力：能力枚举里不应出现 BANK_* 项（账户只服务本人）。 */
    @Test
    void bankHasNoManagementCapability() {
        for (Capability capability : Capability.values()) {
            assertFalse(capability.name().startsWith("BANK"), "银行不应有管理能力: " + capability);
        }
    }

    /** null 一律拒绝。 */
    @Test
    void deniesNullOperands() {
        assertFalse(Permissions.can(null, Capability.USER_MANAGE));
        assertFalse(Permissions.can(Role.ADMIN, null));
        assertFalse(Permissions.can(null, null));
    }
}
