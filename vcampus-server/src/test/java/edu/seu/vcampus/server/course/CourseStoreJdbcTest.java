package edu.seu.vcampus.server.course;

import edu.seu.vcampus.common.course.Classroom;
import edu.seu.vcampus.common.course.College;
import edu.seu.vcampus.common.course.CourseSection;
import edu.seu.vcampus.common.course.Field;
import edu.seu.vcampus.common.course.Student;
import edu.seu.vcampus.common.course.Teacher;
import edu.seu.vcampus.common.course.Timeslot;
import edu.seu.vcampus.server.db.DatabaseAvailability;
import edu.seu.vcampus.server.db.DbHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link CourseStoreJdbc} 的真库集成测试。
 *
 * <p>
 * <b>环境门控</b>（见 ADR-0005）：连不上 MySQL 时整体跳过。前置是库中已有 {@code sql/vCampus-extend.sql} 建出的课程相关表。
 *
 * <p>
 * 用例共用同一个 uuid 前缀，跑完就按这个前缀把各表清干净 —— 表间有外键（教师和选课学生指向学院、 选课与课程领域指向课程、选课还指向用户），所以删除顺序不能乱：先子表后主表，用户放最后。
 *
 * <p>
 * {@code uUuid} 是 {@code CHAR(36)}、{@code coId} 是 {@code VARCHAR(16)}、{@code uId} 是
 * {@code VARCHAR(8)}，测试标识都压在这些宽度内。
 */
class CourseStoreJdbcTest {

    /** 全部测试 uuid 共用的前缀，清理时按它批量删。 */
    private static final String UUID_PREFIX = "jdbc-course-it-";

    /** 被测后端。 */
    private CourseStoreJdbc m_store;

    /** 学院 uuid。 */
    private String m_collegeUuid;

    /** 教师 uuid。 */
    private String m_teacherUuid;

    /** 学生 uuid。 */
    private String m_studentUuid;

    /** 教室 uuid。 */
    private String m_classroomUuid;

    /** 课程 uuid。 */
    private String m_courseUuid;

    /** 每个用例前确认数据库可用并生成一套唯一标识。 */
    @BeforeEach
    void setUp() {
        Assumptions.assumeTrue(databaseAvailable(),
                "MySQL 不可用，跳过 JDBC 集成测试（docker compose up -d mysql 后自动执行）");
        long stamp = System.nanoTime();
        m_collegeUuid = uuid(stamp);
        m_teacherUuid = uuid(stamp + 1L);
        m_studentUuid = uuid(stamp + 2L);
        m_classroomUuid = uuid(stamp + 3L);
        m_courseUuid = uuid(stamp + 4L);
        m_store = new CourseStoreJdbc();
        insertUser(m_studentUuid, "X" + Long.toHexString(stamp).substring(0, 6));
    }

    /** 用例后按外键顺序清理本轮数据。 */
    @AfterEach
    void tearDown() {
        String pattern = UUID_PREFIX + "%";
        run("DELETE FROM tblCourseField WHERE coUuid LIKE ?", pattern);
        run("DELETE FROM tblCourseSelection WHERE coUuid LIKE ?", pattern);
        run("DELETE FROM tblTimeslot WHERE tsOwnerUuid LIKE ?", pattern);
        run("DELETE FROM tblCourse WHERE coUuid LIKE ?", pattern);
        run("DELETE FROM tblTeacherField WHERE tcUuid LIKE ?", pattern);
        run("DELETE FROM tblTeacher WHERE tcUuid LIKE ?", pattern);
        run("DELETE FROM tblCourseStudent WHERE uUuid LIKE ?", pattern);
        run("DELETE FROM tblClassroomTag WHERE crUuid LIKE ?", pattern);
        run("DELETE FROM tblClassroom WHERE crUuid LIKE ?", pattern);
        run("DELETE FROM tblCollegeField WHERE clgUuid LIKE ?", pattern);
        run("DELETE FROM tblCollege WHERE clgUuid LIKE ?", pattern);
        run("DELETE FROM tblUser WHERE uUuid LIKE ?", pattern);
    }

