package edu.seu.vcampus.server.library;

import edu.seu.vcampus.common.library.entity.LibraryAccount;
import edu.seu.vcampus.common.library.entity.LibraryAccountStatus;
import edu.seu.vcampus.server.db.DatabaseAccessException;
import edu.seu.vcampus.server.db.DbHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;

import static edu.seu.vcampus.server.db.JdbcSupport.closeQuietly;

/**
 * 【MySQL 版】图书馆读者账户数据访问：落表 {@code tblLibraryAccount}。
 *
 * <p>
 * <p>
 * 建表见 {@code sql/vCampus.sql} 的 {@code tblLibraryAccount}；一个用户至多一条账户（{@code uUuid} 唯一），软删除只置
 * {@code laDeleted}，不物理删除。
 */
public class LibraryAccountDaoJdbc implements LibraryAccountDao {

    /** 查询列清单；对外标识是 uUuid，laId 只用于回填实体 Long m_id。 */
    private static final String COLUMNS = "laId, uUuid, laStatus, laBorrowLimit,"
            + " laCreatedAt, laUpdatedAt, laDeleted";

    @Override
    public LibraryAccount findByUserUuid(String userUuid) {
        if (userUuid == null) {
            return null;
        }
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rows = null;
        try {
            connection = DbHelper.getConnection();
            statement = connection.prepareStatement(
                    "SELECT " + COLUMNS + " FROM tblLibraryAccount WHERE uUuid = ?");
            statement.setString(1, userUuid);
            rows = statement.executeQuery();
            return rows.next() ? toAccount(rows) : null;
        } catch (SQLException e) {
            throw new DatabaseAccessException("查询读者账户失败: " + userUuid, e);
        } finally {
            closeQuietly(rows);
            closeQuietly(statement);
            closeQuietly(connection);
        }
    }

    @Override
    public boolean insert(LibraryAccount account) {
        if (account == null) {
            return false;
        }
        String sql = "INSERT INTO tblLibraryAccount (uUuid, laStatus, laBorrowLimit,"
                + " laCreatedAt, laUpdatedAt, laDeleted) VALUES (?, ?, ?, ?, ?, 0)";
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet keys = null;
        try {
            connection = DbHelper.getConnection();
            statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, account.getUserUuid());
            statement.setString(2, account.getStatus() == null
                    ? LibraryAccountStatus.NORMAL.name()
                    : account.getStatus().name());
            statement.setInt(3, account.getBorrowLimit());
            statement.setTimestamp(4, timestamp(account.getCreatedAt()));
            statement.setTimestamp(5, timestamp(account.getUpdatedAt()));
            if (statement.executeUpdate() <= 0) {
                return false;
            }
            keys = statement.getGeneratedKeys();
            if (keys.next()) {
                account.setId(Long.valueOf(keys.getLong(1)));// 接口要求开户后回填主键
            }
            return true;
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                return false;// 账户已存在
            }
            throw new DatabaseAccessException("新增读者账户失败: " + account.getUserUuid(), e);
        } finally {
            closeQuietly(keys);
            closeQuietly(statement);
            closeQuietly(connection);
        }
    }

    @Override
    public boolean update(LibraryAccount account) {
        if (account == null || account.getUserUuid() == null) {
            return false;
        }
        String sql = "UPDATE tblLibraryAccount SET laStatus = ?, laBorrowLimit = ?,"
                + " laUpdatedAt = ? WHERE uUuid = ?";
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = DbHelper.getConnection();
            statement = connection.prepareStatement(sql);
            statement.setString(1, account.getStatus() == null
                    ? LibraryAccountStatus.NORMAL.name()
                    : account.getStatus().name());
            statement.setInt(2, account.getBorrowLimit());
            statement.setTimestamp(3, timestamp(account.getUpdatedAt()));
            statement.setString(4, account.getUserUuid());
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseAccessException("更新读者账户失败: " + account.getUserUuid(), e);
        } finally {
            closeQuietly(statement);
            closeQuietly(connection);
        }
    }

    @Override
    public boolean softDelete(String userUuid, Date updatedAt) {
        if (userUuid == null) {
            return false;
        }
        String sql = "UPDATE tblLibraryAccount SET laDeleted = 1, laUpdatedAt = ?"
                + " WHERE uUuid = ? AND laDeleted = 0";
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = DbHelper.getConnection();
            statement = connection.prepareStatement(sql);
            statement.setTimestamp(1, timestamp(updatedAt));
            statement.setString(2, userUuid);
            return statement.executeUpdate() >= 0;// 已删除视为成功（接口约定幂等）
        } catch (SQLException e) {
            throw new DatabaseAccessException("软删除读者账户失败: " + userUuid, e);
        } finally {
            closeQuietly(statement);
            closeQuietly(connection);
        }
    }

    /**
     * 结果行 → 读者账户。
     *
     * @param rows 已定位到某行的结果集
     * @return 读者账户
     * @throws SQLException 读取失败
     */
    private static LibraryAccount toAccount(ResultSet rows) throws SQLException {
        LibraryAccount account = new LibraryAccount(rows.getString("uUuid"),
                rows.getInt("laBorrowLimit"), rows.getTimestamp("laCreatedAt"));
        account.setId(Long.valueOf(rows.getLong("laId")));
        account.setUpdatedAt(rows.getTimestamp("laUpdatedAt"));
        account.setStatus(status(rows.getString("laStatus")));
        if (rows.getBoolean("laDeleted")) {
            account.markDeleted(rows.getTimestamp("laUpdatedAt"));
        }
        return account;
    }

    /**
     * 库中文本 → 账户状态。
     *
     * @param text 库中取值；null 或无法识别时视作正常
     * @return 账户状态
     */
    private static LibraryAccountStatus status(String text) {
        if (text == null) {
            return LibraryAccountStatus.NORMAL;
        }
        try {
            return LibraryAccountStatus.valueOf(text);
        } catch (IllegalArgumentException e) {
            return LibraryAccountStatus.NORMAL;
        }
    }

    /**
     * 日期 → SQL 时间戳。
     *
     * @param date 日期；可为 null
     * @return 时间戳；null 输入返回 null
     */
    private static java.sql.Timestamp timestamp(Date date) {
        return date == null ? null : new java.sql.Timestamp(date.getTime());
    }

}
