package edu.seu.vcampus.server.db;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

/**
 * 数据库连接帮助类.
 *
 * <p>
 * 连接参数优先从 {@code db.properties} 读取(本地开发)， 若文件不存在或配置为空则回退到环境变量(CI/生产环境)。 这样既方便本地开发，又能在 CI
 * 中通过环境变量注入配置。
 *
 * <p>
 * 各模块 DAO 一律通过本类获取连接，不得自行调用 {@code DriverManager}。
 */
public class DbHelper {

    /** 数据库配置。 */
    private static final Properties DB_CONFIG = new Properties();

    /** 数据库主机缺省值。 */
    private static final String DEFAULT_HOST = "localhost";

    /** 数据库端口缺省值。 */
    private static final String DEFAULT_PORT = "3306";

    /** 数据库名缺省值。 */
    private static final String DEFAULT_NAME = "vCampus";

    /**
     * 库名覆盖的系统属性（{@code -Dvcampus.db.name=...}），优先级高于 {@code db.properties} 与 {@code DB_NAME}。
     *
     * <p>
     * 它只决定<b>连哪一个库</b>，不改变任何行为 —— 服务端测试跑在自己的库上（{@code <开发库>_test}， 见测试侧的
     * TestSchemaSetup），免得测试数据落进开发库、或被开发库里上一次跑剩下的 账号影响。生产启动不设置这个属性。
     */
    public static final String NAME_PROPERTY = "vcampus.db.name";

    /**
     * 缺省连接参数（拼 URL 时接在库名后）。
     *
     * <p>
     * allowPublicKeyRetrieval：MySQL 8 默认 caching_sha2_password，首次连接要取服务端公钥， useSSL=false 时不显式打开就报
     * "Public Key Retrieval is not allowed"。这是开发库的取法， 生产环境应改走 SSL。
     */
    private static final String PARAMETERS = "?useSSL=false&allowPublicKeyRetrieval=true"
            + "&serverTimezone=Asia/Shanghai&characterEncoding=utf8";

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        // 尝试加载 db.properties
        try (InputStream input = DbHelper.class.getClassLoader()
                .getResourceAsStream("db.properties")) {
            if (input != null) {
                DB_CONFIG.load(input);
                System.out.println("[DbHelper] 已加载 db.properties");
            } else {
                System.out.println("[DbHelper] db.properties 不存在，将使用环境变量");
            }
        } catch (IOException e) {
            System.err.println("[DbHelper] 加载 db.properties 失败，将使用环境变量");
            e.printStackTrace();
        }
    }

    /**
     * 读取配置值，优先级：db.properties > 环境变量 > 默认值.
     *
     * @param propKey      properties文件中的键
     * @param envKey       环境变量名
     * @param defaultValue 缺省值
     * @return 配置值
     */
    private static String getConfig(String propKey, String envKey, String defaultValue) {
        // 1. 优先读取 db.properties
        String value = DB_CONFIG.getProperty(propKey);
        if (value != null && !value.trim().isEmpty()) {
            return value.trim();
        }

        // 2. 回退到环境变量
        value = System.getenv(envKey);
        if (value != null && !value.trim().isEmpty()) {
            return value.trim();
        }

        // 3. 使用默认值
        return defaultValue;
    }

    /**
     * 构造 JDBC 连接串.
     *
     * <p>
     * 库名覆盖（{@link #NAME_PROPERTY}）在<b>最后</b>统一施加：无论 URL 是配置里的完整 {@code db.url} 还是按 host/port/name
     * 拼出来的，都换成指定的库。早先这个覆盖只写在拼接分支里， 而本地 {@code db.properties} 写了完整的 {@code db.url} —— 于是覆盖被静默忽略，测试照样
     * 写进开发库。
     *
     * @return JDBC URL
     */
    public static String getUrl() {
        String url = DB_CONFIG.getProperty("db.url");
        if (url == null || url.trim().isEmpty()) {
            // 拼接：主机 / 端口 / 库名各自按「配置 > 环境变量 > 缺省」取值
            url = "jdbc:mysql://" + getConfig("db.host", "DB_HOST", DEFAULT_HOST) + ":"
                    + getConfig("db.port", "DB_PORT", DEFAULT_PORT) + "/"
                    + getConfig("db.name", "DB_NAME", DEFAULT_NAME) + PARAMETERS;
        }
        url = url.trim();

        String name = System.getProperty(NAME_PROPERTY);
        if (name != null && !name.trim().isEmpty()) {
            return withDatabase(url, name.trim());
        }
        return url;
    }

    /**
     * 把 JDBC URL 的库名换成指定值（保留主机、端口与查询参数）。
     *
     * @param url  JDBC URL
     * @param name 目标库名
     * @return 换成目标库名的 URL；URL 结构无法识别时原样返回
     */
    private static String withDatabase(String url, String name) {
        int scheme = url.indexOf("//");
        int slash = scheme < 0 ? -1 : url.indexOf('/', scheme + 2);
        if (slash < 0) {
            return url;
        }
        int query = url.indexOf('?', slash);
        String tail = query < 0 ? "" : url.substring(query);
        return url.substring(0, slash + 1) + name + tail;
    }

    /**
     * 获取数据库用户名.
     *
     * @return 用户名
     */
    public static String getUser() {
        String user = getConfig("db.user", "DB_USER", null);
        if (user == null || user.isEmpty()) {
            throw new IllegalStateException(
                    "缺少数据库用户名配置：请在 db.properties 中设置 db.user 或设置环境变量 DB_USER");
        }
        return user;
    }

    /**
     * 获取数据库密码.
     *
     * @return 密码
     */
    public static String getPassword() {
        // 密码允许为空字符串
        return getConfig("db.password", "DB_PASSWORD", "");
    }

    /**
     * 获取一个新的数据库连接，调用方负责关闭（建议使用 try-with-resources）.
     *
     * @return 数据库连接
     * @throws SQLException 连接失败时抛出
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(getUrl(), getUser(), getPassword());
    }

    /**
     * 校验数据库可用；不可用直接抛异常.
     *
     * <p>
     * 服务器不提供内存/文件回退：缺库属于配置错误，应当立刻失败，而不是静默降级到一条 「看起来能跑、重启即失」的路径。那种降级会让人以为系统正常，直到重启才发现什么都没留下。
     *
     * @throws DatabaseAccessException 数据库不可用
     */
    public static void requireAvailable() {
        Connection connection = null;
        try {
            connection = getConnection();
            if (!connection.isValid(2)) {
                throw new DatabaseAccessException("数据库连接无效",
                        new SQLException("连接校验未通过"));
            }
        } catch (SQLException e) {
            throw new DatabaseAccessException("数据库不可用，请检查 db.properties 或 DB_* 环境变量", e);
        } finally {
            JdbcSupport.closeQuietly(connection);
        }
    }

    /**
     * 检查表是否为空.
     *
     * @param tableName 要检查的表名
     * @return 表是否为空
     */
    public static boolean isTableEmpty(String tableName) {
        String sql = "SELECT COUNT(*) FROM " + tableName;
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1) == 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
