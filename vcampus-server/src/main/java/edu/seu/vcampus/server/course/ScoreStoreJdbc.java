package edu.seu.vcampus.server.course;

import edu.seu.vcampus.common.course.Score;
import edu.seu.vcampus.server.db.DatabaseAccessException;
import edu.seu.vcampus.server.db.DbHelper;
import edu.seu.vcampus.server.db.JdbcQuery;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static edu.seu.vcampus.server.db.JdbcSupport.blank;
import static edu.seu.vcampus.server.db.JdbcSupport.closeQuietly;

/**
 * 【MySQL 版】成绩的持久化后端：落表 {@code tblScore}。
 *
 * <p>
 * 表结构见 {@code sql/vCampus.sql} 的 {@code tblScore}：主键是 {@code (uUuid, coUuid, scSemester)}，另有一个自增的
 * {@code scId} 只用于回填实体的 {@code m_id}，以及一个 {@code scCourseCode} 快照 —— 因为 {@link Score}
 * 引用课程用的是<b>课程编号</b>，不是 uuid。
 *
 * <p>
 * 写入是两步而不是一条 {@code INSERT ... SELECT}：先按编号查出 {@code coUuid}，查不到就抛
 * {@link IllegalStateException}（成绩表上有指向课程的外键，硬写只会换来一句看不懂的 1452）。 写完之后再查一次 {@code scId} 回填，因为 upsert
 * 命中已有行时 {@code getGeneratedKeys} 拿不到号。
 */
public final class ScoreStoreJdbc implements ScoreStore {

    /** 查询列。 */
    private static final String COLUMNS = "scId, uUuid, coUuid, scCourseCode, scSemester,"
            + " scScore, scSavedAt";

    /** @param studentUuid 学生 uuid @param courseCode 课程编号 @return 成绩；不存在返回 null */
    @Override
    public Score find(String studentUuid, String courseCode) {
        if (blank(studentUuid) || blank(courseCode)) {
            return null;
        }
        return JdbcQuery.one("SELECT " + COLUMNS + " FROM tblScore"
                + " WHERE uUuid = ? AND scCourseCode = ? ORDER BY scSemester ASC, scId ASC",
                SCORE_MAPPER, studentUuid, courseCode);
    }

    /** @param studentUuid 学生 uuid @return 该学生的全部成绩 */
    @Override
    public List<Score> findByStudent(String studentUuid) {
        if (blank(studentUuid)) {
            return new ArrayList<Score>();
        }
        return JdbcQuery.list("SELECT " + COLUMNS + " FROM tblScore WHERE uUuid = ?"
                + " ORDER BY scSemester ASC, scId ASC", SCORE_MAPPER, studentUuid);
    }

    /** @param courseCode 课程编号 @return 该课程的全部成绩 */
    @Override
    public List<Score> findByCourse(String courseCode) {
        if (blank(courseCode)) {
            return new ArrayList<Score>();
        }
        return JdbcQuery.list("SELECT " + COLUMNS + " FROM tblScore WHERE scCourseCode = ?"
                + " ORDER BY uUuid ASC, scSemester ASC, scId ASC", SCORE_MAPPER, courseCode);
    }

