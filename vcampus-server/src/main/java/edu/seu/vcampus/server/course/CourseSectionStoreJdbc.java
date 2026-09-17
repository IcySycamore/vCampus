package edu.seu.vcampus.server.course;

import edu.seu.vcampus.common.course.CourseSection;
import edu.seu.vcampus.common.course.Field;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static edu.seu.vcampus.server.db.JdbcSupport.blank;
import static edu.seu.vcampus.server.db.JdbcSupport.closeQuietly;
import static edu.seu.vcampus.server.db.JdbcSupport.setNullable;

/**
 * 课程（{@link CourseSection}）在 {@code tblCourse} 及其子表上的读写。
 *
 * <p>
 * 从 {@link CourseStoreJdbc} 拆出来，是因为一门课程要联动四张表：主表 {@code tblCourse}、 可选专业与教师方向要求
 * {@code tblCourseField}、上课时间槽 {@code tblTimeslot}、已选学生
 * {@code tblCourseSelection}。放在一起会让那个类过于臃肿，而这几张表的读写完全自成一块。
 *
 * <p>
 * 子表一律「先删后插」：实体的集合是整体替换的语义，不先清掉，删掉一个可选专业之后旧行会留下来。
 *
 * <p>
 * 两处列与实体的对应需要说明：{@code coCredit} 是 {@code DECIMAL(3,1)} 而实体是 {@code int}， 往返无损；{@code coState}
 * 实体里没有对应字段，落库固定写 {@code '开放'}。
 */
final class CourseSectionStoreJdbc {

    /** 可选专业的 kind 取值。 */
    private static final String KIND_ELIGIBLE_MAJOR = "ELIGIBLE_MAJOR";

    /** 教师方向要求的 kind 取值。 */
    private static final String KIND_REQUIRED_DIRECTION = "REQUIRED_DIRECTION";

    /** 课程时间槽的归属类型。 */
    private static final String SLOT_SECTION = "SECTION";

    /** 主表列清单。 */
    private static final String COLUMNS = "coUuid, coId, coName, coCredit, coCollegeUuid,"
            + " coTeacherUuid, coCapacity, coSemester, coPreferredLocation, coClassroomUuid";

    private CourseSectionStoreJdbc() {
    }

    /**
     * 加载全部课程，含子表内容。
     *
     * @param connection 事务连接
     * @return 课程列表，不返回 null
     * @throws SQLException 查询失败
     */
    static List<CourseSection> loadAll(Connection connection) throws SQLException {
        List<CourseSection> found = new ArrayList<CourseSection>();
        PreparedStatement statement = null;
        ResultSet rows = null;
        try {
            statement = connection.prepareStatement("SELECT " + COLUMNS + " FROM tblCourse"
                    + " ORDER BY coId ASC");
            rows = statement.executeQuery();
            while (rows.next()) {
                found.add(toCourse(rows));
            }
        } finally {
            closeQuietly(rows);
            closeQuietly(statement);
        }
        for (CourseSection course : found) {
            loadFields(connection, course);
            loadStudents(connection, course);
            course.getTimeslots().addAll(
                    TimeslotStoreJdbc.load(connection, SLOT_SECTION, course.getUuid()));
        }
        return found;
    }

