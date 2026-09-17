package edu.seu.vcampus.server.db;

import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * 让服务端测试跑在自己的库上：{@code <开发库>_test}。
 *
 * <p>
 * 背景：账号库从「每个测试各自的临时文件」换成「全测试共用一个 MySQL 库」之后，集成测试开始互相 污染 ——
 * 上一个测试乃至上一次构建留下的账号，会撞上下一个测试。症状是「请求不合法，或数据已存在」、 「登录状态已失效」、「银行账户未开户」。根因是共享可变状态，不是某条 SQL 写错了。
 *
 * <p>
 * 做法：测试启动前把开发库的表结构整体复制到 {@code <开发库>_test}，再把整个测试 JVM 指向它
 * （{@link DbHelper#NAME_PROPERTY}）。开发库只被读取结构，测试写入一律落在副本里；每次运行都重建 副本，因此上一次运行的残留也不会影响这一次。
 *
 * <p>
 * 用 {@link LauncherSessionListener} 而不是某个 {@code @BeforeAll}：它保证在<b>任何</b>测试类之前
 * 执行一次，不依赖测试类的执行顺序，也不必给十几个测试类各加一个基类。
 *
 * <p>
 * 复制用 {@code CREATE TABLE ... LIKE}，所以开发库跑没跑过 {@code sql/vCampus-extend.sql} 会被自动
 * 继承，测试库与开发库结构永远一致。源库不存在、源库没建表、或当前账号没有 DDL 权限时，只打一行 告警并<b>保持原样</b>：测试仍按
 * {@link DatabaseAvailability} 的门控整体跳过，不会把构建卡在 本地环境问题上。
 */
public final class TestSchemaSetup implements LauncherSessionListener {

    /** 测试库名后缀：开发库 {@code vCampus} 对应测试库 {@code vCampus_test}。 */
    public static final String SUFFIX = "_test";

    /** 同一 JVM 内只准备一次。 */
    private static boolean s_prepared;

    @Override
    public void launcherSessionOpened(LauncherSession session) {
        prepare();
    }

    /** 准备测试库；重复调用只做第一次。 */
    static synchronized void prepare() {
        if (s_prepared) {
            return;
        }
        s_prepared = true;

        final String explicit = System.getProperty(DbHelper.NAME_PROPERTY);
        if (explicit != null && !explicit.trim().isEmpty()) {
            log("已显式指定库名 " + explicit.trim() + "，不再准备测试库");
            return;
        }
        final String url = DbHelper.getUrl();
        final String source = databaseNameOf(url);
        if (source.isEmpty() || source.endsWith(SUFFIX)) {
            log("无法从 JDBC URL 解析出开发库名，不再准备测试库");
            return;
        }
        final String target = source + SUFFIX;
        Connection connection = null;
        try {
            connection = DriverManager.getConnection(urlWithoutDatabase(url), DbHelper.getUser(),
                    DbHelper.getPassword());
            final String[] charset = charsetOf(connection, source);
            if (charset == null) {
                log("开发库 " + source + " 不存在，测试库未启用"
                        + "（集成测试将按门控跳过，请先执行 sql/ 下的建库脚本）");
                return;
            }
            recreate(connection, target, charset);
            final int tables = copyTables(connection, source, target);
            if (tables == 0) {
                log("开发库 " + source + " 里没有表，测试库未启用"
                        + "（集成测试将按门控跳过，请先执行 sql/ 下的建库脚本）");
                return;
            }
            System.setProperty(DbHelper.NAME_PROPERTY, target);
            log("本次测试使用独立库 " + target + "（从 " + source + " 复制了 " + tables + " 张表，"
                    + "开发库只读结构）");
        } catch (SQLException e) {
            log("准备测试库失败，本次测试退回 " + source + "：" + e.getMessage());
        } catch (RuntimeException e) {
            log("准备测试库失败，本次测试退回 " + source + "：" + e.getMessage());
        } finally {
            close(connection);
        }
    }

    /**
     * 从 JDBC URL 里取出库名。
     *
     * @param url JDBC URL
     * @return 库名；解析不出时返回空串
     */
    static String databaseNameOf(String url) {
        final int scheme = url.indexOf("//");
        final int slash = url.indexOf('/', scheme + 2);
        if (scheme < 0 || slash < 0) {
            return "";
        }
        final int end = url.indexOf('?', slash);
        final String name = end < 0 ? url.substring(slash + 1) : url.substring(slash + 1, end);
        return name.trim();
    }

    /**
     * 去掉 JDBC URL 里的库名，只保留查询参数（建库语句不能带着库名连接）。
     *
     * @param url JDBC URL
     * @return 不带库名的 JDBC URL
     */
    static String urlWithoutDatabase(String url) {
        final int scheme = url.indexOf("//");
        final int slash = url.indexOf('/', scheme + 2);
        if (scheme < 0 || slash < 0) {
            return url;
        }
        final int query = url.indexOf('?', slash);
        return url.substring(0, slash + 1) + (query < 0 ? "" : url.substring(query));
    }

    /**
     * 丢弃并重建测试库，字符集与排序规则照抄开发库。
     *
     * @param connection 连接（不指定库）
     * @param target     测试库名
     * @param charset    {@code [字符集, 排序规则]}
     * @throws SQLException 建库失败
     */
    private static void recreate(Connection connection, String target, String[] charset)
            throws SQLException {
        execute(connection, "DROP DATABASE IF EXISTS `" + target + "`");
        execute(connection, "CREATE DATABASE `" + target + "` DEFAULT CHARACTER SET "
                + charset[0] + " COLLATE " + charset[1]);
    }

    /**
     * 查开发库的字符集与排序规则。
     *
     * @param connection 连接
     * @param schema     库名
     * @return {@code [字符集, 排序规则]}；库不存在时返回 null
     * @throws SQLException 查询失败
     */
    private static String[] charsetOf(Connection connection, String schema) throws SQLException {
        PreparedStatement statement = null;
        ResultSet rows = null;
        try {
            statement = connection.prepareStatement("SELECT DEFAULT_CHARACTER_SET_NAME,"
                    + " DEFAULT_COLLATION_NAME FROM information_schema.SCHEMATA"
                    + " WHERE SCHEMA_NAME = ?");
            statement.setString(1, schema);
            rows = statement.executeQuery();
            if (!rows.next()) {
                return null;
            }
            return new String[] { rows.getString(1), rows.getString(2) };
        } finally {
            close(rows);
            close(statement);
        }
    }

    /**
     * 把源库的全部基本表按结构复制到测试库。
     *
     * @param connection 连接
     * @param source     源库名
     * @param target     测试库名
     * @return 复制的表数量
     * @throws SQLException 复制失败
     */
    private static int copyTables(Connection connection, String source, String target)
            throws SQLException {
        final List<String> tables = baseTablesOf(connection, source);
        for (String table : tables) {
            execute(connection, "CREATE TABLE `" + target + "`.`" + table + "` LIKE `"
                    + source + "`.`" + table + "`");
        }
        return tables.size();
    }

    /**
     * 列出库里的全部基本表。
     *
     * @param connection 连接
     * @param schema     库名
     * @return 表名列表
     * @throws SQLException 查询失败
     */
    private static List<String> baseTablesOf(Connection connection, String schema)
            throws SQLException {
        final List<String> names = new ArrayList<String>();
        PreparedStatement statement = null;
        ResultSet rows = null;
        try {
            statement = connection.prepareStatement("SELECT TABLE_NAME FROM"
                    + " information_schema.TABLES WHERE TABLE_SCHEMA = ?"
                    + " AND TABLE_TYPE = 'BASE TABLE' ORDER BY TABLE_NAME");
            statement.setString(1, schema);
            rows = statement.executeQuery();
            while (rows.next()) {
                names.add(rows.getString(1));
            }
        } finally {
            close(rows);
            close(statement);
        }
        return names;
    }

    /**
     * 执行一条 DDL。
     *
     * @param connection 连接
     * @param sql        语句
     * @throws SQLException 执行失败
     */
    private static void execute(Connection connection, String sql) throws SQLException {
        Statement statement = null;
        try {
            statement = connection.createStatement();
            statement.execute(sql);
        } finally {
            close(statement);
        }
    }

    /**
     * 静默收尾。
     *
     * @param closeable 待关闭对象；可为 null
     */
    private static void close(AutoCloseable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception e) {
            log("收尾失败：" + e.getMessage());
        }
    }

    /**
     * 打一行测试库相关的提示，便于在构建日志里看清本次到底连了哪个库。
     *
     * @param message 提示内容
     */
    private static void log(String message) {
        System.out.println("[TestSchema] " + message);
    }
}