    /** @param score 成绩 @return 落库后的记录号；写入失败返回 null */
    @Override
    public Long save(Score score) {
        if (score == null || blank(score.getStudentUuid()) || blank(score.getCourseCode())) {
            return null;
        }
        String sql = "INSERT INTO tblScore (uUuid, coUuid, scCourseCode, scSemester, scScore,"
                + " scSavedAt) VALUES (?, ?, ?, ?, ?, ?)"
                + " ON DUPLICATE KEY UPDATE scScore = VALUES(scScore),"
                + " scSavedAt = VALUES(scSavedAt), scCourseCode = VALUES(scCourseCode)";
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = DbHelper.getConnection();
            String courseUuid = courseUuid(connection, score.getCourseCode());
            if (courseUuid == null) {
                throw new IllegalStateException("成绩对应的课程不存在: " + score.getCourseCode());
            }
            statement = connection.prepareStatement(sql);
            statement.setString(1, score.getStudentUuid());
            statement.setString(2, courseUuid);
            statement.setString(3, score.getCourseCode());
            statement.setString(4, score.getSemester() == null ? "" : score.getSemester());
            if (score.getScore() == null) {
                statement.setNull(5, java.sql.Types.DECIMAL);
            } else {
                statement.setBigDecimal(5, java.math.BigDecimal.valueOf(score.getScore()));
            }
            statement.setTimestamp(6, new java.sql.Timestamp(System.currentTimeMillis()));
            statement.executeUpdate();
            return storedId(connection, score, courseUuid);
        } catch (SQLException e) {
            throw new DatabaseAccessException("保存成绩失败: " + score.getCourseCode(), e);
        } finally {
            closeQuietly(statement);
            closeQuietly(connection);
        }
    }

    /** @param studentUuid 学生 uuid @param courseCode 课程编号 @return 命中记录为 true */
    @Override
    public boolean delete(String studentUuid, String courseCode) {
        if (blank(studentUuid) || blank(courseCode)) {
            return false;
        }
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = DbHelper.getConnection();
            statement = connection.prepareStatement(
                    "DELETE FROM tblScore WHERE uUuid = ? AND scCourseCode = ?");
            statement.setString(1, studentUuid);
            statement.setString(2, courseCode);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseAccessException("删除成绩失败: " + courseCode, e);
        } finally {
            closeQuietly(statement);
            closeQuietly(connection);
        }
    }

    /**
     * 按课程编号查课程 uuid。
     *
     * @param connection 已取到的连接
     * @param courseCode 课程编号
     * @return 课程 uuid；课程不存在返回 null
     * @throws SQLException 查询失败
     */
    private static String courseUuid(Connection connection, String courseCode) throws SQLException {
        PreparedStatement statement = null;
        ResultSet rows = null;
        try {
            statement = connection.prepareStatement("SELECT coUuid FROM tblCourse WHERE coId = ?");
            statement.setString(1, courseCode);
            rows = statement.executeQuery();
            return rows.next() ? rows.getString(1) : null;
        } finally {
            closeQuietly(rows);
            closeQuietly(statement);
        }
    }

    /**
     * 查回刚写入那条成绩的记录号。
     *
     * @param connection 已取到的连接
     * @param score      成绩
     * @param courseUuid 课程 uuid
     * @return 记录号；查不到返回 null
     * @throws SQLException 查询失败
     */
    private static Long storedId(Connection connection, Score score, String courseUuid)
            throws SQLException {
        PreparedStatement statement = null;
        ResultSet rows = null;
        try {
            statement = connection.prepareStatement("SELECT scId FROM tblScore WHERE uUuid = ?"
                    + " AND coUuid = ? AND scSemester = ?");
            statement.setString(1, score.getStudentUuid());
            statement.setString(2, courseUuid);
            statement.setString(3, score.getSemester() == null ? "" : score.getSemester());
            rows = statement.executeQuery();
            return rows.next() ? Long.valueOf(rows.getLong(1)) : null;
        } finally {
            closeQuietly(rows);
            closeQuietly(statement);
        }
    }

    /** 行映射器：结果集当前行 → 成绩。 */
    private static final JdbcQuery.RowMapper<Score> SCORE_MAPPER = new JdbcQuery.RowMapper<Score>() {
        @Override
        public Score map(ResultSet rows) throws SQLException {
            Score score = new Score(rows.getString("uUuid"), rows.getString("scCourseCode"),
                    rows.getString("scSemester"));
            score.setId(Long.valueOf(rows.getLong("scId")));
            double value = rows.getDouble("scScore");
            score.setScore(rows.wasNull() ? null : Double.valueOf(value));
            return score;
        }
    };

}
