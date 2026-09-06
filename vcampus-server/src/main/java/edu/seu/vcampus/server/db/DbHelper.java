package edu.seu.vcampus.server.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 数据库连接帮助类.
 *
 * <p>连接参数一律从环境变量读取（{@code DB_HOST} / {@code DB_PORT} / {@code DB_NAME} /
 * {@code DB_USER} / {@code DB_PASSWORD}），源码中不保存任何明文口令；CI 通过环境变量
 * 注入 MySQL service 的连接信息（见 ci.yml）。主机/端口/库名缺省时回退到本地开发值，
 * 但用户名与密码必须由环境变量提供，缺失时直接报错。
 *
 * <p>各模块 DAO 一律通过本类获取连接，不得自行调用 {@code DriverManager}。
 */
public class DbHelper {

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
    }

    /**
     * 读取环境变量，为空时返回缺省值.
     *
     * @param key 环境变量名
     * @param defaultValue 缺省值
     * @return 环境变量值，或缺省值
     */
    private static String env(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return value;
    }

    /**
     * 读取必填环境变量，缺失时抛出异常（避免在源码中写死口令）.
     *
     * @param key 环境变量名
     * @return 环境变量值
     */
    private static String requiredEnv(String key) {
        String value = System.getenv(key);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException(
                    "缺少数据库环境变量 " + key + "，请先配置后再启动（源码不保存明文口令）");
        }
        return value;
    }

    /**
     * 构造 JDBC 连接串.
     *
     * @return JDBC URL
     */
    public static String getUrl() {
        return "jdbc:mysql://" + env("DB_HOST", DEFAULT_HOST)
                + ":" + env("DB_PORT", DEFAULT_PORT)
                + "/" + env("DB_NAME", DEFAULT_NAME)
                + "?useSSL=false&serverTimezone=UTC&characterEncoding=utf8";
    }

    /**
     * 获取一个新的数据库连接，调用方负责关闭（建议使用 try-with-resources）.
     *
     * @return 数据库连接
     * @throws SQLException 连接失败时抛出
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(getUrl(),
                requiredEnv("DB_USER"), requiredEnv("DB_PASSWORD"));
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