    @Test
    void collegeRoundTripKeepsDirectionsAndMajors() {
        College college = new College(m_collegeUuid, "集成测试学院");
        college.setWebsite("https://example.invalid");
        college.setDescription("仅用于测试");
        college.getResearchDirections().add(new Field("人工智能"));
        college.getMajors().add(new Field("软件工程"));
        college.getMajors().add(new Field("信息安全"));
        assertTrue(m_store.saveCollege(college), "写入学院应成功");

        College loaded = findCollege();
        assertNotNull(loaded, "应按 uuid 读回学院");
        assertEquals("集成测试学院", loaded.getName());
        assertEquals("https://example.invalid", loaded.getWebsite());
        assertEquals(1, loaded.getResearchDirections().size(), "研究方向应读回一条");
        assertTrue(loaded.getResearchDirections().contains(new Field("人工智能")));
        assertEquals(2, loaded.getMajors().size(), "专业应读回两条");
        assertTrue(loaded.getMajors().contains(new Field("信息安全")));
    }

    @Test
    void collegeFieldIsReplacedNotAppended() {
        College college = new College(m_collegeUuid, "集成测试学院");
        college.getMajors().add(new Field("软件工程"));
        m_store.saveCollege(college);

        college.getMajors().clear();
        college.getMajors().add(new Field("人工智能"));
        m_store.saveCollege(college);

        College loaded = findCollege();
        assertEquals(1, loaded.getMajors().size(), "覆盖写入后旧专业不应残留");
        assertTrue(loaded.getMajors().contains(new Field("人工智能")));
    }

    @Test
    void teacherRoundTripKeepsDirectionsAndTimeslots() {
        m_store.saveCollege(new College(m_collegeUuid, "集成测试学院"));
        Teacher teacher = teacher();
        teacher.getResearchDirections().add(new Field("计算机网络"));
        teacher.getAvailableTimeslots().add(new Timeslot(1, 480, 600));
        teacher.getPreferenceTimeslots().add(new Timeslot(3, 600, 720));
        assertTrue(m_store.saveTeacher(teacher), "写入教师应成功");

        Teacher loaded = findTeacher();
        assertNotNull(loaded, "应按 uuid 读回教师");
        assertEquals(m_collegeUuid, loaded.getCollegeUuid());
        assertEquals("集成测试研究组", loaded.getResearchGroup());
        assertEquals(1, loaded.getResearchDirections().size());
        assertTrue(loaded.getResearchDirections().contains(new Field("计算机网络")));
        assertEquals(1, loaded.getAvailableTimeslots().size(), "可用时间槽应读回");
        assertEquals(1, loaded.getPreferenceTimeslots().size(), "偏好时间槽应读回");
        assertTrue(loaded.getAvailableTimeslots().contains(new Timeslot(1, 480, 600)));
    }

    @Test
    void deleteTeacherAlsoRemovesFieldsAndTimeslots() {
        m_store.saveCollege(new College(m_collegeUuid, "集成测试学院"));
        Teacher teacher = teacher();
        teacher.getResearchDirections().add(new Field("数据库"));
        teacher.getAvailableTimeslots().add(new Timeslot(2, 480, 600));
        m_store.saveTeacher(teacher);

        assertTrue(m_store.deleteTeacher(m_teacherUuid), "删除应命中");
        assertNull(findTeacher(), "教师应已删除");
        assertEquals(0, countRows("tblTeacherField", "tcUuid", m_teacherUuid),
                "研究方向是子表，必须一并清掉");
        assertEquals(0, countRows("tblTimeslot", "tsOwnerUuid", m_teacherUuid),
                "时间槽也要一并清掉");
        assertFalse(m_store.deleteTeacher(m_teacherUuid), "重复删除不应命中");
    }

