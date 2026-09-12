package edu.seu.vcampus.common.student.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 人员类别测试：确认档案只覆盖师生，「不维护管理员信息」有类型层的保证。
 *
 * <p>
 * 这一组用例是组长那条意见的可执行副本——只要有人往 {@link PersonCategory} 里加了 ADMIN，
 * {@link #onlyStudentsAndTeachers()} 立刻变红。
 */
class PersonCategoryTest {

    /**
     * 枚举只有学生与教师两个取值：管理员档案在类型层面就无法表达。
     */
    @Test
    void onlyStudentsAndTeachers() {
        PersonCategory[] all = PersonCategory.values();
        assertEquals(2, all.length);
        assertEquals(PersonCategory.STUDENT, all[0]);
        assertEquals(PersonCategory.TEACHER, all[1]);
    }

    /**
     * 按显示名解析。
     */
    @Test
    void parseByDisplayName() {
        assertEquals(PersonCategory.STUDENT, PersonCategory.fromDisplayName("学生"));
        assertEquals(PersonCategory.TEACHER, PersonCategory.fromDisplayName("教师"));
    }

    /**
     * 「管理员」不是人员类别，解析不出来。
     */
    @Test
    void administratorIsNotAPersonCategory() {
        assertNull(PersonCategory.fromDisplayName("管理员"));
        assertFalse(PersonCategory.requiresProfile("管理员"));
    }

    /**
     * 只有师生需要建人员档案。
     */
    @Test
    void onlyTeachersAndStudentsRequireProfile() {
        assertTrue(PersonCategory.requiresProfile("学生"));
        assertTrue(PersonCategory.requiresProfile("教师"));
        assertFalse(PersonCategory.requiresProfile("管理员"));
        assertFalse(PersonCategory.requiresProfile(null));
        assertFalse(PersonCategory.requiresProfile("不存在的角色"));
    }

    /**
     * 显示名可读且与用户模块的角色名一致。
     */
    @Test
    void displayNames() {
        assertEquals("学生", PersonCategory.STUDENT.getDisplayName());
        assertEquals("教师", PersonCategory.TEACHER.getDisplayName());
    }

    /**
     * 三参构造默认建成学生档案（兼容只关心学籍的旧调用方）。
     */
    @Test
    void defaultCategoryIsStudent() {
        StudentProfile profile = new StudentProfile("uuid-1", 2026, CampusStatus.ENROLLED);

        assertEquals(PersonCategory.STUDENT, profile.getPersonCategory());
    }

    /**
     * 可以显式建一条教师档案。
     */
    @Test
    void canBuildTeacherProfile() {
        StudentProfile teacher = new StudentProfile("uuid-t", PersonCategory.TEACHER, 2020,
                CampusStatus.ENROLLED);

        assertEquals(PersonCategory.TEACHER, teacher.getPersonCategory());
        assertEquals(2020, teacher.getJoinYear());
    }

    /**
     * 类别为 null 时回落成学生，避免出现「类别为空」这种第三种状态。
     */
    @Test
    void nullCategoryFallsBackToStudent() {
        StudentProfile profile = new StudentProfile("uuid-n", null, 2026,
                CampusStatus.ENROLLED);

        assertEquals(PersonCategory.STUDENT, profile.getPersonCategory());
    }

    /**
     * 默认构造的对象也要能安全读类别（不返回 null）。
     */
    @Test
    void emptyProfileStillHasCategory() {
        assertEquals(PersonCategory.STUDENT, new StudentProfile().getPersonCategory());
    }
}
