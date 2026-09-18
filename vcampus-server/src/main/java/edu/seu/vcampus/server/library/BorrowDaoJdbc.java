package edu.seu.vcampus.server.library;

import edu.seu.vcampus.common.library.entity.BorrowRecord;
import edu.seu.vcampus.common.library.entity.PopularBorrow;
import edu.seu.vcampus.server.db.DatabaseAccessException;
import edu.seu.vcampus.server.db.DbHelper;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static edu.seu.vcampus.server.db.JdbcSupport.closeQuietly;

/**
 * 【MySQL 版】借阅记录数据访问：落表 {@code tblBorrow}（含扩展列：续借次数与罚金三项）。
 *
 * <p>
 * 表结构见 {@code sql/vCampus.sql} 的 {@code tblBorrow}：用户标识存的是<b>账户 uuid</b>（该表的 {@code uId} 是
 * {@code VARCHAR(64)}，而不是原设计里的 8 位业务号）。
 *
 * <p>
 * 全部状态变更都写成<b>带前置条件的单条 UPDATE</b>（未归还、未缴罚金等），并发重复归还/重复缴费 至多生效一次；这正是接口注释要求的原子语义，不必在业务层加锁。
 */
public class BorrowDaoJdbc implements BorrowDao {

    /** 查询列清单。 */
    private static final String COLUMNS = "rId, uId, bIsbn, bTitle, rBorrowedAt, rDueAt,"
            + " rReturnedAt, rRenewalCount, rFineAmount, rFinePaid, rFineTransactionId";

    @Override
    public List<PopularBorrow> findPopular(int limit) {
        List<PopularBorrow> ranked = new ArrayList<PopularBorrow>();
        if (limit <= 0) {
            return ranked;
        }
        String sql = "SELECT bIsbn, bTitle, COUNT(*) AS borrowCount FROM tblBorrow"
                + " GROUP BY bIsbn, bTitle ORDER BY borrowCount DESC, bTitle ASC LIMIT ?";
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rows = null;
        try {
            connection = DbHelper.getConnection();
            statement = connection.prepareStatement(sql);
            statement.setInt(1, limit);
            rows = statement.executeQuery();
            while (rows.next()) {
                ranked.add(new PopularBorrow(rows.getString("bIsbn"), rows.getString("bTitle"),
                        rows.getInt("borrowCount")));
            }
            return ranked;
        } catch (SQLException e) {
            throw new DatabaseAccessException("查询热门借阅失败", e);
        } finally {
            closeQuietly(rows);
            closeQuietly(statement);
            closeQuietly(connection);
        }
    }

    @Override
    public List<BorrowRecord> findByUser(String userId) {
        List<BorrowRecord> found = new ArrayList<BorrowRecord>();
        if (userId == null) {
            return found;
        }
        String sql = "SELECT " + COLUMNS + " FROM tblBorrow WHERE uId = ?"
                + " ORDER BY rBorrowedAt DESC";
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rows = null;
        try {
            connection = DbHelper.getConnection();
            statement = connection.prepareStatement(sql);
            statement.setString(1, userId);
            rows = statement.executeQuery();
            while (rows.next()) {
                found.add(toRecord(rows));
            }
            Collections.sort(found, new Comparator<BorrowRecord>() {
                @Override
                public int compare(BorrowRecord left, BorrowRecord right) {
                    long leftTime = left.getBorrowedAt() == null
                            ? 0L
                            : left.getBorrowedAt().getTime();
                    long rightTime = right.getBorrowedAt() == null
                            ? 0L
                            : right.getBorrowedAt().getTime();
                    return rightTime > leftTime ? 1 : (rightTime < leftTime ? -1 : 0);
                }
            });
            return found;
        } catch (SQLException e) {
            throw new DatabaseAccessException("查询用户借阅记录失败", e);
        } finally {
            closeQuietly(rows);
            closeQuietly(statement);
            closeQuietly(connection);
        }
    }

    @Override
    public boolean hasActive(Connection connection, String userId, String isbn)
            throws SQLException {
        if (userId == null || isbn == null) {
            return false;
        }
        boolean borrowed = connection == null;
        Connection conn = borrowed ? DbHelper.getConnection() : connection;
        PreparedStatement statement = null;
        ResultSet rows = null;
        try {
            statement = conn.prepareStatement("SELECT COUNT(*) FROM tblBorrow"
                    + " WHERE uId = ? AND bIsbn = ? AND rReturnedAt IS NULL");
            statement.setString(1, userId);
            statement.setString(2, isbn);
            rows = statement.executeQuery();
            return rows.next() && rows.getInt(1) > 0;
        } finally {
            closeQuietly(rows);
            closeQuietly(statement);
            if (borrowed) {
                closeQuietly(conn);
            }
        }
    }

