package edu.seu.vcampus.server.course;

import edu.seu.vcampus.common.course.College;
import edu.seu.vcampus.server.db.DatabaseAvailability;
import edu.seu.vcampus.server.db.DbHelper;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link CollegePoolBootstrap} 的真库集成测试。
 *
 * <p>
 * <b>环境门控</b>（见 ADR-0005）：连不上 MySQL 时整体跳过。前置是库中已有建库脚本建出的表。
 *
 * <p>
 * 用例自己生成唯一 uuid 与唯一学院名（{@code clgName} 上有唯一索引），跑完按 uuid 删干净，不碰库里 既有的学院 —— 开发库里那所缺省学院要留着给别的用例用。
 */
class CollegePoolBootstrapTest {

    /** 本轮测试的 uuid 前缀。 */
    private static final String PREFIX = "pool-boot-";

    /** 本轮写入的学院 uuid。 */
    private final List<String> m_created = new ArrayList<String>();

    /** 临时目录（引导文件放这里，不污染仓库里的 data/）。 */
    @TempDir
    File m_directory;

    /** 用例前确认数据库可用。 */
    @BeforeEach
    void setUp() {
        Assumptions.assumeTrue(databaseAvailable(),
                "MySQL 不可用，跳过学院池引导的真库测试（docker compose up -d mysql）");
    }

    /** 用例后按 uuid 删掉本轮建的学院。 */
    @AfterEach
    void tearDown() {
        for (String uuid : m_created) {
            execute("DELETE FROM tblCollege WHERE clgUuid = ?", uuid);
        }
    }

    /** 文件不存在时生成模板，并把模板里的缺省学院导入 —— 全新部署靠的就是这一步。 */
    @Test
    void missingFileIsTemplatedAndItsCollegeIsImported() throws IOException {
        File file = new File(m_directory, "colleges.tsv");
        CourseDao dao = newDao();

        List<String> pool = CollegePoolBootstrap.seed(dao, file);
        m_created.addAll(pool);

        assertTrue(file.isFile(), "文件缺失时应写出模板");
        assertEquals(Arrays.asList(CourseModule.DEFAULT_COLLEGE_UUID), pool,
                "模板里的缺省学院应当就是学院池");
        College stored = newDao().findCollege(CourseModule.DEFAULT_COLLEGE_UUID);
        assertNotNull(stored, "缺省学院应当真的落库，否则师生注册必撞外键");
        assertEquals("计算机学院", stored.getName());
    }

    /** 文件里的每一行都入库，池的顺序按文件顺序；官网与简介可省略。 */
    @Test
    void fileLinesBecomeThePoolInFileOrder() throws IOException {
        String first = uuid("a");
        String second = uuid("b");
        write("colleges.tsv", "# 注释行\n\n"
                + first + "\t数学学院\thttps://math.example.edu\t数分与拓扑\n"
                + second + "\t外国语学院\n");

        List<String> pool = CollegePoolBootstrap.seed(newDao(), file("colleges.tsv"));
        m_created.addAll(pool);

        assertEquals(Arrays.asList(first, second), pool, "池应按文件顺序");
        CourseDao reloaded = newDao();
        assertEquals("数学学院", reloaded.findCollege(first).getName());
        assertEquals("https://math.example.edu", reloaded.findCollege(first).getWebsite());
        assertEquals("数分与拓扑", reloaded.findCollege(first).getDescription());
        assertEquals("外国语学院", reloaded.findCollege(second).getName());
    }

    /** 重复导入是覆盖写：改名后重跑不会堆出第二行，也不会撞上学院名的唯一索引。 */
    @Test
    void reimportUpdatesInsteadOfDuplicating() throws IOException {
        String uuid = uuid("c");
        write("colleges.tsv", uuid + "\t老名字\n");
        m_created.add(CollegePoolBootstrap.seed(newDao(), file("colleges.tsv")).get(0));

        write("colleges.tsv", uuid + "\t新名字\n");
        List<String> pool = CollegePoolBootstrap.seed(newDao(), file("colleges.tsv"));

        assertEquals(Arrays.asList(uuid), pool);
        assertEquals("新名字", newDao().findCollege(uuid).getName(), "重跑应覆盖而不是新建");
        assertEquals(1L, countColleges(uuid), "同一个 uuid 只应有一行");
    }

    /** 空行、注释与缺列的坏行都跳过，不影响同一文件里其它学院的导入。 */
    @Test
    void blankCommentAndBrokenLinesAreSkipped() throws IOException {
        String good = uuid("d");
        write("colleges.tsv", "# 只有注释\n   \n只有一个字段\n\t缺 uuid\n"
                + good + "\t体育学院\n");

        List<String> pool = CollegePoolBootstrap.seed(newDao(), file("colleges.tsv"));
        m_created.addAll(pool);

        assertEquals(Arrays.asList(good), pool, "坏行应当被跳过而不是变成空 uuid 的学院");
    }

    /**
     * 写一份引导文件。
     *
     * @param name    文件名
     * @param content 内容（UTF-8）
     * @throws IOException 写入失败
     */
    private void write(String name, String content) throws IOException {
        Files.write(file(name).toPath(), content.getBytes(Charset.forName("UTF-8")));
    }

    /**
     * @param name 文件名
     * @return 临时目录下的文件
     */
    private File file(String name) {
        return new File(m_directory, name);
    }

    /**
     * 造一个从库里恢复过的课程目录（每个用例都用新的，确保断言读到的是库而不是本进程的内存）。
     *
     * @return 课程目录
     */
    private static CourseDao newDao() {
        return new CourseDao(new CourseStoreJdbc());
    }

    /**
     * @param tag 区分标记
     * @return 长度不超 36 的唯一 uuid
     */
    private String uuid(String tag) {
        return PREFIX + tag + Long.toHexString(System.nanoTime());
    }

    /**
     * 数一数某个学院的落库行数。
     *
     * @param uuid 学院 uuid
     * @return 行数
     */
    private static long countColleges(String uuid) {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = DbHelper.getConnection();
            statement = connection.prepareStatement(
                    "SELECT COUNT(*) FROM tblCollege WHERE clgUuid = ?");
            statement.setString(1, uuid);
            java.sql.ResultSet rows = statement.executeQuery();
            return rows.next() ? rows.getLong(1) : 0L;
        } catch (SQLException e) {
            return -1L;
        } finally {
            close(connection, statement);
        }
    }

    /**
     * 执行一条清理语句。
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
            // 清理失败不影响用例结论
        } finally {
            close(connection, statement);
        }
    }

    /**
     * 关掉连接与语句，异常忽略。
     *
     * @param connection 连接
     * @param statement  语句
     */
    private static void close(Connection connection, PreparedStatement statement) {
        try {
            if (statement != null) {
                statement.close();
            }
        } catch (SQLException e) {
            // 忽略
        }
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            // 忽略
        }
    }

    /**
     * 探测数据库是否可用。
     *
     * @return 能取到连接且建表脚本跑过返回 true
     */
    private static boolean databaseAvailable() {
        Connection connection = null;
        try {
            connection = DbHelper.getConnection();
            return connection != null && DatabaseAvailability.isReady();
        } catch (SQLException e) {
            return false;
        } catch (RuntimeException e) {
            return false;
        } finally {
            close(connection, null);
        }
    }
}
