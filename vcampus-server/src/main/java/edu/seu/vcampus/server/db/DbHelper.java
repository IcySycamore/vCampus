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
 * <p>连接参数优先从 {@code db.properties} 读取(本地开发)，
 * 若文件不存在或配置为空则回退到环境变量(CI/生产环境)。
 * 这样既方便本地开发，又能在 CI 中通过环境变量注入配置。
 *
 * <p>各模块 DAO 一律通过本类获取连接，不得自行调用 {@code DriverManager}。
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
     * @param propKey properties文件中的键
     * @param envKey 环境变量名
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
     * @return JDBC URL
     */
    public static String getUrl() {
        // 如果 db.properties 中有完整的 db.url，直接使用
        String url = DB_CONFIG.getProperty("db.url");
        if (url != null && !url.trim().isEmpty()) {
            return url.trim();
        }

        // 否则从配置或环境变量拼接
        String host = getConfig("db.host", "DB_HOST", DEFAULT_HOST);
        String port = getConfig("db.port", "DB_PORT", DEFAULT_PORT);
        String name = getConfig("db.name", "DB_NAME", DEFAULT_NAME);

        return "jdbc:mysql://" + host + ":" + port + "/" + name
                + "?useSSL=false&allowPublicKeyRetrieval=true"
                + "&serverTimezone=Asia/Shanghai&characterEncoding=utf8";
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
