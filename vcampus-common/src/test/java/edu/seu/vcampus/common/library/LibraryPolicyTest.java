package edu.seu.vcampus.common.library;

import java.math.BigDecimal;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** 双端共享额度和权限，中文、英文大小写及未知角色均有明确结果。 */
class LibraryPolicyTest {
    @ParameterizedTest
    @CsvSource({"学生,30,false", "student,30,false", "STUDENT,30,false", "Student,30,false",
        "教师,30,false", "teacher,30,false", "TEACHER,30,false", "Teacher,30,false",
        "管理员,0,true", "admin,0,true", "ADMIN,0,true", "other,0,false"})
    void normalizesRolesWithoutGrantingUnknownRoles(String role, int limit, boolean manage) {
        assertEquals(limit, LibraryPolicy.borrowLimit(role));
        assertEquals(manage, LibraryPolicy.canManage(role));
    }

    @Test
    void overdueFineRoundsAnyStartedDayUp() {
        long day = 24L * 60L * 60L * 1000L;
        assertEquals(new BigDecimal("0.00"),
                LibraryPolicy.overdueFine(new Date(day), new Date(day)));
        assertEquals(new BigDecimal("0.10"),
                LibraryPolicy.overdueFine(new Date(day), new Date(day + 1L)));
        assertEquals(new BigDecimal("0.20"),
                LibraryPolicy.overdueFine(new Date(day), new Date(day * 2L + 1L)));
    }
}
