package edu.seu.vcampus.server.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC 查询的公共支撑：把「开连接 → 绑定参数 → 遍历结果集 → 关资源」这套样板收在一处。
 *
 * <p>
 * 之前通用层只有 {@link JdbcSupport}（连接、事务、可空参数绑定），没有查询支持，于是每个 DAO 自己写 一遍 try/finally 与 while(rows.next())
 * —— 代价不是行数，而是「参数绑定漏了一个」「结果集没关」 这类错误要读十几份实现才能发现。本类补上这一层：**按需查询的 DAO 直接用这几个方法**， 只保留自己的 SQL 与行映射。
 *
 * <p>
 * 与 {@link JdbcSupport#inTransaction} 的分工：需要「同一业务动作的多条 SQL 跑在同一条连接上」时用 那边的事务体；单条查询与单条写入用本类即可。
 */
public final class JdbcQuery {

    private JdbcQuery() {
    }

    /**
     * 行映射器：把结果集当前行映射成一个对象。
     *
     * @param <T> 结果类型
     */
    public interface RowMapper<T> {
        /**
         * 映射当前行。
         *
         * @param rows 已定位到某行的结果集
         * @return 映射结果
         * @throws SQLException 读取失败
         */
        T map(ResultSet rows) throws SQLException;
    }

    /**
     * 查询列表。
     *
     * @param <T>    结果类型
     * @param sql    带 {@code ?} 的查询语句
     * @param mapper 行映射器
     * @param params 参数，按顺序绑定
     * @return 结果列表，查不到返回空列表（不返回 null）
     */
    public static <T> List<T> list(String sql, RowMapper<T> mapper, Object... params) {
        List<T> found = new ArrayList<T>();
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rows = null;
        try {
            connection = JdbcSupport.openConnection();
            statement = connection.prepareStatement(sql);
            bind(statement, params);
            rows = statement.executeQuery();
            while (rows.next()) {
                found.add(mapper.map(rows));
            }
            return found;
        } catch (SQLException e) {
            throw new DatabaseAccessException("查询失败: " + sql, e);
        } finally {
            JdbcSupport.closeQuietly(rows);
            JdbcSupport.closeQuietly(statement);
            JdbcSupport.closeQuietly(connection);
        }
    }

    /**
     * 查询单条，取第一条。
     *
     * @param <T>    结果类型
     * @param sql    带 {@code ?} 的查询语句
     * @param mapper 行映射器
     * @param params 参数，按顺序绑定
     * @return 第一条结果；查不到返回 null
     */
    public static <T> T one(String sql, RowMapper<T> mapper, Object... params) {
        List<T> found = list(sql, mapper, params);
        return found.isEmpty() ? null : found.get(0);
    }

    /**
     * 查询单个数值列（{@code COUNT}、{@code MAX} 之类）。
     *
     * @param sql    带 {@code ?} 的查询语句
     * @param params 参数，按顺序绑定
     * @return 数值；查不到或列为 NULL 返回 null
     */
    public static Long number(String sql, Object... params) {
        return one(sql, new RowMapper<Long>() {
            @Override
            public Long map(ResultSet rows) throws SQLException {
                long value = rows.getLong(1);
                return rows.wasNull() ? null : Long.valueOf(value);
            }
        }, params);
    }

    /**
     * 执行一条写语句（{@code INSERT} / {@code UPDATE} / {@code DELETE}）。
     *
     * @param sql    带 {@code ?} 的语句
     * @param params 参数，按顺序绑定
     * @return 受影响行数
     */
    public static int update(String sql, Object... params) {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = JdbcSupport.openConnection();
            statement = connection.prepareStatement(sql);
            bind(statement, params);
            return statement.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseAccessException("写入失败: " + sql, e);
        } finally {
            JdbcSupport.closeQuietly(statement);
            JdbcSupport.closeQuietly(connection);
        }
    }

    /**
     * 按顺序绑定参数；null 按 {@code VARCHAR} 写空值。
     *
     * @param statement 预编译语句
     * @param params    参数
     * @throws SQLException 绑定失败
     */
    private static void bind(PreparedStatement statement, Object[] params) throws SQLException {
        for (int index = 0; index < params.length; index++) {
            Object value = params[index];
            if (value == null) {
                statement.setNull(index + 1, Types.VARCHAR);
            } else {
                statement.setObject(index + 1, value);
            }
        }
    }
}
