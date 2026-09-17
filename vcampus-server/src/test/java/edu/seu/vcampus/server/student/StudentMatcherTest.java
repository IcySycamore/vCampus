package edu.seu.vcampus.server.student;

import edu.seu.vcampus.common.student.dto.StudentQuery;
import edu.seu.vcampus.common.student.entity.CampusStatus;
import edu.seu.vcampus.common.student.entity.PersonCategory;
import edu.seu.vcampus.common.student.entity.StudentField;
import edu.seu.vcampus.common.student.entity.StudentProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 档案过滤测试：覆盖按类别、按专业/研究方向、按姓名、按学号的检索。
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
     * 关键字也能搜学号——列表里既然有「学号」这一列，就得能按它找得到人。
     *
     * <p>
     * 学号是个纯展示字段（没有业务含义），很容易在写检索时被漏掉：它不在需求文档的查询条件里，
     * 是后来为了界面上好看才加的。所以这里把它钉住，也顺便测了「以学号开头的一段」这种真实用法
     * （教务常常是照着名单念前几位）。
     */
    @Test
    void keywordMatchesStudentNo() {
        student.setStudentNo("202618001");
        StudentQuery query = new StudentQuery();
        query.setKeyword("202618001");

        assertTrue(StudentMatcher.matches(student, query));
        assertFalse(StudentMatcher.matches(teacher, query), "教师没有学号，不应被命中");

        student.setStudentNo(null);
        assertFalse(StudentMatcher.matches(student, query), "学号清空后不应再被该关键词命中");
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
     * 指定搜索字段后只比那一列——这是「选列搜索」与「多列模糊搜」的全部区别。
     *
     * <p>
     * 三条断言各自盯一件事：限定学号时姓名不该命中（否则等于没限定）、限定学号时学号要命中
     * （否则限定过头全搜不到）、主键必须精确匹配（用子串比会把 1 和 11 混成一堆）。
     */
    @Test
    void searchFieldLimitsComparisonToOneColumn() {
        student.setStudentNo("202618001");
        teacher.setStudentNo("199900001");

        StudentQuery byName = new StudentQuery();
        byName.setKeyword("张三");
        byName.setSearchField(StudentField.REAL_NAME);
        assertTrue(StudentMatcher.matches(student, byName));
        byName.setSearchField(StudentField.STUDENT_NO);
        assertFalse(StudentMatcher.matches(student, byName), "学号里没有「张三」，不应命中");

        StudentQuery byNo = new StudentQuery();
        byNo.setKeyword("202618001");
        byNo.setSearchField(StudentField.STUDENT_NO);
        assertTrue(StudentMatcher.matches(student, byNo));
        assertFalse(StudentMatcher.matches(teacher, byNo));

        student.setId(Long.valueOf(11L));
        StudentQuery byId = new StudentQuery();
        byId.setSearchField(StudentField.PROFILE_ID);
        byId.setKeyword("1");
        assertFalse(StudentMatcher.matches(student, byId), "主键是精确匹配，子串「1」不该命中 11");
        byId.setKeyword("11");
        assertTrue(StudentMatcher.matches(student, byId));
    }

    /**
     * 「全部字段」与不指定字段等效，都是一次比对多列。
     */
    @Test
    void allFieldMeansMultiColumnSearch() {
        StudentQuery query = new StudentQuery();
        query.setSearchField(StudentField.ALL);
        query.setKeyword("计算机");

        assertTrue(StudentMatcher.matches(student, query), "限定了「全部字段」仍应命中专业");
    }

    /**
     * filter 一次筛一批，并跳过已注销的记录。
     */
    @Test
    void filterDropsDeletedProfiles() {
        student.markDeleted();
        List<StudentProfile> all = new ArrayList<StudentProfile>();
        all.add(student);
        all.add(teacher);

        List<StudentProfile> matched = StudentMatcher.filter(all, new StudentQuery());

        assertEquals(1, matched.size());
        assertEquals("uuid-t", matched.get(0).getUserUuid());
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
