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
     * 按用户 id 应能查到本人的学籍记录。
     */
    @Test
    void queryByUserIdReturnsOwnProfile() {
        StudentProfile profile = new StudentProfile(5001L, 2026,
                EnrollmentStatus.ENROLLED);
        service.registerStudent(profile);

        StudentProfile found = service.queryByUserId(5001L);
        assertNotNull(found);
        assertEquals(profile.getId(), found.getId());
    }

    /**
     * 按不存在的用户 id 查询返回 null。
     */
    @Test
    void queryByUserIdMissingReturnsNull() {
        assertNull(service.queryByUserId(8888L));
    }

    /**
     * changeStatus 应修改学籍状态并持久化。
     */
    @Test
    void changeStatusUpdatesAndPersists() {
        StudentProfile profile = new StudentProfile(6001L, 2026,
                EnrollmentStatus.ENROLLED);
        service.registerStudent(profile);

        assertTrue(service.changeStatus(profile.getId(), EnrollmentStatus.SUSPENDED));
        assertEquals(EnrollmentStatus.SUSPENDED,
                service.queryProfile(profile.getId()).getStatus());
    }

    /**
     * changeStatus 对不存在的记录应返回 false。
     */
    @Test
    void changeStatusMissingReturnsFalse() {
        assertFalse(service.changeStatus(9999L, EnrollmentStatus.SUSPENDED));
    }

    /**
     * changeStatus 参数为 null 应返回 false。
     */
    @Test
    void changeStatusNullReturnsFalse() {
        assertFalse(service.changeStatus(null, EnrollmentStatus.SUSPENDED));
        assertFalse(service.changeStatus(1L, null));
    }

    /**
     * 空参校验：登记 null 返回 false。
     */
    @Test
    void registerNullReturnsFalse() {
        assertFalse(service.registerStudent(null));
    }
}
