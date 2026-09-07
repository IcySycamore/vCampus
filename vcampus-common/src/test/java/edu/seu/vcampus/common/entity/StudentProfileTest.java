package edu.seu.vcampus.common.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 学籍记录与学籍状态枚举的单元测试。
 */
class StudentProfileTest {

    /**
     * 构造器应正确保存各字段，且初始未删除。
     */
    @Test
    void constructorStoresFieldsAndDefaultsNotDeleted() {
        Long userId = 1001L;

        StudentProfile record = new StudentProfile(userId, 2026,
                EnrollmentStatus.ENROLLED);

        assertEquals(userId, record.getUserId());
        assertEquals(2026, record.getEnrollYear());
        assertEquals(EnrollmentStatus.ENROLLED, record.getStatus());
        assertFalse(record.isDeleted());
    }

    /**
     * 软删除只置标记，用户 id 与学籍字段应保持可查（避免悬空指针）。
     */
    @Test
    void markDeletedKeepsRecordQueryable() {
        Long userId = 1002L;

        StudentProfile record = new StudentProfile(userId, 2025,
                EnrollmentStatus.GRADUATED);
        record.markDeleted();

        assertTrue(record.isDeleted());
        assertEquals(userId, record.getUserId());
        assertEquals(EnrollmentStatus.GRADUATED, record.getStatus());
    }

    /**
     * 软删除后可以恢复。
     */
    @Test
    void restoreClearsDeletedFlag() {
        StudentProfile record = new StudentProfile(null, 2024,
                EnrollmentStatus.SUSPENDED);
        record.markDeleted();
        record.restore();

        assertFalse(record.isDeleted());
    }

    /**
     * 空构造器应可设置主键 id。
     */
    @Test
    void emptyConstructorAllowsSettingId() {
        StudentProfile record = new StudentProfile();
        assertNull(record.getId());

        record.setId(5L);
        assertEquals(Long.valueOf(5L), record.getId());
    }

    /**
     * 学籍状态枚举应包含在读、休学、退学、毕业四种状态。
     */
    @Test
    void enrollmentStatusHasFourValues() {
        assertEquals(4, EnrollmentStatus.values().length);
        assertEquals(EnrollmentStatus.ENROLLED, EnrollmentStatus.valueOf("ENROLLED"));
        assertEquals(EnrollmentStatus.SUSPENDED, EnrollmentStatus.valueOf("SUSPENDED"));
        assertEquals(EnrollmentStatus.WITHDRAWN, EnrollmentStatus.valueOf("WITHDRAWN"));
        assertEquals(EnrollmentStatus.GRADUATED, EnrollmentStatus.valueOf("GRADUATED"));
    }
}