    @Test
    void studentAndClassroomRoundTrip() {
        m_store.saveCollege(new College(m_collegeUuid, "集成测试学院"));
        Student student = new Student(m_studentUuid, m_collegeUuid, new Field("人工智能"));
        student.getAvailableTimeslots().add(new Timeslot(5, 480, 600));
        assertTrue(m_store.saveStudent(student), "写入学生应成功");

        Student loadedStudent = findStudent();
        assertNotNull(loadedStudent, "应读回学生档案");
        assertEquals(m_collegeUuid, loadedStudent.getCollegeUuid());
        assertEquals(new Field("人工智能"), loadedStudent.getMajor());
        assertEquals(1, loadedStudent.getAvailableTimeslots().size());

        Classroom classroom = new Classroom(m_classroomUuid, m_collegeUuid, 60, "集成测试楼-101");
        classroom.getTags().add("多媒体");
        classroom.getPreferenceTimeslots().add(new Timeslot(2, 600, 720));
        assertTrue(m_store.saveClassroom(classroom), "写入教室应成功");

        Classroom loadedClassroom = findClassroom();
        assertNotNull(loadedClassroom, "应读回教室");
        assertEquals(60, loadedClassroom.getCapacity());
        assertEquals("集成测试楼-101", loadedClassroom.getLocation());
        assertEquals(1, loadedClassroom.getTags().size(), "标签应读回");
        assertTrue(loadedClassroom.getTags().contains("多媒体"));
        assertEquals(1, loadedClassroom.getPreferenceTimeslots().size());
    }

    @Test
    void courseRoundTripKeepsAllSubTables() {
        m_store.saveCollege(new College(m_collegeUuid, "集成测试学院"));
        m_store.saveTeacher(teacher());

        CourseSection course = new CourseSection("IT101", "集成测试课程", m_collegeUuid, 50);
        course.setUuid(m_courseUuid);
        course.setCredit(3);
        course.setSemester("2026-2027-1");
        course.setPreferredLocation("集成测试楼");
        course.setTeacherUuid(m_teacherUuid);
        course.setClassroomUuid(m_classroomUuid);
        course.getEligibleMajors().add(new Field("人工智能"));
        course.getRequiredDirections().add(new Field("数据库"));
        course.getTimeslots().add(new Timeslot(1, 480, 600));
        course.getStudentUuids().add(m_studentUuid);
        assertTrue(m_store.saveCourse(course), "写入课程应成功");

        CourseSection loaded = findCourse();
        assertNotNull(loaded, "应按 uuid 读回课程");
        assertEquals("IT101", loaded.getCode());
        assertEquals("集成测试课程", loaded.getName());
        assertEquals(3, loaded.getCredit(), "学分在库里是 DECIMAL(3,1)，往返应无损");
        assertEquals(50, loaded.getCapacity());
        assertEquals("2026-2027-1", loaded.getSemester());
        assertEquals("集成测试楼", loaded.getPreferredLocation());
        assertEquals(m_teacherUuid, loaded.getTeacherUuid());
        assertEquals(m_classroomUuid, loaded.getClassroomUuid());
        assertEquals(1, loaded.getEligibleMajors().size(), "可选专业在子表里");
        assertTrue(loaded.getEligibleMajors().contains(new Field("人工智能")));
        assertEquals(1, loaded.getRequiredDirections().size(), "教师方向要求在子表里");
        assertEquals(1, loaded.getTimeslots().size(), "上课时间槽在 tblTimeslot 里");
        assertEquals(1, loaded.getStudentUuids().size(), "已选学生在 tblCourseSelection 里");
        assertEquals(m_studentUuid, loaded.getStudentUuids().iterator().next());
    }

    /**
     * 造一个教师。
     *
     * @return 教师实体
     */
    private Teacher teacher() {
        Teacher teacher = new Teacher(m_teacherUuid, m_collegeUuid);
        teacher.setResearchGroup("集成测试研究组");
        return teacher;
    }

    /**
     * 取出本用例的学院。
     *
     * @return 学院；不存在返回 null
     */
    private College findCollege() {
        for (College item : m_store.loadColleges()) {
            if (m_collegeUuid.equals(item.getUuid())) {
                return item;
            }
        }
        return null;
    }

    /**
     * 取出本用例的教师。
     *
     * @return 教师；不存在返回 null
     */
    private Teacher findTeacher() {
        for (Teacher item : m_store.loadTeachers()) {
            if (m_teacherUuid.equals(item.getUuid())) {
                return item;
            }
        }
        return null;
    }

    /**
     * 取出本用例的学生档案。
     *
     * @return 学生；不存在返回 null
     */
    private Student findStudent() {
        for (Student item : m_store.loadStudents()) {
            if (m_studentUuid.equals(item.getUuid())) {
                return item;
            }
        }
        return null;
    }

