package edu.seu.vcampus.server.course;

import edu.seu.vcampus.common.course.Score;
import edu.seu.vcampus.server.db.DatabaseAvailability;
import edu.seu.vcampus.server.db.DbHelper;
import edu.seu.vcampus.server.db.TestIds;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ScoreStoreJdbc} 的真库集成测试。
 *
 * <p>
 * <b>环境门控</b>（见 ADR-0005）：连不上 MySQL 时整体跳过。前置是库中已有 {@code sql/vCampus.sql} 建出的 {@code tblScore} 与
 * {@code tblCourse}。
 *
 * <p>
 * 成绩表对 {@code tblUserCredential} 与 {@code tblCourse} 都有外键，所以每个用例先造一行临时账户 和一门临时课程，跑完按外键顺序（成绩 → 课程 →
 * 账户）物理删除。{@code ucUuid} 与 {@code coUuid} 是 {@code CHAR(36)}、{@code coId} 是 {@code VARCHAR(16)}。
 */
class ScoreStoreJdbcTest {

    /** 测试 uuid 前缀。 */
    private static final String UUID_PREFIX = "jdbc-score-it-";

    /** 本轮测试学生 uuid。 */
    private String m_studentUuid;

    /** 本轮测试课程 uuid。 */
    private String m_courseUuid;

    /** 本轮测试课程编号。 */
    private String m_courseCode;

    /** 被测后端。 */
    private ScoreStoreJdbc m_store;

    /** 每个用例前确认数据库可用、造临时用户与课程并生成唯一标识。 */
    @BeforeEach
    void setUp() {
        Assumptions.assumeTrue(databaseAvailable(),
                "MySQL 不可用，跳过 JDBC 集成测试（docker compose up -d mysql 后自动执行）");
        long stamp = System.nanoTime();
        m_studentUuid = uuid(stamp);
        m_courseUuid = uuid(stamp + 1L);
        m_courseCode = "C" + TestIds.hex(stamp, 10);
        m_store = new ScoreStoreJdbc();
        insertAccount(m_studentUuid, "C" + TestIds.hex(stamp, 6));
        insertCourse(m_courseUuid, m_courseCode);
    }

    /** 用例后按外键顺序物理删除测试数据。 */
    @AfterEach
    void tearDown() {
        executeUpdate("DELETE FROM tblScore WHERE uUuid = ?", m_studentUuid);
        executeUpdate("DELETE FROM tblCourse WHERE coUuid = ?", m_courseUuid);
        executeUpdate("DELETE FROM tblUserCredential WHERE ucUuid = ?", m_studentUuid);
    }

    @Test
    void saveBackfillsRecordIdAndReadsItBack() {
        Score score = new Score(m_studentUuid, m_courseCode, "2026-2027-1");
        score.setScore(Double.valueOf(92.5));

        Long id = m_store.save(score);
        assertNotNull(id, "写入应回填记录号");

        Score loaded = findScore();
        assertNotNull(loaded, "按学生 + 课程编号应能查到");
        assertEquals(id, loaded.getId(), "读回的成绩应带同一个记录号");
        assertEquals(m_studentUuid, loaded.getStudentUuid());
        assertEquals(m_courseCode, loaded.getCourseCode());
        assertEquals("2026-2027-1", loaded.getSemester());
        assertEquals(Double.valueOf(92.5), loaded.getScore());
    }

    @Test
    void saveWithoutScoreKeepsNullMeaningNotYetGraded() {
        m_store.save(new Score(m_studentUuid, m_courseCode, "2026-2027-1"));

        Score loaded = findScore();
        assertNotNull(loaded, "选课但未录入成绩也应有一条");
        assertNull(loaded.getScore(), "未录入时分数应为 null");
    }

    @Test
    void saveIsUpsertOnStudentCourseSemester() {
        Score first = new Score(m_studentUuid, m_courseCode, "2026-2027-1");
        first.setScore(Double.valueOf(60.0));
        m_store.save(first);

        Score again = new Score(m_studentUuid, m_courseCode, "2026-2027-1");
        again.setScore(Double.valueOf(88.0));
        m_store.save(again);

        assertEquals(1, countMine(), "同一学生同一课程同一学期只应有一行");
        assertEquals(Double.valueOf(88.0), findScore().getScore(), "分数应被覆盖");
    }

    @Test
    void saveRefusesUnknownCourseCode() {
        final Score orphan = new Score(m_studentUuid, "NO-SUCH-CODE", "2026-2027-1");
        orphan.setScore(Double.valueOf(70.0));

        assertThrows(IllegalStateException.class, new Executable() {
            @Override
            public void execute() throws Throwable {
                m_store.save(orphan);
            }
        });
    }

    @Test
    void deleteRemovesEverySemesterOfThatCourse() {
        m_store.save(new Score(m_studentUuid, m_courseCode, "2026-2027-1"));
        m_store.save(new Score(m_studentUuid, m_courseCode, "2027-2028-1"));
        assertEquals(2, countMine(), "两个学期应有两行");

        assertTrue(m_store.delete(m_studentUuid, m_courseCode), "删除应命中");
        assertEquals(0, countMine(), "同一课程的各学期应一并删除");
    }

    /**
     * 统计本用例那条成绩的行数（只按本用例的学生与课程过滤，不受库里其它数据影响）。
     *
     * @return 行数
     */
    private int countMine() {
        int count = 0;
        for (Score item : m_store.findByStudent(m_studentUuid)) {
            if (m_courseCode.equals(item.getCourseCode())) {
                count++;
            }
        }
        return count;
    }

    /**
     * 取出本用例那条成绩。
     *
     * @return 成绩；不存在返回 null
     */
    private Score findScore() {
        return m_store.find(m_studentUuid, m_courseCode);
    }

    /**
     * 生成恰好 36 位的测试 uuid（{@code uUuid} 是 {@code CHAR(36)}）。
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
     * 插入一行临时账户，满足成绩表的外键。
     *
     * @param userUuid 账户 uuid
     * @param username 登录名，最多 50 字符
     */
    private static void insertAccount(String userUuid, String username) {
        run("INSERT INTO tblUserCredential (ucUsername, ucUuid, ucName, ucSalt, ucHash, ucRole)"
                + " VALUES (?, ?, ?, ?, ?, ?)",
                username, userUuid, "集成测试", "testsalt", "testhash", "学生");
    }

    /**
     * 插入一门临时课程，满足成绩表的外键。
     *
     * @param courseUuid 课程 uuid
     * @param courseCode 课程编号
     */
    private static void insertCourse(String courseUuid, String courseCode) {
        run("INSERT INTO tblCourse (coUuid, coId, coName, coCredit, coState)"
                + " VALUES (?, ?, ?, 3, '开放')", courseUuid, courseCode, "集成测试课程");
    }

    /**
     * 执行一条单参数写语句（用于清理测试数据）。
     *
     * @param sql   语句
     * @param param 唯一参数
     */
    private static void executeUpdate(String sql, String param) {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = DbHelper.getConnection();
            statement = connection.prepareStatement(sql);
            statement.setString(1, param);
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
     * 执行一条多参数写语句。
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
            throw new IllegalStateException("准备测试数据失败", e);
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
