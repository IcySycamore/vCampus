package edu.seu.vcampus.server.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

/**
 * JDBC 实现的公共支撑：取连接、关资源、跑事务。
 *
 * <p>
 * 各 {@code *StoreJdbc} / {@code *DaoJdbc} 只应保留自己的 SQL 与列映射， 连接生命周期与资源关闭一律走本类 —— 此前
 * {@code closeQuietly} 在 11 个类里各写了一份，{@code blank} 3 份， 事务的 setAutoCommit/commit/rollback
 * 也有两套各自实现。同一段样板抄多遍的代价不是行数， 而是「哪个实现会自己开连接」这件事必须读实现才知道。
 *
 * <p>
 * 本类也是事务的唯一入口：{@link #inTransaction} 保证一条业务动作的全部 SQL 跑在<b>同一条</b>连接上。 图书馆此前用内存 {@code DataSource}
 * 假装有事务，上层 {@code setAutoCommit(false)} 落在 no-op 上、 下层 DAO 各自 auto-commit，
 * 跨表写在中途失败会留半截状态且不报错；走本类即可避免这类接缝错位。
 */
public final class JdbcSupport {

    private JdbcSupport() {
    }

    /**
     * 事务体：在给定连接上执行一组 SQL。
     *
     * @param <T> 返回值类型
     */
    public interface Work<T> {
        /**
         * 执行事务内的操作。
         *
         * @param connection 事务连接
         * @return 操作结果
         * @throws SQLException 任一 SQL 失败即整体回滚
         */
        T run(Connection connection) throws SQLException;
    }

    /**
     * 取一条新连接（调用方负责关闭）。
     *
     * @return 连接
     */
    public static Connection openConnection() {
        try {
            return DbHelper.getConnection();
        } catch (SQLException e) {
            throw new DatabaseAccessException("获取数据库连接失败", e);
        }
    }

    /**
     * 取连接：调用方已提供则直接沿用，否则新开一条。
     *
     * <p>
     * 用于兼容「上层已经开好事务、把连接传下来」与「本层独立调用」两种入口， 避免出现「上层以为在事务里、下层却另开连接」的错位。
     *
     * @param provided 调用方传入的连接；可为 null
     * @return 可用连接
     */
    public static Connection openIfAbsent(Connection provided) {
        return provided != null ? provided : openConnection();
    }

    /**
     * 在一条连接上跑完一个工作单元，成功提交、失败回滚。
     *
     * @param <T>  返回值类型
     * @param work 工作单元
     * @return 工作单元的返回值
     * @throws DatabaseAccessException SQL 失败或回滚后抛出
     */
    public static <T> T inTransaction(Work<T> work) {
        Connection connection = openConnection();
        try {
            boolean original = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                T result = work.run(connection);
                connection.commit();
                return result;
            } catch (SQLException e) {
                rollbackQuietly(connection);
                throw new DatabaseAccessException("事务执行失败", e);
            } catch (RuntimeException e) {
                rollbackQuietly(connection);
                throw e;
            } finally {
                setAutoCommitQuietly(connection, original);
            }
        } catch (SQLException e) {
            throw new DatabaseAccessException("事务执行失败", e);
        } finally {
            closeQuietly(connection);
        }
    }

    /**
     * 关闭资源，忽略关闭期异常。
     *
     * @param closeable 可关闭对象；可为 null
     */
    public static void closeQuietly(AutoCloseable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception ignored) {
            // 关闭失败不影响业务结果
        }
    }

    /**
     * 判断文本是否为空。
     *
     * @param text 文本
     * @return 为 null 或全空白返回 true
     */
    public static boolean blank(String text) {
        return text == null || text.trim().isEmpty();
    }

    /**
     * 按需写入可空文本列。
     *
     * @param statement 预编译语句
     * @param index     参数下标
     * @param value     值；null 时写 NULL
     * @throws SQLException 设置失败
     */
    public static void setNullable(PreparedStatement statement, int index, String value)
            throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.VARCHAR);
        } else {
            statement.setString(index, value);
        }
    }

    /**
     * 按需写入可空整数列。
     *
     * @param statement 预编译语句
     * @param index     参数下标
     * @param value     值；null 时写 NULL
     * @throws SQLException 设置失败
     */
    public static void setNullableLong(PreparedStatement statement, int index, Long value)
            throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.BIGINT);
        } else {
            statement.setLong(index, value.longValue());
        }
    }

    /**
     * 回滚并保留原始异常。
     *
     * @param connection 连接
     */
    private static void rollbackQuietly(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
            // 回滚失败时原始异常更重要，不覆盖它
        }
    }

    /**
     * 恢复 autoCommit 设置。
     *
     * @param connection 连接
     * @param value      原值
     */
    private static void setAutoCommitQuietly(Connection connection, boolean value) {
        try {
            connection.setAutoCommit(value);
        } catch (SQLException ignored) {
            // 连接即将关闭，恢复失败无影响
        }
    }
}
