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
 * 学籍业务服务的单元测试：基本 CRUD（不含权限）。
 */
class StudentServiceTest {

    /** 被测服务。 */
    private StudentService service;

    /** 底层 DAO（内存实现）。 */
    private StudentDaoMemory dao;

    /**
     * 每个测试前重建服务。
     */
    @BeforeEach
    void setUp() {
        dao = new StudentDaoMemory();
        service = new StudentService(dao);
    }

    /**
     * 登记后可按主键查询。
     */
    @Test
    void registerThenQuery() {
        StudentProfile profile = new StudentProfile(1001L, 2026,
                EnrollmentStatus.ENROLLED);
        assertTrue(service.registerStudent(profile));

        StudentProfile found = service.queryProfile(profile.getId());
        assertNotNull(found);
        assertEquals(profile.getUserId(), found.getUserId());
    }

    /**
     * 列出全部未删除记录。
     */
    @Test
    void listAllProfiles() {
        service.registerStudent(new StudentProfile(2001L, 2024,
                EnrollmentStatus.ENROLLED));
        service.registerStudent(new StudentProfile(2002L, 2025,
                EnrollmentStatus.ENROLLED));

        List<StudentProfile> all = service.listAllProfiles();
        assertEquals(2, all.size());
    }

    /**
     * 更新应生效。
     */
    @Test
    void updateProfileChangesStatus() {
        StudentProfile profile = new StudentProfile(3001L, 2026,
                EnrollmentStatus.ENROLLED);
        service.registerStudent(profile);

        profile.setStatus(EnrollmentStatus.SUSPENDED);
        assertTrue(service.updateProfile(profile));

        assertEquals(EnrollmentStatus.SUSPENDED,
                service.queryProfile(profile.getId()).getStatus());
    }

    /**
     * 软删除后查询返回 null。
     */
    @Test
    void deleteStudentHidesProfile() {
        StudentProfile profile = new StudentProfile(4001L, 2026,
                EnrollmentStatus.GRADUATED);
        service.registerStudent(profile);

        assertTrue(service.deleteStudent(profile.getId()));
        assertNull(service.queryProfile(profile.getId()));
    }

    /**
     * 查询不存在的记录返回 null。
     */
    @Test
    void queryMissingReturnsNull() {
        assertNull(service.queryProfile(9999L));
    }

    /**
     * 空参校验：登记 null 返回 false。
     */
    @Test
    void registerNullReturnsFalse() {
        assertFalse(service.registerStudent(null));
    }
}
