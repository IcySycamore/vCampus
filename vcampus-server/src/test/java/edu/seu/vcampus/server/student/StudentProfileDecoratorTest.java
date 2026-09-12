package edu.seu.vcampus.server.student;

import edu.seu.vcampus.common.student.entity.EnrollmentStatus;
import edu.seu.vcampus.common.student.entity.PersonCategory;
import edu.seu.vcampus.common.student.entity.StudentProfile;
import edu.seu.vcampus.server.user.InMemoryUserRepository;
import edu.seu.vcampus.server.user.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * 姓名联查测试：「用 201 拿到教师和学生姓名」这条需求的直接落点。
 *
 * <p>
 * 学籍表里没有姓名，只有账户 uuid。这一组用例锁住的是：服务端查询时会把姓名补齐，且在任何
 * 一种「补不上」的情况下（没有用户模块、账户已注销、账号本来就没采集姓名）都不抛异常——
 * 列表显示不该因为某一行没有名字就整页失败。
 */
class StudentProfileDecoratorTest {

    /**
     * 有用户模块时按 uuid 把姓名填进档案。
     */
    @Test
    void fillsRealNameFromUserModule() {
        UserRepository users = new InMemoryUserRepository();
        users.save("001", "uuid-1", "salt", "hash", "学生", "张三");
        StudentProfile profile = new StudentProfile("uuid-1", 2026, EnrollmentStatus.ENROLLED);

        StudentProfile result = new StudentProfileDecorator(users).decorate(profile);

        assertSame(profile, result);
        assertEquals("张三", result.getRealName());
        assertEquals("张三", result.getDisplayName());
    }

    /**
     * 教师档案同样能拿到姓名（组长的需求覆盖师生两类，不只是学生）。
     */
    @Test
    void fillsTeacherName() {
        UserRepository users = new InMemoryUserRepository();
        users.save("t01", "uuid-t", "salt", "hash", "教师", "李老师");
        StudentProfile teacher = new StudentProfile("uuid-t", PersonCategory.TEACHER, 2020,
                EnrollmentStatus.ENROLLED);

        new StudentProfileDecorator(users).decorate(teacher);

        assertEquals("李老师", teacher.getRealName());
        assertEquals(PersonCategory.TEACHER, teacher.getPersonCategory());
    }

    /**
     * 没有用户模块（单模块测试）时不填充，也不报错。
     */
    @Test
    void withoutUserModuleKeepsNameNull() {
        StudentProfile profile = new StudentProfile("uuid-1", 2026, EnrollmentStatus.ENROLLED);

        new StudentProfileDecorator(null).decorate(profile);

        assertNull(profile.getRealName());
        assertEquals("uuid-1", profile.getDisplayName());// 回退成 uuid，不是空串
    }

    /**
     * 账户查不到（已注销）时不抛异常，姓名保持 null。
     */
    @Test
    void unknownUuidIsTolerated() {
        StudentProfile profile = new StudentProfile("uuid-ghost", 2026,
                EnrollmentStatus.ENROLLED);

        new StudentProfileDecorator(new InMemoryUserRepository()).decorate(profile);

        assertNull(profile.getRealName());
    }

    /**
     * 管理员不采集姓名：填出来是 null，显示名回退 uuid。
     */
    @Test
    void administratorHasNoName() {
        UserRepository users = new InMemoryUserRepository();
        users.save("003", "uuid-a", "salt", "hash", "管理员", null);
        StudentProfile profile = new StudentProfile("uuid-a", 2026, EnrollmentStatus.ENROLLED);

        new StudentProfileDecorator(users).decorate(profile);

        assertNull(profile.getRealName());
        assertEquals("uuid-a", profile.getDisplayName());
    }

    /**
     * 批量填充：列表里每一条都要补上，不能只补第一条。
     */
    @Test
    void fillsList() {
        UserRepository users = new InMemoryUserRepository();
        users.save("001", "uuid-1", "salt", "hash", "学生", "张三");
        users.save("002", "uuid-2", "salt", "hash", "学生", "李四");
        List<StudentProfile> profiles = new ArrayList<StudentProfile>();
        profiles.add(new StudentProfile("uuid-1", 2026, EnrollmentStatus.ENROLLED));
        profiles.add(new StudentProfile("uuid-2", 2026, EnrollmentStatus.ENROLLED));

        new StudentProfileDecorator(users).decorate(profiles);

        assertEquals("张三", profiles.get(0).getRealName());
        assertEquals("李四", profiles.get(1).getRealName());
    }

    /**
     * 传 null 不炸（查询没命中时链路会传 null 进来）。
     */
    @Test
    void nullInputIsSafe() {
        StudentProfileDecorator decorator =
                new StudentProfileDecorator(new InMemoryUserRepository());

        assertNull(decorator.decorate((StudentProfile) null));
        assertNull(decorator.decorate((List<StudentProfile>) null));
    }

    /**
     * 账户 uuid 为 null 时不去查（ConcurrentHashMap 不接受 null 键）。
     */
    @Test
    void nullUuidIsSafe() {
        StudentProfile profile = new StudentProfile(null, 2026, EnrollmentStatus.ENROLLED);

        new StudentProfileDecorator(new InMemoryUserRepository()).decorate(profile);

        assertNull(profile.getRealName());
        assertEquals("", profile.getDisplayName());
    }
}
