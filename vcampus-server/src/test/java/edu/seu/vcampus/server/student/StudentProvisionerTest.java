package edu.seu.vcampus.server.student;

import edu.seu.vcampus.common.student.entity.EnrollmentStatus;
import edu.seu.vcampus.common.student.entity.StudentProfile;
import edu.seu.vcampus.common.user.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.Calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * StudentProvisioner 测试：建号即建学籍档案、非学生不建、幂等与撤销。
 */
class StudentProvisionerTest {

    private StudentDaoMemory dao;
    private StudentProvisioner provisioner;

    /**
     * 每个用例使用独立内存 DAO。
     */
    @BeforeEach
    void setUp() {
        dao = new StudentDaoMemory();
        provisioner = new StudentProvisioner(dao);
    }

    /** 学生账号建档案：状态在读、入学年份为当前年份。 */
    @Test
    void provisionsProfileForStudent() {
        provisioner.provision("uuid-1", "张三", Role.STUDENT);

        StudentProfile profile = dao.findByUserUuid("uuid-1");
        assertNotNull(profile);
        assertEquals(EnrollmentStatus.ENROLLED, profile.getStatus());
        assertEquals(Calendar.getInstance().get(Calendar.YEAR), profile.getEnrollYear());
        assertEquals(1, dao.findAll().size());
    }

    /** 教师与管理员没有学籍，不建档；角色无法解析时同样不建。 */
    @Test
    void skipsNonStudents() {
        provisioner.provision("uuid-2", "李老师", Role.TEACHER);
        provisioner.provision("uuid-3", "王管理", Role.ADMIN);
        provisioner.provision("uuid-4", "未知", null);
        provisioner.provision(null, "无 uuid", Role.STUDENT);

        assertNull(dao.findByUserUuid("uuid-2"));
        assertNull(dao.findByUserUuid("uuid-3"));
        assertNull(dao.findByUserUuid("uuid-4"));
        assertEquals(0, dao.findAll().size());
    }

    /** 幂等：重复开户不会产生第二条档案。 */
    @Test
    void isIdempotent() {
        provisioner.provision("uuid-1", "张三", Role.STUDENT);
        provisioner.provision("uuid-1", "张三", Role.STUDENT);

        assertEquals(1, dao.findAll().size());
    }

    /** 撤销：软删除档案，普通查询看不到。 */
    @Test
    void revokesBySoftDelete() {
        provisioner.provision("uuid-1", "张三", Role.STUDENT);

        provisioner.revoke("uuid-1");

        assertNull(dao.findByUserUuid("uuid-1"));
        assertEquals(0, dao.findAll().size());
        provisioner.revoke("uuid-unknown");// 不存在时静默
        provisioner.revoke(null);
    }

    /** DAO 为 null 时构造失败。 */
    @Test
    void rejectsNullDao() {
        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() {
                new StudentProvisioner(null);
            }
        });
    }
}
