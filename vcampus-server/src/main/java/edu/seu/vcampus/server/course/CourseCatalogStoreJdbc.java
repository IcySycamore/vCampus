package edu.seu.vcampus.server.course;

import edu.seu.vcampus.common.course.Classroom;
import edu.seu.vcampus.common.course.College;
import edu.seu.vcampus.common.course.Field;
import edu.seu.vcampus.common.course.Student;
import edu.seu.vcampus.common.course.Teacher;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static edu.seu.vcampus.server.db.JdbcSupport.blank;
import static edu.seu.vcampus.server.db.JdbcSupport.closeQuietly;
import static edu.seu.vcampus.server.db.JdbcSupport.setNullable;

/**
 * 学院、教师、选课学生、教室四类实体在库里的读写。
 *
 * <p>
 * 从 {@link CourseStoreJdbc} 拆出来，与 {@link CourseSectionStoreJdbc} 分工：那边管课程本体，
 * 这边管课程周围的组织结构。四类实体的读写形状相近（一张主表 + 一到两张子表 + 时间槽），放在 一起也便于共用
 * {@link edu.seu.vcampus.server.db.JdbcSupport} 里的参数绑定与资源关闭。
 *
 * <p>
 * <b>不落库的两个反向索引</b>：{@code College.teacherUuids}、{@code Teacher.claimedCourseUuids} 与
 * {@code Student.selectedCourseUuids} 都是关系的反查视角，库里分别由
 * {@code tblTeacher.tcCollegeUuid}、{@code tblCourse.coTeacherUuid}、 {@code tblCourseSelection}
 * 表达；这里不写也不读它们，改由 {@code CourseDao} 在恢复时统一重建， 免得同一关系存两份、日后互相矛盾。
 */
final class CourseCatalogStoreJdbc {

    /** 学院研究方向与专业的 kind 取值。 */
    private static final String KIND_DIRECTION = "DIRECTION";

    /** 学院专业的 kind 取值。 */
    private static final String KIND_MAJOR = "MAJOR";

    /** 教师可用时间槽的归属类型。 */
    private static final String SLOT_TEACHER_AVAILABLE = "TEACHER_AVAILABLE";

    /** 教师偏好时间槽的归属类型。 */
    private static final String SLOT_TEACHER_PREFERENCE = "TEACHER_PREFERENCE";

    /** 学生可用时间槽的归属类型。 */
    private static final String SLOT_STUDENT_AVAILABLE = "STUDENT_AVAILABLE";

    /** 学生偏好时间槽的归属类型。 */
    private static final String SLOT_STUDENT_PREFERENCE = "STUDENT_PREFERENCE";

    /** 教室可用时间槽的归属类型。 */
    private static final String SLOT_CLASSROOM_AVAILABLE = "CLASSROOM_AVAILABLE";

    /** 教室偏好时间槽的归属类型。 */
    private static final String SLOT_CLASSROOM_PREFERENCE = "CLASSROOM_PREFERENCE";

    private CourseCatalogStoreJdbc() {
    }

    /**
     * 加载全部学院（含研究方向与专业；不含教师反向索引）。
     *
     * @param connection 事务连接
     * @return 学院列表，不返回 null
     * @throws SQLException 查询失败
     */
    static List<College> loadColleges(Connection connection) throws SQLException {
        List<College> found = new ArrayList<College>();
        PreparedStatement statement = null;
        ResultSet rows = null;
        try {
            statement = connection.prepareStatement("SELECT clgUuid, clgName, clgWebsite,"
                    + " clgDescription FROM tblCollege ORDER BY clgName ASC");
            rows = statement.executeQuery();
            while (rows.next()) {
                College college = new College(rows.getString("clgUuid"), rows.getString("clgName"));
                college.setWebsite(rows.getString("clgWebsite"));
                college.setDescription(rows.getString("clgDescription"));
                found.add(college);
            }
        } finally {
            closeQuietly(rows);
            closeQuietly(statement);
        }
        for (College college : found) {
            loadCollegeFields(connection, college);
        }
        return found;
    }

