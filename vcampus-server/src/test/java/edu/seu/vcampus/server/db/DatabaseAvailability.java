package edu.seu.vcampus.server.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 真库集成测试的环境门控：库连得上<b>且</b>建表脚本已经跑过。
 *
 * <p>
 * 只判断「能否连上」是不够的 —— 连上但没建表时，测试不会跳过，而是抛一串 {@code Table 'vCampus.tblXxx' doesn't exist} 把构建打红。CI 的
 * mysql service 只负责建出 空库，建表脚本由 workflow 单独执行；两者有一边落空就会踩这个坑。
 *
 * <p>
 * 因此这里额外查一次 {@code information_schema}：表不在就当作「数据库不可用」，让各测试按 ADR-0005 的约定整体跳过，而不是把别人的 PR 卡在本地环境问题上。
 *
 * <p>
 * 查的是<b>当前</b>库。服务端测试的库由 {@link TestSchemaSetup} 在测试启动前指向 {@code <开发库>_test}，所以这里探测的其实是那张副本 ——
 * 与运行时各 DAO 实际写的库一致， 这正是关键：门控必须探测测试真正要写的那个库。
 */
public final class DatabaseAvailability {

    /** 探测「建库脚本是否跑过」用的表：账户表是其它表外键的目标，也是服务端最先用到的一张。 */
    private static final String PROBE_TABLE = "tblUserCredential";

    private DatabaseAvailability() {
    }

    /**
     * 数据库是否可用于集成测试。
     *
     * @return 连得上且建库脚本已执行返回 true
     */
    public static boolean isReady() {
        Connection connection = null;
        try {
            connection = DbHelper.getConnection();
            return hasTable(connection, PROBE_TABLE);
        } catch (SQLException e) {
            return false;
        } catch (RuntimeException e) {
            return false;
        } finally {
            close(connection);
        }
    }

    /**
     * 指定表是否都已建好。
     *
     * @param tables 表名
     * @return 全部存在返回 true；连不上或任一张缺失返回 false
     */
    public static boolean tablesExist(String... tables) {
        Connection connection = null;
        try {
            connection = DbHelper.getConnection();
            for (String table : tables) {
                if (!hasTable(connection, table)) {
                    return false;
                }
            }
            return true;
        } catch (SQLException e) {
            return false;
        } catch (RuntimeException e) {
            return false;
        } finally {
            close(connection);
        }
    }

    /**
     * 查 information_schema 判断表是否存在（只看当前库）。
     *
     * @param connection 连接
     * @param table      表名
     * @return 存在返回 true
     * @throws SQLException 查询失败
     */
    private static boolean hasTable(Connection connection, String table) throws SQLException {
        PreparedStatement statement = null;
        ResultSet rows = null;
        try {
            statement = connection.prepareStatement("SELECT COUNT(*) FROM information_schema.TABLES"
                    + " WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?");
            statement.setString(1, table);
            rows = statement.executeQuery();
            return rows.next() && rows.getInt(1) > 0;
        } finally {
            closeQuietly(rows);
            closeQuietly(statement);
        }
    }

    /**
     * 安静关闭资源。
     *
     * @param closeable 可关闭对象；可为 null
     */
    private static void closeQuietly(AutoCloseable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception ignored) {
            // 关闭失败不影响判断结果
        }
    }

    /**
     * 安静关闭连接。
     *
     * @param connection 连接；可为 null
     */
    private static void close(Connection connection) {
        closeQuietly(connection);
    }
}
