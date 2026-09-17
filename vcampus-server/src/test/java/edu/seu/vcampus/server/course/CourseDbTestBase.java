package edu.seu.vcampus.server.course;

import edu.seu.vcampus.server.db.DatabaseAvailability;
import edu.seu.vcampus.server.db.DbHelper;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;

/**
 * 课程模块测试基类：所有用例都跑在<b>真库</b>（{@link CourseStoreJdbc} / {@link ScoreStoreJdbc}）上。
 *
 * <p>
 * <b>环境门控</b>（见 ADR-0005）：连不上 MySQL 时整类跳过，前置条件是库里已有 {@code sql/vCampus.sql} 建出的表。
 *
 * <p>
 * <b>隔离方式</b>：用例假设「课程世界里一开始什么都没有」—— 这是内存实现时期的既有前提，也是这些用例
 * 能断言具体条数的原因。所以每个用例开跑前先把课程相关的十几张表清空，收尾时再把 {@code sql/vCampus-data.sql} 里的引用数据原样导回，
 * 保证后面跑的其它测试仍然看得到那所学院与三门课。
 *
 * <p>
 * <b>账户行要自己补</b>：{@code tblCourseSelection.uUuid} 与 {@code tblScore.uUuid} 都有指向
 * {@code tblUserCredential} 的外键，用例里凭空造的「假学生 uuid」是写不进去的，必须用 {@link #account(String)}
 * 建一个真账户。
 */
abstract class CourseDbTestBase {

    /** 本类测试自己建的账户 uuid 前缀，收尾时按它删干净。 */
    private static final String ACCOUNT_PREFIX = "cs-it-";

    /** 同一轮里保证 uuid 不重复。 */
    private static final AtomicInteger SEQ = new AtomicInteger();

    /** 课程相关表，按「子表在前」排列，逐张清空时不会被外键拦下。 */
    private static final String[] TABLES = {
        "tblScore", "tblCourseSelection", "tblCourseStudent", "tblCourseField", "tblTimeslot",
        "tblCourse", "tblTeacherField", "tblTeacher", "tblStudentProfile", "tblClassroomTag",
        "tblClassroom", "tblBuilding", "tblCollegeField", "tblCollege",
    };

    /** 引用数据脚本（相对模块目录或仓库根）。 */
    private static final String DATA_FILE = "sql" + File.separator + "vCampus-data.sql";

    /** 课程目录，接真库。 */
    protected CourseDao dbCourse;

    /** 成绩，接真库。 */
    protected ScoreDao dbScore;

    /** 清出干净的课程世界，并把两个 DAO 接上真库；数据库不可用时整类跳过。 */
    @BeforeEach
    void openDatabase() {
        Assumptions.assumeTrue(DatabaseAvailability.isReady(), "数据库不可用，跳过课程集成测试");
        clearCourseWorld();
        dbCourse = new CourseDao(new CourseStoreJdbc());
        dbScore = new ScoreDao(new ScoreStoreJdbc());
    }

    /** 收尾：清掉本轮数据，并把引用数据导回去。 */
    @AfterEach
    void closeDatabase() {
        if (dbCourse == null) {
            return;
        }
        clearCourseWorld();
        execute("DELETE FROM tblUserCredential WHERE ucUuid LIKE ?", ACCOUNT_PREFIX + "%");
        dbCourse = null;
        dbScore = null;
    }

    /** 整类跑完，把引用数据重置回脚本定义的样子。 */
    @AfterAll
    static void restoreReferenceData() {
        try {
            runScript();
        } catch (RuntimeException e) {
            System.err.println("[CourseDbTestBase] 还原引用数据失败：" + e.getMessage());
        }
    }

    /**
     * 建一个学生账户并返回它的 uuid。
     *
     * @param tag 便于辨认用途的短标签
     * @return 账户 uuid
     */
    protected static String account(String tag) {
        String uuid = ACCOUNT_PREFIX + tag + "-" + SEQ.incrementAndGet();
        execute("INSERT INTO tblUserCredential (ucUsername, ucUuid, ucSalt, ucHash, ucRole,"
                + " ucEnabled, ucName) VALUES (?, ?, 'salt', 'hash', '学生', 1, ?)",
                "acc-" + uuid, uuid, tag);
        return uuid;
    }

    /** 逐张清空课程相关表。 */
    private static void clearCourseWorld() {
        for (String table : TABLES) {
            execute("DELETE FROM " + table);
        }
    }

    /**
     * 执行一条带参数的写语句。
     *
     * @param sql    语句
     * @param params 参数
     */
    private static void execute(String sql, String... params) {
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
            throw new IllegalStateException("执行失败: " + sql, e);
        } finally {
            close(statement, connection);
        }
    }

    /** 把引用数据脚本重新导进测试库。 */
    private static void runScript() {
        File file = locateDataFile();
        if (file == null) {
            return;
        }
        String script;
        try {
            script = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("读取引用数据脚本失败: " + file, e);
        }
        Connection connection = null;
        Statement statement = null;
        try {
            connection = DbHelper.getConnection();
            statement = connection.createStatement();
            for (String raw : script.split(";")) {
                String sql = stripComments(raw);
                if (sql.isEmpty() || sql.toUpperCase().startsWith("USE ")) {
                    continue;
                }
                statement.execute(sql);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("导入引用数据失败: " + file, e);
        } finally {
            close(statement, connection);
        }
    }

    /**
     * 找到引用数据脚本。
     *
     * @return 脚本；找不到返回 null
     */
    private static File locateDataFile() {
        String[] candidates = {DATA_FILE, ".." + File.separator + DATA_FILE};
        for (String candidate : candidates) {
            File file = new File(candidate);
            if (file.isFile()) {
                return file;
            }
        }
        return null;
    }

    /**
     * 去掉行注释与空行。
     *
     * @param raw 一段语句文本
     * @return 可直接执行的语句
     */
    private static String stripComments(String raw) {
        StringBuilder cleaned = new StringBuilder();
        for (String line : raw.split("\\r?\\n")) {
            int comment = line.indexOf("--");
            String body = (comment < 0 ? line : line.substring(0, comment)).trim();
            if (!body.isEmpty()) {
                cleaned.append(body).append(' ');
            }
        }
        return cleaned.toString().trim();
    }

    /**
     * 安静关闭资源。
     *
     * @param statement  语句；可为 null
     * @param connection 连接；可为 null
     */
    private static void close(AutoCloseable statement, Connection connection) {
        if (statement != null) {
            try {
                statement.close();
            } catch (Exception ignored) {
                // 关闭失败无需上报
            }
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
                // 关闭失败无需上报
            }
        }
    }
}
