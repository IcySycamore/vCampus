package edu.seu.vcampus.server.student;

import edu.seu.vcampus.common.student.entity.CampusStatus;
import edu.seu.vcampus.common.student.entity.PersonCategory;
import edu.seu.vcampus.common.student.entity.StudentProfile;
import edu.seu.vcampus.common.user.entity.Role;
import edu.seu.vcampus.server.user.UserRepository.Credential;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 启动补建档测试：装配完成前就已存在的账号，也得有在校档案。
 *
 * <p>
 * 这条路径给 {@code data/admins.tsv} 导入的账号兜底——它们建立时各模块的开户钩子还没登记，
 * 于是有姓名、没档案，界面上就是一片「学籍记录不存在」。
 */
class StudentModuleTest {

    @Test
    void provisionsInSchoolAccountsAndSkipsAdmin() {
        StudentDao dao = new StudentDaoMemory();

        int handled = StudentModule.provisionExisting(new StudentProvisioner(dao), accounts());

        assertEquals(3, handled, "三个账号都该过一遍");
        assertNotNull(dao.findByUserUuid("uuid-student"));
        assertNotNull(dao.findByUserUuid("uuid-teacher"));
        assertNull(dao.findByUserUuid("uuid-admin"), "管理员不是校园成员，不建档");
    }

    @Test
    void mapsRoleToCategory() {
        StudentDao dao = new StudentDaoMemory();

        StudentModule.provisionExisting(new StudentProvisioner(dao), accounts());

        StudentProfile student = dao.findByUserUuid("uuid-student");
        StudentProfile teacher = dao.findByUserUuid("uuid-teacher");
        assertEquals(PersonCategory.STUDENT, student.getPersonCategory());
        assertEquals(PersonCategory.TEACHER, teacher.getPersonCategory());
        assertEquals(CampusStatus.ENROLLED, student.getStatus());
        assertTrue(student.getJoinYear() > 2000, "年份取当前年份，不该是 0");
    }

    @Test
    void unknownRoleIsSkipped() {
        StudentDao dao = new StudentDaoMemory();
        List<Credential> accounts = new ArrayList<Credential>();
        accounts.add(new Credential("ghost", "uuid-ghost", "幽灵", "salt", "hash",
                "无此角色", true));

        int handled = StudentModule.provisionExisting(new StudentProvisioner(dao), accounts);

        assertEquals(1, handled, "过了一遍，但不会建档");
        assertNull(dao.findByUserUuid("uuid-ghost"));
    }

    @Test
    void nullArgumentsDoNotThrow() {
        assertEquals(0, StudentModule.provisionExisting(null, accounts()));
        assertEquals(0, StudentModule.provisionExisting(
                new StudentProvisioner(new StudentDaoMemory()), null));
    }

    /**
     * 造三个账号：学生、教师、管理员。角色列存的是显示名，与 users.tsv 保持一致。
     *
     * @return 账号列表
     */
    private static List<Credential> accounts() {
        List<Credential> accounts = new ArrayList<Credential>();
        accounts.add(account("student", "uuid-student", "演示学生", Role.STUDENT));
        accounts.add(account("teacher", "uuid-teacher", "演示教师", Role.TEACHER));
        accounts.add(account("admin", "uuid-admin", "系统管理员", Role.ADMIN));
        return accounts;
    }

    /**
     * 造一个账号。
     *
     * @param userName 登录名
     * @param uuid 账户 uuid
     * @param displayName 姓名
     * @param role 角色
     * @return 账号
     */
    private static Credential account(String userName, String uuid, String displayName, Role role) {
        return new Credential(userName, uuid, displayName, "salt", "hash",
                role.getDisplayName(), true);
    }
}