    /**
     * 写入或覆盖一门课程及其子表内容。
     *
     * @param connection 事务连接
     * @param course     课程
     * @throws SQLException 写入失败
     */
    static void save(Connection connection, CourseSection course) throws SQLException {
        if (course == null || blank(course.getUuid()) || blank(course.getCode())) {
            throw new IllegalArgumentException("课程 uuid 与编号都不能为空");
        }
        String sql = "INSERT INTO tblCourse (coUuid, coId, coName, coCredit, coState,"
                + " coCollegeUuid, coTeacherUuid, coCapacity, coSemester, coPreferredLocation,"
                + " coClassroomUuid) VALUES (?, ?, ?, ?, '开放', ?, ?, ?, ?, ?, ?)"
                + " ON DUPLICATE KEY UPDATE coName = VALUES(coName), coCredit = VALUES(coCredit),"
                + " coCollegeUuid = VALUES(coCollegeUuid), coTeacherUuid = VALUES(coTeacherUuid),"
                + " coCapacity = VALUES(coCapacity), coSemester = VALUES(coSemester),"
                + " coPreferredLocation = VALUES(coPreferredLocation),"
                + " coClassroomUuid = VALUES(coClassroomUuid)";
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement(sql);
            statement.setString(1, course.getUuid());
            statement.setString(2, course.getCode());
            statement.setString(3, course.getName() == null ? "" : course.getName());
            statement.setBigDecimal(4, BigDecimal.valueOf(course.getCredit()));
            setNullable(statement, 5, course.getCollegeUuid());
            setNullable(statement, 6, course.getTeacherUuid());
            statement.setInt(7, course.getCapacity());
            setNullable(statement, 8, course.getSemester());
            setNullable(statement, 9, course.getPreferredLocation());
            setNullable(statement, 10, course.getClassroomUuid());
            statement.executeUpdate();
        } finally {
            closeQuietly(statement);
        }
        replaceFields(connection, course);
        replaceStudents(connection, course);
        TimeslotStoreJdbc.replace(connection, SLOT_SECTION, course.getUuid(),
                course.getTimeslots());
    }

    /**
     * 写入课程的可选专业与教师方向要求。
     *
     * @param connection 事务连接
     * @param course     课程
     * @throws SQLException 写入失败
     */
    private static void replaceFields(Connection connection, CourseSection course)
            throws SQLException {
        PreparedStatement delete = null;
        PreparedStatement insert = null;
        try {
            delete = connection.prepareStatement(
                    "DELETE FROM tblCourseField WHERE coUuid = ?");
            delete.setString(1, course.getUuid());
            delete.executeUpdate();

            insert = connection.prepareStatement("INSERT INTO tblCourseField (coUuid, cfdKind,"
                    + " cfdField) VALUES (?, ?, ?)");
            addFieldBatch(insert, course.getUuid(), KIND_ELIGIBLE_MAJOR,
                    course.getEligibleMajors());
            addFieldBatch(insert, course.getUuid(), KIND_REQUIRED_DIRECTION,
                    course.getRequiredDirections());
            insert.executeBatch();
        } finally {
            closeQuietly(delete);
            closeQuietly(insert);
        }
    }

    /**
     * 为一批领域追加插入参数。
     *
     * @param statement  预编译语句，参数为 (coUuid, kind, field)
     * @param courseUuid 课程 uuid
     * @param kind       kind 取值
     * @param fields     领域集合；可为 null 或空
     * @throws SQLException 追加失败
     */
    private static void addFieldBatch(PreparedStatement statement, String courseUuid, String kind,
            java.util.Set<Field> fields) throws SQLException {
        if (fields == null) {
            return;
        }
        for (Field field : fields) {
            statement.setString(1, courseUuid);
            statement.setString(2, kind);
            statement.setString(3, field.getName());
            statement.addBatch();
        }
    }

    /**
     * 写入课程的已选学生。
     *
     * @param connection 事务连接
     * @param course     课程
     * @throws SQLException 写入失败
     */
    private static void replaceStudents(Connection connection, CourseSection course)
            throws SQLException {
        PreparedStatement delete = null;
        PreparedStatement insert = null;
        try {
            delete = connection.prepareStatement(
                    "DELETE FROM tblCourseSelection WHERE coUuid = ?");
            delete.setString(1, course.getUuid());
            delete.executeUpdate();

            insert = connection.prepareStatement("INSERT INTO tblCourseSelection (uUuid, coUuid,"
                    + " csSemester) VALUES (?, ?, ?)");
            for (String studentUuid : course.getStudentUuids()) {
                insert.setString(1, studentUuid);
                insert.setString(2, course.getUuid());
                insert.setString(3, course.getSemester() == null ? "" : course.getSemester());
                insert.addBatch();
            }
            insert.executeBatch();
        } finally {
            closeQuietly(delete);
            closeQuietly(insert);
        }
    }

    /**
     * 读回课程的可选专业与教师方向要求。
     *
     * @param connection 事务连接
     * @param course     课程
     * @throws SQLException 查询失败
     */
    private static void loadFields(Connection connection, CourseSection course)
            throws SQLException {
        PreparedStatement statement = null;
        ResultSet rows = null;
        try {
            statement = connection.prepareStatement("SELECT cfdKind, cfdField FROM tblCourseField"
                    + " WHERE coUuid = ?");
            statement.setString(1, course.getUuid());
            rows = statement.executeQuery();
            while (rows.next()) {
                Field field = new Field(rows.getString("cfdField"));
                if (KIND_ELIGIBLE_MAJOR.equals(rows.getString("cfdKind"))) {
                    course.getEligibleMajors().add(field);
                } else if (KIND_REQUIRED_DIRECTION.equals(rows.getString("cfdKind"))) {
                    course.getRequiredDirections().add(field);
                }
            }
        } finally {
            closeQuietly(rows);
            closeQuietly(statement);
        }
    }

    /**
     * 读回课程的已选学生。
     *
     * @param connection 事务连接
     * @param course     课程
     * @throws SQLException 查询失败
     */
    private static void loadStudents(Connection connection, CourseSection course)
            throws SQLException {
        PreparedStatement statement = null;
        ResultSet rows = null;
        try {
            statement = connection.prepareStatement(
                    "SELECT uUuid FROM tblCourseSelection WHERE coUuid = ?");
            statement.setString(1, course.getUuid());
            rows = statement.executeQuery();
            while (rows.next()) {
                course.getStudentUuids().add(rows.getString("uUuid"));
            }
        } finally {
            closeQuietly(rows);
            closeQuietly(statement);
        }
    }

    /**
     * 删除一门课程及其全部子行。
     *
     * <p>
     * 顺序是必须的：{@code tblScore} 上有指向 {@code tblCourse} 的外键，主表先删会被拦住。 时间槽按
     * {@code SLOT_SECTION} 归属类型定位，与读写时用的类型一致。
     *
     * @param connection 事务连接
     * @param uuid       课程 uuid；为 null 时直接返回 false
     * @return 主表命中记录为 true
     * @throws SQLException 删除失败
     */
    static boolean deleteCourse(Connection connection, String uuid) throws SQLException {
        if (uuid == null) {
            return false;
        }
        deleteBy(connection, "DELETE FROM tblCourseSelection WHERE coUuid = ?", uuid);
        deleteBy(connection, "DELETE FROM tblCourseField WHERE coUuid = ?", uuid);
        deleteBy(connection, "DELETE FROM tblScore WHERE coUuid = ?", uuid);
        PreparedStatement slots = null;
        try {
            slots = connection.prepareStatement("DELETE FROM tblTimeslot"
                    + " WHERE tsOwnerType = ? AND tsOwnerUuid = ?");
            slots.setString(1, SLOT_SECTION);
            slots.setString(2, uuid);
            slots.executeUpdate();
        } finally {
            closeQuietly(slots);
        }
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement("DELETE FROM tblCourse WHERE coUuid = ?");
            statement.setString(1, uuid);
            return statement.executeUpdate() > 0;
        } finally {
            closeQuietly(statement);
        }
    }

    /**
     * 按 uuid 删一张子表的行。
     *
     * @param connection 事务连接
     * @param sql        带一个占位符的删除语句
     * @param uuid       课程 uuid
     * @throws SQLException 删除失败
     */
    private static void deleteBy(Connection connection, String sql, String uuid)
            throws SQLException {
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement(sql);
            statement.setString(1, uuid);
            statement.executeUpdate();
        } finally {
            closeQuietly(statement);
        }
    }

    /**
     * 结果行 → 课程（不含子表内容）。
     *
     * @param rows 已定位到某行的结果集
     * @return 课程
     * @throws SQLException 读取失败
     */
    private static CourseSection toCourse(ResultSet rows) throws SQLException {
        CourseSection course = new CourseSection(rows.getString("coId"), rows.getString("coName"),
                rows.getString("coCollegeUuid"), rows.getInt("coCapacity"));
        course.setUuid(rows.getString("coUuid"));
        course.setCredit(rows.getInt("coCredit"));
        course.setTeacherUuid(rows.getString("coTeacherUuid"));
        course.setSemester(rows.getString("coSemester"));
        course.setPreferredLocation(rows.getString("coPreferredLocation"));
        course.setClassroomUuid(rows.getString("coClassroomUuid"));
        return course;
    }

}
