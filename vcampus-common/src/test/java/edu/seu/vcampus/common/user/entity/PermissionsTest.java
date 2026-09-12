package edu.seu.vcampus.common.user.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 权限矩阵测试：逐条验证「谁能做什么」，并覆盖默认拒绝与只读约束。
 *
 * <p>
 * 这些断言同时是文档 §2.3 能力矩阵的可执行副本——矩阵改了，这里必须一起改，否则测试会红。
 */
class PermissionsTest {

    /**
     * 角色为 null 时必须拒绝（默认拒绝原则）。
     */
    @Test
    void nullRoleIsDenied() {
        assertFalse(Permissions.can(null, Capability.STUDENT_VIEW_ALL));
        assertFalse(Permissions.can(null, Capability.STUDENT_MODIFY_APPLY));
    }

    /**
     * 能力为 null 时必须拒绝。
     */
    @Test
    void nullCapabilityIsDenied() {
        assertFalse(Permissions.can(Role.ADMIN, null));
    }

    /**
     * 学生可以提交修改申请，但不能审核。
     */
    @Test
    void studentCanApplyButCannotAudit() {
        assertTrue(Permissions.can(Role.STUDENT, Capability.STUDENT_MODIFY_APPLY));
        assertFalse(Permissions.can(Role.STUDENT, Capability.STUDENT_MODIFY_AUDIT));
    }

    /**
     * 学生没有任何管理类能力（遍历全部管理能力，防止漏配）。
     */
    @Test
    void studentHasNoManagementCapability() {
        Capability[] management = {
            Capability.USER_MANAGE,
            Capability.STUDENT_VIEW_ALL,
            Capability.STUDENT_MODIFY_AUDIT,
            Capability.STUDENT_REGISTER,
            Capability.STUDENT_DELETE,
            Capability.STUDENT_CHANGE_STATUS,
            Capability.COURSE_GRADE_VIEW_ALL,
            Capability.COURSE_GRADE_EDIT,
            Capability.COURSE_MANAGE,
            Capability.LIBRARY_BORROW_MANAGE,
            Capability.LIBRARY_MANAGE,
            Capability.SHOP_ORDER_MANAGE,
            Capability.SHOP_MANAGE
        };
        int index = 0;
        while (index < management.length) {
            assertFalse(Permissions.can(Role.STUDENT, management[index]),
                    "学生不应拥有 " + management[index]);
            index = index + 1;
        }
    }

    /**
     * 教师可以查看全部学籍、审核申请、改学籍状态。
     */
    @Test
    void teacherCanViewAuditAndChangeStatus() {
        assertTrue(Permissions.can(Role.TEACHER, Capability.STUDENT_VIEW_ALL));
        assertTrue(Permissions.can(Role.TEACHER, Capability.STUDENT_MODIFY_AUDIT));
        assertTrue(Permissions.can(Role.TEACHER, Capability.STUDENT_CHANGE_STATUS));
    }

    /**
     * 教师没有最高管理权（用户管理、登记、删除学籍）。
     */
    @Test
    void teacherHasNoTopLevelManagement() {
        assertFalse(Permissions.can(Role.TEACHER, Capability.USER_MANAGE));
        assertFalse(Permissions.can(Role.TEACHER, Capability.STUDENT_REGISTER));
        assertFalse(Permissions.can(Role.TEACHER, Capability.STUDENT_DELETE));
    }

    /**
     * 管理员拥有管理能力，但按设计不参与「学生申请」与「选课」。
     */
    @Test
    void adminHasManagementButNotStudentActions() {
        assertTrue(Permissions.can(Role.ADMIN, Capability.STUDENT_REGISTER));
        assertTrue(Permissions.can(Role.ADMIN, Capability.STUDENT_DELETE));
        assertTrue(Permissions.can(Role.ADMIN, Capability.STUDENT_CHANGE_STATUS));
        assertTrue(Permissions.can(Role.ADMIN, Capability.STUDENT_VIEW_ALL));
        assertFalse(Permissions.can(Role.ADMIN, Capability.STUDENT_MODIFY_APPLY));
        assertFalse(Permissions.can(Role.ADMIN, Capability.COURSE_SELECT));
    }

    /**
     * 三种角色的能力集合都非空，且互不相同（防止矩阵退化成一模一样）。
     */
    @Test
    void ofReturnsDistinctNonEmptySets() {
        Set<Capability> student = Permissions.of(Role.STUDENT);
        Set<Capability> teacher = Permissions.of(Role.TEACHER);
        Set<Capability> admin = Permissions.of(Role.ADMIN);
        assertNotNull(student);
        assertNotNull(teacher);
        assertNotNull(admin);
        assertFalse(student.isEmpty());
        assertFalse(teacher.isEmpty());
        assertFalse(admin.isEmpty());
        assertFalse(student.equals(teacher));
        assertFalse(teacher.equals(admin));
    }

    /**
     * 返回的集合必须不可修改，防止外部代码偷偷扩权。
     */
    @Test
    void ofReturnsUnmodifiableSet() {
        final Set<Capability> capabilities = Permissions.of(Role.STUDENT);
        assertThrows(UnsupportedOperationException.class, new Executable() {
            @Override
            public void execute() {
                capabilities.add(Capability.USER_MANAGE);
            }
        });
    }

    /**
     * 未知角色返回空集合而非 null（调用方可以安全遍历）。
     */
    @Test
    void unknownRoleReturnsEmptySet() {
        assertEquals(0, Permissions.of(null).size());
    }
}
