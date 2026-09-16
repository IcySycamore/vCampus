package edu.seu.vcampus.common.student.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 在校人员档案与在校状态枚举的单元测试。
 */
class StudentProfileTest {

    /**
     * 构造器应正确保存各字段，且初始未删除。
     */
    @Test
    void constructorStoresFieldsAndDefaultsNotDeleted() {
        String userUuid = "uuid-1001";

        StudentProfile record = new StudentProfile(userUuid, 2026, CampusStatus.ENROLLED);

        assertEquals(userUuid, record.getUserUuid());
        assertEquals(2026, record.getJoinYear());
        assertEquals(CampusStatus.ENROLLED, record.getStatus());
        assertFalse(record.isDeleted());
    }

    /**
     * 软删除只置标记，用户 uuid 与学籍字段应保持可查（避免悬空指针）。
     */
    @Test
    void markDeletedKeepsRecordQueryable() {
        String userUuid = "uuid-1002";

        StudentProfile record = new StudentProfile(userUuid, 2025, CampusStatus.GRADUATED);
        record.markDeleted();

        assertTrue(record.isDeleted());
        assertEquals(userUuid, record.getUserUuid());
        assertEquals(CampusStatus.GRADUATED, record.getStatus());
    }

    /**
     * 软删除后可以恢复。
     */
    @Test
    void restoreClearsDeletedFlag() {
        StudentProfile record = new StudentProfile(null, 2024, CampusStatus.SUSPENDED);
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
     * 在校状态应覆盖师生两侧：在学生、暂离、离校、毕业，外加教师退休。
     */
    @Test
    void campusStatusCoversBothStudentsAndTeachers() {
        assertEquals(5, CampusStatus.values().length);
        assertEquals(CampusStatus.ENROLLED, CampusStatus.valueOf("ENROLLED"));
        assertEquals(CampusStatus.SUSPENDED, CampusStatus.valueOf("SUSPENDED"));
        assertEquals(CampusStatus.WITHDRAWN, CampusStatus.valueOf("WITHDRAWN"));
        assertEquals(CampusStatus.GRADUATED, CampusStatus.valueOf("GRADUATED"));
        assertEquals(CampusStatus.RETIRED, CampusStatus.valueOf("RETIRED"));
    }

    /**
     * 显示名取师生都能接受的中性表述，不出现「学籍」字样。
     */
    @Test
    void campusStatusDisplayNames() {
        assertEquals("在校", CampusStatus.ENROLLED.getDisplayName());
        assertEquals("暂离", CampusStatus.SUSPENDED.getDisplayName());
        assertEquals("退休", CampusStatus.RETIRED.getDisplayName());
        assertEquals(CampusStatus.RETIRED, CampusStatus.fromDisplayName("退休"));
        assertNull(CampusStatus.fromDisplayName("不存在的状态"));
    }
}
