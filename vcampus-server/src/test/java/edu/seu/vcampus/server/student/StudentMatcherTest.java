package edu.seu.vcampus.server.student;

import edu.seu.vcampus.common.student.dto.StudentQuery;
import edu.seu.vcampus.common.student.entity.CampusStatus;
import edu.seu.vcampus.common.student.entity.PersonCategory;
import edu.seu.vcampus.common.student.entity.StudentProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 档案过滤测试：覆盖按类别、按专业/研究方向、按姓名的检索。
 *
 * <p>
 * 核心用例是 {@link #keywordFindsBothSidesByField()}——查「计算机」时同时命中该专业的学生与
 * 该方向的教师。这是「教师的研究方向和学生的专业对应」这条需求在检索层面的落点：两边共用
 * {@code field} 字段，所以一个索引就能把两类人都捞出来。
 */
class StudentMatcherTest {

    /** 学生档案。 */
    private StudentProfile student;

    /** 教师档案。 */
    private StudentProfile teacher;

    /**
     * 预置一条学生与一条教师档案。
     */
    @BeforeEach
    void setUp() {
        student = new StudentProfile("uuid-s", 2026, CampusStatus.ENROLLED);
        student.setRealName("张三");
        student.setField("计算机科学与技术");

        teacher = new StudentProfile("uuid-t", PersonCategory.TEACHER, 2015,
                CampusStatus.ENROLLED);
        teacher.setRealName("李老师");
        teacher.setField("计算机视觉");
    }

    /**
     * 空条件不过滤。
     */
    @Test
    void nullQueryMatchesEverything() {
        assertTrue(StudentMatcher.matches(student, null));
        assertTrue(StudentMatcher.matches(teacher, null));
    }

    /**
     * 按人员类别过滤：只查教师时学生被排除。
     */
    @Test
    void filterByCategory() {
        StudentQuery query = new StudentQuery();
        query.setPersonCategory(PersonCategory.TEACHER);

        assertTrue(StudentMatcher.matches(teacher, query));
        assertFalse(StudentMatcher.matches(student, query));
    }

    /**
     * 按专业/研究方向过滤：同一个字段，两边各自能查到。
     */
    @Test
    void filterByField() {
        StudentQuery query = new StudentQuery();
        query.setField("计算机视觉");

        assertTrue(StudentMatcher.matches(teacher, query));
        assertFalse(StudentMatcher.matches(student, query));
    }

    /**
     * 关键字搜专业时同时命中师生两侧——一个搜索框覆盖两类人。
     */
    @Test
    void keywordFindsBothSidesByField() {
        StudentQuery query = new StudentQuery();
        query.setKeyword("计算机");

        assertTrue(StudentMatcher.matches(student, query), "学生的专业应被命中");
        assertTrue(StudentMatcher.matches(teacher, query), "教师的研究方向应被命中");
    }

    /**
     * 关键字也能搜姓名。
     */
    @Test
    void keywordMatchesRealName() {
        StudentQuery query = new StudentQuery();
        query.setKeyword("张三");

        assertTrue(StudentMatcher.matches(student, query));
        assertFalse(StudentMatcher.matches(teacher, query));
    }

    /**
     * 关键字也能搜入校年份。
     */
    @Test
    void keywordMatchesJoinYear() {
        StudentQuery query = new StudentQuery();
        query.setKeyword("2015");

        assertTrue(StudentMatcher.matches(teacher, query));
        assertFalse(StudentMatcher.matches(student, query));
    }

    /**
     * 关键字为空或纯空白不过滤。
     */
    @Test
    void blankKeywordIgnored() {
        StudentQuery query = new StudentQuery();
        query.setKeyword("   ");

        assertTrue(StudentMatcher.matches(student, query));
    }

    /**
     * 按在校状态过滤。
     */
    @Test
    void filterByStatus() {
        StudentQuery query = new StudentQuery();
        query.setStatus(CampusStatus.GRADUATED);

        assertFalse(StudentMatcher.matches(student, query));
    }

    /**
     * 按主键精确过滤。
     */
    @Test
    void filterByProfileId() {
        student.setId(7L);
        StudentQuery query = StudentQuery.byProfileId(7L);

        assertTrue(StudentMatcher.matches(student, query));
    }

    /**
     * 多个条件是「与」的关系，冲突时应当不匹配。
     */
    @Test
    void conditionsAreConjunctive() {
        StudentQuery query = new StudentQuery();
        query.setPersonCategory(PersonCategory.TEACHER);
        query.setKeyword("张三");

        assertFalse(StudentMatcher.matches(teacher, query));
    }
}