    /**
     * 写入或覆盖一个学院及其研究方向、专业。
     *
     * @param connection 事务连接
     * @param college    学院
     * @throws SQLException 写入失败
     */
    static void saveCollege(Connection connection, College college) throws SQLException {
        if (college == null || blank(college.getUuid())) {
            throw new IllegalArgumentException("学院 uuid 不能为空");
        }
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement("INSERT INTO tblCollege (clgUuid, clgName,"
                    + " clgWebsite, clgDescription) VALUES (?, ?, ?, ?)"
                    + " ON DUPLICATE KEY UPDATE clgName = VALUES(clgName),"
                    + " clgWebsite = VALUES(clgWebsite), clgDescription = VALUES(clgDescription)");
            statement.setString(1, college.getUuid());
            statement.setString(2, college.getName() == null ? "" : college.getName());
            setNullable(statement, 3, college.getWebsite());
            setNullable(statement, 4, college.getDescription());
            statement.executeUpdate();
        } finally {
            closeQuietly(statement);
        }
        replaceCollegeFields(connection, college);
    }

    /**
     * 加载全部教师（含研究方向与时间槽；不含已认领课程的反向索引）。
     *
     * @param connection 事务连接
     * @return 教师列表，不返回 null
     * @throws SQLException 查询失败
     */
    static List<Teacher> loadTeachers(Connection connection) throws SQLException {
        List<Teacher> found = new ArrayList<Teacher>();
        PreparedStatement statement = null;
        ResultSet rows = null;
        try {
            statement = connection.prepareStatement("SELECT tcUuid, tcCollegeUuid,"
                    + " tcResearchGroup FROM tblTeacher ORDER BY tcUuid ASC");
            rows = statement.executeQuery();
            while (rows.next()) {
                Teacher teacher = new Teacher(rows.getString("tcUuid"),
                        rows.getString("tcCollegeUuid"));
                teacher.setResearchGroup(rows.getString("tcResearchGroup"));
                found.add(teacher);
            }
        } finally {
            closeQuietly(rows);
            closeQuietly(statement);
        }
        for (Teacher teacher : found) {
            loadTeacherFields(connection, teacher);
            teacher.getAvailableTimeslots().addAll(TimeslotStoreJdbc.load(connection,
                    SLOT_TEACHER_AVAILABLE, teacher.getUuid()));
            teacher.getPreferenceTimeslots().addAll(TimeslotStoreJdbc.load(connection,
                    SLOT_TEACHER_PREFERENCE, teacher.getUuid()));
        }
        return found;
    }

    /**
     * 写入或覆盖一个教师及其研究方向、时间槽。
     *
     * @param connection 事务连接
     * @param teacher    教师
     * @throws SQLException 写入失败
     */
    static void saveTeacher(Connection connection, Teacher teacher) throws SQLException {
        if (teacher == null || blank(teacher.getUuid())) {
            throw new IllegalArgumentException("教师 uuid 不能为空");
        }
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement("INSERT INTO tblTeacher (tcUuid,"
                    + " tcCollegeUuid, tcResearchGroup) VALUES (?, ?, ?)"
                    + " ON DUPLICATE KEY UPDATE tcCollegeUuid = VALUES(tcCollegeUuid),"
                    + " tcResearchGroup = VALUES(tcResearchGroup)");
            statement.setString(1, teacher.getUuid());
            statement.setString(2, teacher.getCollegeUuid() == null
                    ? ""
                    : teacher.getCollegeUuid());
            setNullable(statement, 3, teacher.getResearchGroup());
            statement.executeUpdate();
        } finally {
            closeQuietly(statement);
        }
        replaceTeacherFields(connection, teacher);
        TimeslotStoreJdbc.replace(connection, SLOT_TEACHER_AVAILABLE, teacher.getUuid(),
                teacher.getAvailableTimeslots());
        TimeslotStoreJdbc.replace(connection, SLOT_TEACHER_PREFERENCE, teacher.getUuid(),
                teacher.getPreferenceTimeslots());
    }

    /**
     * 删除一个教师及其研究方向、时间槽。
     *
     * @param connection 事务连接
     * @param uuid       教师 uuid
     * @return 命中记录为 true
     * @throws SQLException 删除失败
     */
    static boolean deleteTeacher(Connection connection, String uuid) throws SQLException {
        if (blank(uuid)) {
            return false;
        }
        PreparedStatement statement = null;
        try {
            // 子表先删：tblTeacherField 上有指向 tblTeacher 的外键，反过来会报 1451
            statement = connection.prepareStatement(
                    "DELETE FROM tblTeacherField WHERE tcUuid = ?");
            statement.setString(1, uuid);
            statement.executeUpdate();
            closeQuietly(statement);

            TimeslotStoreJdbc.delete(connection, SLOT_TEACHER_AVAILABLE, uuid);
            TimeslotStoreJdbc.delete(connection, SLOT_TEACHER_PREFERENCE, uuid);

            statement = connection.prepareStatement("DELETE FROM tblTeacher WHERE tcUuid = ?");
            statement.setString(1, uuid);
            return statement.executeUpdate() > 0;
        } finally {
            closeQuietly(statement);
        }
    }

    /**
     * 加载全部选课模块的学生档案（含时间槽；不含已选课程的反向索引）。
     *
     * @param connection 事务连接
     * @return 学生列表，不返回 null
     * @throws SQLException 查询失败
     */
    static List<Student> loadStudents(Connection connection) throws SQLException {
        List<Student> found = new ArrayList<Student>();
        PreparedStatement statement = null;
        ResultSet rows = null;
        try {
            statement = connection.prepareStatement("SELECT uUuid, cstCollegeUuid, cstMajor"
                    + " FROM tblCourseStudent ORDER BY uUuid ASC");
            rows = statement.executeQuery();
            while (rows.next()) {
                found.add(new Student(rows.getString("uUuid"),
                        rows.getString("cstCollegeUuid"),
                        fieldOrNull(rows.getString("cstMajor"))));
            }
        } finally {
            closeQuietly(rows);
            closeQuietly(statement);
        }
        for (Student student : found) {
            student.getAvailableTimeslots().addAll(TimeslotStoreJdbc.load(connection,
                    SLOT_STUDENT_AVAILABLE, student.getUuid()));
            student.getPreferenceTimeslots().addAll(TimeslotStoreJdbc.load(connection,
                    SLOT_STUDENT_PREFERENCE, student.getUuid()));
        }
        return found;
    }

    /**
     * 写入或覆盖一个学生档案及其时间槽。
     *
     * @param connection 事务连接
     * @param student    学生
     * @throws SQLException 写入失败
     */
    static void saveStudent(Connection connection, Student student) throws SQLException {
        if (student == null || blank(student.getUuid())) {
            throw new IllegalArgumentException("学生 uuid 不能为空");
        }
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement("INSERT INTO tblCourseStudent (uUuid,"
                    + " cstCollegeUuid, cstMajor) VALUES (?, ?, ?)"
                    + " ON DUPLICATE KEY UPDATE cstCollegeUuid = VALUES(cstCollegeUuid),"
                    + " cstMajor = VALUES(cstMajor)");
            statement.setString(1, student.getUuid());
            statement.setString(2, student.getCollegeUuid() == null
                    ? ""
                    : student.getCollegeUuid());
            statement.setString(3, student.getMajor() == null
                    ? ""
                    : student.getMajor().getName());
            statement.executeUpdate();
        } finally {
            closeQuietly(statement);
        }
        TimeslotStoreJdbc.replace(connection, SLOT_STUDENT_AVAILABLE, student.getUuid(),
                student.getAvailableTimeslots());
        TimeslotStoreJdbc.replace(connection, SLOT_STUDENT_PREFERENCE, student.getUuid(),
                student.getPreferenceTimeslots());
    }

    /**
     * 加载全部教室（含标签与时间槽）。
     *
     * @param connection 事务连接
     * @return 教室列表，不返回 null
     * @throws SQLException 查询失败
     */
    static List<Classroom> loadClassrooms(Connection connection) throws SQLException {
        List<Classroom> found = new ArrayList<Classroom>();
        PreparedStatement statement = null;
        ResultSet rows = null;
        try {
            statement = connection.prepareStatement("SELECT crUuid, crCollegeUuid, crCapacity,"
                    + " crLocation, crName, crBuildingUuid FROM tblClassroom"
                    + " ORDER BY crLocation ASC, crName ASC");
            rows = statement.executeQuery();
            while (rows.next()) {
                Classroom classroom = new Classroom(rows.getString("crUuid"),
                        rows.getString("crCollegeUuid"), rows.getInt("crCapacity"),
                        rows.getString("crLocation"));
                classroom.setName(rows.getString("crName"));
                classroom.setBuildingUuid(rows.getString("crBuildingUuid"));
                found.add(classroom);
            }
        } finally {
            closeQuietly(rows);
            closeQuietly(statement);
        }
        for (Classroom classroom : found) {
            loadTags(connection, classroom);
            classroom.getAvailableTimeslots().addAll(TimeslotStoreJdbc.load(connection,
                    SLOT_CLASSROOM_AVAILABLE, classroom.getUuid()));
            classroom.getPreferenceTimeslots().addAll(TimeslotStoreJdbc.load(connection,
                    SLOT_CLASSROOM_PREFERENCE, classroom.getUuid()));
        }
        return found;
    }

    /**
     * 写入或覆盖一个教室及其标签、时间槽。
     *
     * @param connection 事务连接
     * @param classroom  教室
     * @throws SQLException 写入失败
     */
    static void saveClassroom(Connection connection, Classroom classroom) throws SQLException {
        if (classroom == null || blank(classroom.getUuid())) {
            throw new IllegalArgumentException("教室 uuid 不能为空");
        }
        String sql = "INSERT INTO tblClassroom (crUuid, crCollegeUuid, crCapacity, crLocation,"
                + " crName, crBuildingUuid)"
                + " VALUES (?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE crCollegeUuid ="
                + " VALUES(crCollegeUuid), crCapacity = VALUES(crCapacity),"
                + " crLocation = VALUES(crLocation), crName = VALUES(crName),"
                + " crBuildingUuid = VALUES(crBuildingUuid)";
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement(sql);
            statement.setString(1, classroom.getUuid());
            setNullable(statement, 2, classroom.getCollegeUuid());
            statement.setInt(3, classroom.getCapacity());
            statement.setString(4, classroom.getLocation() == null ? "" : classroom.getLocation());
            setNullable(statement, 5, classroom.getName());
            setNullable(statement, 6, classroom.getBuildingUuid());
            statement.executeUpdate();
        } finally {
            closeQuietly(statement);
        }
        replaceTags(connection, classroom);
        TimeslotStoreJdbc.replace(connection, SLOT_CLASSROOM_AVAILABLE, classroom.getUuid(),
                classroom.getAvailableTimeslots());
        TimeslotStoreJdbc.replace(connection, SLOT_CLASSROOM_PREFERENCE, classroom.getUuid(),
                classroom.getPreferenceTimeslots());
    }

    /**
     * 写入学院的研究方向与专业。
     *
     * @param connection 事务连接
     * @param college    学院
     * @throws SQLException 写入失败
     */
    private static void replaceCollegeFields(Connection connection, College college)
            throws SQLException {
        PreparedStatement delete = null;
        PreparedStatement insert = null;
        try {
            delete = connection.prepareStatement("DELETE FROM tblCollegeField WHERE clgUuid = ?");
            delete.setString(1, college.getUuid());
            delete.executeUpdate();

            insert = connection.prepareStatement("INSERT INTO tblCollegeField (clgUuid, cfKind,"
                    + " cfField) VALUES (?, ?, ?)");
            addFieldBatch(insert, college.getUuid(), KIND_DIRECTION,
                    college.getResearchDirections());
            addFieldBatch(insert, college.getUuid(), KIND_MAJOR, college.getMajors());
            insert.executeBatch();
        } finally {
            closeQuietly(delete);
            closeQuietly(insert);
        }
    }

    /**
     * 写入教师的研究方向。
     *
     * @param connection 事务连接
     * @param teacher    教师
     * @throws SQLException 写入失败
     */
    private static void replaceTeacherFields(Connection connection, Teacher teacher)
            throws SQLException {
        PreparedStatement delete = null;
        PreparedStatement insert = null;
        try {
            delete = connection.prepareStatement("DELETE FROM tblTeacherField WHERE tcUuid = ?");
            delete.setString(1, teacher.getUuid());
            delete.executeUpdate();

            insert = connection.prepareStatement(
                    "INSERT INTO tblTeacherField (tcUuid, tfField) VALUES (?, ?)");
            if (teacher.getResearchDirections() != null) {
                for (Field field : teacher.getResearchDirections()) {
                    insert.setString(1, teacher.getUuid());
                    insert.setString(2, field.getName());
                    insert.addBatch();
                }
            }
            insert.executeBatch();
        } finally {
            closeQuietly(delete);
            closeQuietly(insert);
        }
    }

    /**
     * 写入教室标签。
     *
     * @param connection 事务连接
     * @param classroom  教室
     * @throws SQLException 写入失败
     */
    private static void replaceTags(Connection connection, Classroom classroom)
            throws SQLException {
        PreparedStatement delete = null;
        PreparedStatement insert = null;
        try {
            delete = connection.prepareStatement("DELETE FROM tblClassroomTag WHERE crUuid = ?");
            delete.setString(1, classroom.getUuid());
            delete.executeUpdate();

            insert = connection.prepareStatement(
                    "INSERT INTO tblClassroomTag (crUuid, ctagLabel) VALUES (?, ?)");
            if (classroom.getTags() != null) {
                for (String tag : classroom.getTags()) {
                    insert.setString(1, classroom.getUuid());
                    insert.setString(2, tag);
                    insert.addBatch();
                }
            }
            insert.executeBatch();
        } finally {
            closeQuietly(delete);
            closeQuietly(insert);
        }
    }

    /**
     * 读回学院的研究方向与专业。
     *
     * @param connection 事务连接
     * @param college    学院
     * @throws SQLException 查询失败
     */
    private static void loadCollegeFields(Connection connection, College college)
            throws SQLException {
        PreparedStatement statement = null;
        ResultSet rows = null;
        try {
            statement = connection.prepareStatement("SELECT cfKind, cfField FROM tblCollegeField"
                    + " WHERE clgUuid = ?");
            statement.setString(1, college.getUuid());
            rows = statement.executeQuery();
            while (rows.next()) {
                Field field = fieldOrNull(rows.getString("cfField"));
                if (field == null) {
                    continue;
                }
                if (KIND_DIRECTION.equals(rows.getString("cfKind"))) {
                    college.getResearchDirections().add(field);
                } else if (KIND_MAJOR.equals(rows.getString("cfKind"))) {
                    college.getMajors().add(field);
                }
            }
        } finally {
            closeQuietly(rows);
            closeQuietly(statement);
        }
    }

    /**
     * 读回教师的研究方向。
     *
     * @param connection 事务连接
     * @param teacher    教师
     * @throws SQLException 查询失败
     */
    private static void loadTeacherFields(Connection connection, Teacher teacher)
            throws SQLException {
        PreparedStatement statement = null;
        ResultSet rows = null;
        try {
            statement = connection.prepareStatement(
                    "SELECT tfField FROM tblTeacherField WHERE tcUuid = ?");
            statement.setString(1, teacher.getUuid());
            rows = statement.executeQuery();
            while (rows.next()) {
                Field direction = fieldOrNull(rows.getString("tfField"));
                if (direction != null) {
                    teacher.getResearchDirections().add(direction);
                }
            }
        } finally {
            closeQuietly(rows);
            closeQuietly(statement);
        }
    }

    /**
     * 读回教室标签。
     *
     * @param connection 事务连接
     * @param classroom  教室
     * @throws SQLException 查询失败
     */
    private static void loadTags(Connection connection, Classroom classroom) throws SQLException {
        PreparedStatement statement = null;
        ResultSet rows = null;
        try {
            statement = connection.prepareStatement(
                    "SELECT ctagLabel FROM tblClassroomTag WHERE crUuid = ?");
            statement.setString(1, classroom.getUuid());
            rows = statement.executeQuery();
            while (rows.next()) {
                classroom.getTags().add(rows.getString("ctagLabel"));
            }
        } finally {
            closeQuietly(rows);
            closeQuietly(statement);
        }
    }

    /**
     * 按需构造领域对象：库里存空串（或 NULL）时表示「未设置」。
     *
     * <p>
     * 不能把空值直接交给 {@link Field} —— 它拒绝空名（这个不变式是对的），而 {@code cstMajor} / {@code cfField} /
     * {@code tfField} 都是可空列，存空串表示未设置。 少了这一步转换，一行空值就会让整个服务装配不起来。专业与研究方向本就是可选的。
     *
     * @param name 领域名称；空或全空白视为未设置
     * @return 领域对象；未设置时返回 null
     */
    private static Field fieldOrNull(String name) {
        return blank(name) ? null : new Field(name);
    }

    /**
     * 为一批领域追加插入参数。
     *
     * @param statement 预编译语句，参数为 (uuid, kind, field)
     * @param ownerUuid 归属 uuid
     * @param kind      kind 取值
     * @param fields    领域集合；可为 null
     * @throws SQLException 追加失败
     */
    private static void addFieldBatch(PreparedStatement statement, String ownerUuid, String kind,
            Set<Field> fields) throws SQLException {
        if (fields == null) {
            return;
        }
        for (Field field : fields) {
            statement.setString(1, ownerUuid);
            statement.setString(2, kind);
            statement.setString(3, field.getName());
            statement.addBatch();
        }
    }

}
