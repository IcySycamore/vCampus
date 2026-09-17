package edu.seu.vcampus.server.student;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;
import javax.sql.DataSource;

/**
 * 学籍模块的数据库连接来源：把「连哪个库、用什么账号」与 DAO 代码分开。
 *
 * <p>
 * 与图书馆模块一样实现标准的 {@link DataSource}，好处是 JDBC DAO 只依赖这个接口，测试可以换成
 * 指向别的库的实现，而不用改 DAO 一行。
 *
 * <p>
 * <b>配置来源按优先级查找</b>（先找到的先用）：
 * <ol>
 * <li>系统属性 {@code vcampus.db.url} / {@code vcampus.db.user} / {@code vcampus.db.password}
 * （便于单次运行覆盖）；</li>
 * <li>环境变量 {@code DB_HOST} / {@code DB_PORT} / {@code DB_NAME} / {@code DB_USER} /
 * {@code DB_PASSWORD}——CI 的构建步骤已经导出这五个，所以流水线上不需要额外配置；</li>
 * <li>类路径下的 {@code db.properties}（团队约定文件）；</li>
 * <li>都没有时用本机默认：{@code 127.0.0.1:3306/vCampus}、{@code root}、空口令。</li>
 * </ol>
 *
 * <p>
 * 库名默认写 {@code vCampus}（与 {@code sql/vCampus.sql} 的 {@code CREATE DATABASE} 一致，大小写
 * 敏感：Linux 上的 MySQL 把 {@code vcampus} 与 {@code vCampus} 当两个库）。
 *
 * <p>
 * <b>不做连接池</b>：每次 {@link #getConnection()} 现开一条连接，与 {@code DbHelper} 的用法一致，
 * 也够校园项目的并发量；将来要池化只需替换本类的实现，DAO 不用动。
 */
public class StudentDataSource implements DataSource {

    /** 系统属性 / 环境变量名。 */
    private static final String[] URL_KEYS = { "vcampus.db.url", "DB_URL" };

    /** 用户名键。 */
    private static final String[] USER_KEYS = { "vcampus.db.user", "DB_USER" };

    /** 口令键（环境变量通常是 DB_PASSWORD）。 */
    private static final String[] PASSWORD_KEYS = { "vcampus.db.password", "DB_PASSWORD" };

    /** 主机键。 */
    private static final String[] HOST_KEYS = { "vcampus.db.host", "DB_HOST" };

    /** 端口键。 */
    private static final String[] PORT_KEYS = { "vcampus.db.port", "DB_PORT" };

    /** 库名键。 */
    private static final String[] NAME_KEYS = { "vcampus.db.name", "DB_NAME" };

    /** 兜底主机。 */
    private static final String DEFAULT_HOST = "127.0.0.1";

    /** 兜底端口。 */
    private static final String DEFAULT_PORT = "3306";

    /** 兜底库名（与建库脚本一致，注意大小写）。 */
    private static final String DEFAULT_NAME = "vCampus";

    /** 最终连接串。 */
    private final String m_url;

    /** 最终用户名。 */
    private final String m_user;

    /** 最终口令。 */
    private final String m_password;

    /** 日志输出（DataSource 接口要求）。 */
    private PrintWriter m_log_writer;

    /** 登录超时（DataSource 接口要求）。 */
    private int m_login_timeout;

    /** 用解析出来的配置建连接来源。 */
    public StudentDataSource() {
        Properties file = loadProperties();
        String url = firstOf(URL_KEYS);
        if (url == null) {
            url = file.getProperty("db.url");
        }
        if (url == null) {
            url = "jdbc:mysql://" + valueOf(HOST_KEYS, file, "db.host", DEFAULT_HOST) + ":"
                    + valueOf(PORT_KEYS, file, "db.port", DEFAULT_PORT) + "/"
                    + valueOf(NAME_KEYS, file, "db.name", DEFAULT_NAME)
                    + "?useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8";
        }
        String user = firstOf(USER_KEYS);
        if (user == null) {
            user = file.getProperty("db.username", "root");
        }
        String password = firstOf(PASSWORD_KEYS);
        if (password == null) {
            password = file.getProperty("db.password", "");
        }
        this.m_url = url;
        this.m_user = user;
        this.m_password = password;
        loadDriver();
    }