    /**
     * 取出本用例的教室。
     *
     * @return 教室；不存在返回 null
     */
    private Classroom findClassroom() {
        for (Classroom item : m_store.loadClassrooms()) {
            if (m_classroomUuid.equals(item.getUuid())) {
                return item;
            }
        }
        return null;
    }

    /**
     * 取出本用例的课程。
     *
     * @return 课程；不存在返回 null
     */
    private CourseSection findCourse() {
        for (CourseSection item : m_store.loadCourses()) {
            if (m_courseUuid.equals(item.getUuid())) {
                return item;
            }
        }
        return null;
    }

    /**
     * 统计某列匹配的行数。
     *
     * @param table  表名
     * @param column 列名
     * @param value  取值
     * @return 行数
     */
    private static int countRows(String table, String column, String value) {
        Connection connection = null;
        PreparedStatement statement = null;
        java.sql.ResultSet rows = null;
        try {
            connection = DbHelper.getConnection();
            statement = connection.prepareStatement(
                    "SELECT COUNT(*) FROM " + table + " WHERE " + column + " = ?");
            statement.setString(1, value);
            rows = statement.executeQuery();
            return rows.next() ? rows.getInt(1) : 0;
        } catch (SQLException e) {
            throw new IllegalStateException("统计行数失败", e);
        } finally {
            close(rows, statement, connection);
        }
    }

    /**
     * 生成恰好 36 位的测试 uuid（{@code uUuid} 等列是 {@code CHAR(36)}）。
     *
     * @param stamp 时间戳
     * @return 补足 36 位的 uuid
     */
    private static String uuid(long stamp) {
        StringBuilder builder = new StringBuilder(UUID_PREFIX);
        builder.append(Long.toHexString(stamp));
        while (builder.length() < 36) {
            builder.append('0');
        }
        return builder.substring(0, 36);
    }

    /**
     * 插入一行临时用户（选课表要指向它）。
     *
     * @param userUuid 用户 uuid
     * @param loginId  登录 ID，最多 8 字符
     */
    private static void insertUser(String userUuid, String loginId) {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = DbHelper.getConnection();
            statement = connection.prepareStatement("INSERT INTO tblUser (uUuid, uId, uName,"
                    + " uPwd, uRole) VALUES (?, ?, ?, ?, ?)");
            statement.setString(1, userUuid);
            statement.setString(2, loginId);
            statement.setString(3, "集成测试");
            statement.setString(4, "x");
            statement.setString(5, "学生");
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("插入测试用户失败", e);
        } finally {
            close(null, statement, connection);
        }
    }

    /**
     * 执行一条带任意参数的语句（用于清理）。
     *
     * @param sql    语句
     * @param params 参数
     */
    private static void run(String sql, String... params) {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = DbHelper.getConnection();
            statement = connection.prepareStatement(sql);
            for (int i = 0; i < params.length; i++) {
                statement.setString(i + 1, params[i]);
            }
            statement.executeUpdate();
        } catch (SQLException e) {
            // 清理失败不影响用例结论
        } catch (RuntimeException e) {
            // 数据库不可用时 setUp 已跳过，这里同样忽略
        } finally {
            close(null, statement, connection);
        }
    }

    /**
     * 探测数据库是否可用。
     *
     * @return 能取到连接返回 true
     */
    private static boolean databaseAvailable() {
        Connection connection = null;
        try {
            connection = DbHelper.getConnection();
            // 连得上不代表建表脚本跑过；缺表时应当整体跳过，而不是抛一堆 Table doesn't exist
            return connection != null && DatabaseAvailability.isReady();
        } catch (SQLException e) {
            return false;
        } catch (RuntimeException e) {
            return false;
        } finally {
            close(null, null, connection);
        }
    }

    /**
     * 安静关闭资源。
     *
     * @param rows       结果集；可为 null
     * @param statement  语句；可为 null
     * @param connection 连接；可为 null
     */
    private static void close(java.sql.ResultSet rows, PreparedStatement statement,
            Connection connection) {
        if (rows != null) {
            try {
                rows.close();
            } catch (SQLException ignored) {
                // 关闭失败无影响
            }
        }
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException ignored) {
                // 关闭失败无影响
            }
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
                // 关闭失败无影响
            }
        }
    }
}
