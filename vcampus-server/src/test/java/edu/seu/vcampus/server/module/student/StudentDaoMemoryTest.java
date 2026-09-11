package edu.seu.vcampus.server.module.student;

import edu.seu.vcampus.common.entity.EnrollmentStatus;
import edu.seu.vcampus.common.entity.StudentProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 学籍内存 DAO 的单元测试：增删改查与软删除语义。
 */
class StudentDaoMemoryTest {

    /** 被测 DAO。 */
    private StudentDaoMemory dao;

    /**
     * 每个测试前重建空 DAO。
     */
    @BeforeEach
    void setUp() {
        dao = new StudentDaoMemory();
    }

    /**
     * insert 后应能按主键查到，且自动分配自增主键。
     */
    @Test
    void insertThenFindById() {
        StudentProfile profile = new StudentProfile("uuid-1001", 2026,
                EnrollmentStatus.ENROLLED);
        assertTrue(dao.insert(profile));
        assertNotNull(profile.getId());

        StudentProfile found = dao.findById(profile.getId());
        assertNotNull(found);
        assertEquals(profile.getUserUuid(), found.getUserUuid());
    }

    /**
     * findByUserUuid 应按用户 uuid 查到对应记录。
     */
    @Test
    void findByUserUuidReturnsMatch() {
        String userUuid = "uuid-2001";
        dao.insert(new StudentProfile(userUuid, 2025, EnrollmentStatus.ENROLLED));

        StudentProfile found = dao.findByUserUuid(userUuid);
        assertNotNull(found);
        assertEquals(userUuid, found.getUserUuid());
    }

    /**
     * findAll 应返回全部未删除记录。
     */
    @Test
    void findAllReturnsAllActive() {
        dao.insert(new StudentProfile("uuid-3001", 2024, EnrollmentStatus.ENROLLED));
        dao.insert(new StudentProfile("uuid-3002", 2025, EnrollmentStatus.SUSPENDED));

        List<StudentProfile> all = dao.findAll();
        assertEquals(2, all.size());
    }

    /**
     * update 应按主键更新记录。
     */
    @Test
    void updateChangesStatus() {
        StudentProfile profile = new StudentProfile("uuid-4001", 2026,
                EnrollmentStatus.ENROLLED);
        dao.insert(profile);

        profile.setStatus(EnrollmentStatus.SUSPENDED);
        assertTrue(dao.update(profile));

        assertEquals(EnrollmentStatus.SUSPENDED,
                dao.findById(profile.getId()).getStatus());
    }

    /**
     * 软删除后记录仍在存储中，但普通查询查不到（避免悬空指针）。
     */
    @Test
    void softDeleteHidesRecord() {
        StudentProfile profile = new StudentProfile("uuid-5001", 2026,
                EnrollmentStatus.GRADUATED);
        dao.insert(profile);

        assertTrue(dao.softDelete(profile.getId()));
        assertNull(dao.findById(profile.getId()));
        assertNull(dao.findByUserUuid(profile.getUserUuid()));
        assertEquals(0, dao.findAll().size());
    }

    /**
     * 删除不存在的记录应返回 false。
     */
    @Test
    void softDeleteMissingReturnsFalse() {
        assertFalse(dao.softDelete(9999L));
    }
}
