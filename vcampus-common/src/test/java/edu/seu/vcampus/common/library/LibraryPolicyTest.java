package edu.seu.vcampus.common.library;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** 双端共享额度和权限，中文、英文大小写及未知角色均有明确结果。 */
class LibraryPolicyTest {
    @ParameterizedTest
    @CsvSource({"学生,3,false", "student,3,false", "STUDENT,3,false", "Student,3,false",
        "教师,5,false", "teacher,5,false", "TEACHER,5,false", "Teacher,5,false",
        "管理员,10,true", "admin,10,true", "ADMIN,10,true", "other,0,false"})
    void normalizesRolesWithoutGrantingUnknownRoles(String role, int limit, boolean manage) {
        assertEquals(limit, LibraryPolicy.borrowLimit(role));
        assertEquals(manage, LibraryPolicy.canManage(role));
    }
}