    /**
     * 取一条新连接。
     *
     * @return 数据库连接（由调用方负责关闭，推荐 try-with-resources）
     * @throws SQLException 连不上或配置无效
     */
    @Override
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(m_url, m_user, m_password);
    }

    /**
     * 取一条新连接（用户名口令由本类配置决定，忽略这里的参数）。
     *
     * @param username 忽略
     * @param password 忽略
     * @return 数据库连接
     * @throws SQLException 连不上
     */
    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return getConnection();
    }

    /** @return 当前连接串（不含口令，便于日志） */
    public String getUrl() {
        return m_url;
    }

    /** @return 日志输出 */
    @Override
    public PrintWriter getLogWriter() {
        return m_log_writer;
    }

    /**
     * 设置日志输出。
     *
     * @param out 日志输出
     */
    @Override
    public void setLogWriter(PrintWriter out) {
        this.m_log_writer = out;
    }

    /**
     * 设置登录超时。
     *
     * @param seconds 秒
     */
    @Override
    public void setLoginTimeout(int seconds) {
        this.m_login_timeout = seconds;
    }

    /** @return 登录超时（秒） */
    @Override
    public int getLoginTimeout() {
        return m_login_timeout;
    }

    /**
     * 取父日志器。
     *
     * @return 不支持
     * @throws SQLFeatureNotSupportedException 恒抛
     */
    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException("本实现不使用 java.util.logging");
    }

    /**
     * 解包。
     *
     * @param iface 目标接口
     * @param &lt;T&gt; 目标类型
     * @return 本对象
     * @throws SQLException 类型不匹配
     */
    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface != null && iface.isInstance(this)) {
            return iface.cast(this);
        }
        throw new SQLException("不是 " + iface + " 的包装");
    }

    /**
     * 是否为本接口的包装。
     *
     * @param iface 目标接口
     * @return 恒 false
     */
    @Override
    public boolean isWrapperFor(Class<?> iface) {
        return false;
    }

    /**
     * 按顺序取第一个非空配置。
     *
     * @param keys 系统属性名与环境变量名（同名同义）
     * @return 值；都没有返回 null
     */
    private static String firstOf(String[] keys) {
        int index = 0;
        while (index < keys.length) {
            String value = System.getProperty(keys[index]);
            if (value == null) {
                value = System.getenv(keys[index]);
            }
            if (value != null && value.trim().length() > 0) {
                return value.trim();
            }
            index = index + 1;
        }
        return null;
    }

    /**
     * 取一个配置项：先查系统属性/环境变量，再查 db.properties，最后用兜底值。
     *
     * @param keys 系统属性与环境变量名
     * @param file db.properties 内容
     * @param fileKey db.properties 里的键名
     * @param fallback 兜底值
     * @return 值
     */
    private static String valueOf(String[] keys, Properties file, String fileKey,
            String fallback) {
        String value = firstOf(keys);
        if (value == null) {
            value = file.getProperty(fileKey);
        }
        return value == null ? fallback : value;
    }

    /**
     * 读类路径下的 db.properties；读不到返回空表。
     *
     * @return 配置内容
     */
    private static Properties loadProperties() {
        Properties properties = new Properties();
        InputStream stream = StudentDataSource.class.getClassLoader()
                .getResourceAsStream("db.properties");
        if (stream == null) {
            return properties;
        }
        try {
            properties.load(stream);
        } catch (IOException ignored) {
            // 配置文件坏了不该让服务起不来：退回到默认配置，连不上时错误信息里会有 URL
        } finally {
            try {
                stream.close();
            } catch (IOException ignored) {
                // 关闭失败无后续处理
            }
        }
        return properties;
    }

    /** 显式加载 MySQL 驱动，缺驱动时给一句能看懂的提示。 */
    private static void loadDriver() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException exception) {
            System.err.println("找不到 MySQL 驱动（mysql-connector-j），学籍模块无法使用数据库存储");
        }
    }
}