    @Override
    public long insert(Connection connection, BorrowRecord record) throws SQLException {
        if (record == null) {
            return 0L;
        }
        String sql = "INSERT INTO tblBorrow (uId, bIsbn, bTitle, rBorrowedAt, rDueAt,"
                + " rRenewalCount, rFineAmount, rFinePaid) VALUES (?, ?, ?, ?, ?, 0, 0, 0)";
        boolean borrowed = connection == null;
        Connection conn = borrowed ? DbHelper.getConnection() : connection;
        PreparedStatement statement = null;
        ResultSet keys = null;
        try {
            statement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, record.getUserId());
            statement.setString(2, record.getIsbn());
            statement.setString(3, record.getBookTitle());
            statement.setTimestamp(4, timestamp(record.getBorrowedAt()));
            statement.setTimestamp(5, timestamp(record.getDueAt()));
            if (statement.executeUpdate() <= 0) {
                return 0L;
            }
            keys = statement.getGeneratedKeys();
            return keys.next() ? keys.getLong(1) : 0L;
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                throw new SQLException("duplicate or invalid active borrow", e);
            }
            throw e;
        } finally {
            closeQuietly(keys);
            closeQuietly(statement);
            if (borrowed) {
                closeQuietly(conn);
            }
        }
    }

    @Override
    public BorrowRecord findActiveById(Connection connection, long id) throws SQLException {
        return findOne(connection, "SELECT " + COLUMNS + " FROM tblBorrow"
                + " WHERE rId = ? AND rReturnedAt IS NULL", id);
    }

    @Override
    public BorrowRecord findById(Connection connection, long id) throws SQLException {
        return findOne(connection, "SELECT " + COLUMNS + " FROM tblBorrow WHERE rId = ?", id);
    }

    @Override
    public boolean markReturned(Connection connection, long id, Timestamp returnedAt,
            BigDecimal fineAmount, boolean finePaid) throws SQLException {
        String sql = "UPDATE tblBorrow SET rReturnedAt = ?, rFineAmount = ?, rFinePaid = ?"
                + " WHERE rId = ? AND rReturnedAt IS NULL";
        boolean borrowed = connection == null;
        Connection conn = borrowed ? DbHelper.getConnection() : connection;
        PreparedStatement statement = null;
        try {
            statement = conn.prepareStatement(sql);
            statement.setTimestamp(1, returnedAt);
            statement.setBigDecimal(2, fineAmount == null ? BigDecimal.ZERO : fineAmount);
            statement.setInt(3, finePaid ? 1 : 0);
            statement.setLong(4, id);
            return statement.executeUpdate() > 0;
        } finally {
            closeQuietly(statement);
            if (borrowed) {
                closeQuietly(conn);
            }
        }
    }

    @Override
    public boolean renew(Connection connection, long id, Timestamp dueAt, int renewalCount)
            throws SQLException {
        String sql = "UPDATE tblBorrow SET rDueAt = ?, rRenewalCount = ?"
                + " WHERE rId = ? AND rReturnedAt IS NULL";
        boolean borrowed = connection == null;
        Connection conn = borrowed ? DbHelper.getConnection() : connection;
        PreparedStatement statement = null;
        try {
            statement = conn.prepareStatement(sql);
            statement.setTimestamp(1, dueAt);
            statement.setInt(2, renewalCount);
            statement.setLong(3, id);
            return statement.executeUpdate() > 0;
        } finally {
            closeQuietly(statement);
            if (borrowed) {
                closeQuietly(conn);
            }
        }
    }

    @Override
    public boolean markFinePaid(Connection connection, long id, String transactionId)
            throws SQLException {
        String sql = "UPDATE tblBorrow SET rFinePaid = 1, rFineTransactionId = ?"
                + " WHERE rId = ? AND rFinePaid = 0";
        boolean borrowed = connection == null;
        Connection conn = borrowed ? DbHelper.getConnection() : connection;
        PreparedStatement statement = null;
        try {
            statement = conn.prepareStatement(sql);
            statement.setString(1, transactionId);
            statement.setLong(2, id);
            return statement.executeUpdate() > 0;
        } finally {
            closeQuietly(statement);
            if (borrowed) {
                closeQuietly(conn);
            }
        }
    }

    /**
     * 按记录号查一条记录。
     *
     * @param connection 事务连接；null 表示自行取用
     * @param sql        查询语句（含一个参数）
     * @param id         记录号
     * @return 借阅记录；不存在返回 null
     * @throws SQLException 查询失败
     */
    private BorrowRecord findOne(Connection connection, String sql, long id) throws SQLException {
        boolean borrowed = connection == null;
        Connection conn = borrowed ? DbHelper.getConnection() : connection;
        PreparedStatement statement = null;
        ResultSet rows = null;
        try {
            statement = conn.prepareStatement(sql);
            statement.setLong(1, id);
            rows = statement.executeQuery();
            return rows.next() ? toRecord(rows) : null;
        } finally {
            closeQuietly(rows);
            closeQuietly(statement);
            if (borrowed) {
                closeQuietly(conn);
            }
        }
    }

    /**
     * 结果行 → 借阅记录。
     *
     * @param rows 已定位到某行的结果集
     * @return 借阅记录
     * @throws SQLException 读取失败
     */
    private static BorrowRecord toRecord(ResultSet rows) throws SQLException {
        BorrowRecord record = new BorrowRecord(rows.getString("uId"), rows.getString("bIsbn"),
                rows.getString("bTitle"), rows.getTimestamp("rBorrowedAt"),
                rows.getTimestamp("rDueAt"));
        record.setId(Long.valueOf(rows.getLong("rId")));
        record.setReturnedAt(rows.getTimestamp("rReturnedAt"));
        record.setRenewalCount(rows.getInt("rRenewalCount"));
        record.setFineAmount(rows.getBigDecimal("rFineAmount"));
        record.setFinePaid(rows.getBoolean("rFinePaid"));
        record.setFineTransactionId(rows.getString("rFineTransactionId"));
        return record;
    }

    /**
     * 日期 → SQL 时间戳。
     *
     * @param date 日期；可为 null
     * @return 时间戳；null 输入返回 null
     */
    private static Timestamp timestamp(java.util.Date date) {
        return date == null ? null : new Timestamp(date.getTime());
    }

}
