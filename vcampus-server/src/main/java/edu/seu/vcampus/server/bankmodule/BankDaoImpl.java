package edu.seu.vcampus.server.bankmodule;

import edu.seu.vcampus.common.bank.entity.BankAccount;
import edu.seu.vcampus.server.db.DbHelper;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

/**
 * 银行账户数据访问实现。
 *
 * <p>通过 JDBC 操作银行账户表。假设表名为 tblBankAccount，字段包括：
 * accountId, userId, balance, status, createdAt, updatedAt。</p>
 */
public class BankDaoImpl implements BankDao {

    private static final String TABLE_NAME = "tblBankAccount";

    @Override
    public BankAccount findByUserUuid(String userUuid) {
        String sql = "SELECT accountId, userId, balance, status, createdAt, updatedAt "
                + "FROM " + TABLE_NAME + " WHERE userId = ?";

        try (Connection conn = DbHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, userUuid);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToAccount(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean updateBalance(String userUuid, BigDecimal newBalance) {
        String sql = "UPDATE " + TABLE_NAME
                + " SET balance = ?, updatedAt = ? WHERE userId = ?";

        try (Connection conn = DbHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setBigDecimal(1, newBalance);
            stmt.setTimestamp(2, new java.sql.Timestamp(System.currentTimeMillis()));
            stmt.setString(3, userUuid);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean insert(BankAccount account) {
        String sql = "INSERT INTO " + TABLE_NAME
                + " (accountId, userId, balance, status, createdAt, updatedAt) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DbHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, account.getAccountId());
            stmt.setString(2, account.getUserId());
            stmt.setBigDecimal(3, account.getBalance());
            stmt.setString(4, account.getStatus());
            stmt.setTimestamp(5, new java.sql.Timestamp(
                account.getCreatedAt() != null ? account.getCreatedAt().getTime() : System.currentTimeMillis()));
            stmt.setTimestamp(6, new java.sql.Timestamp(
                account.getUpdatedAt() != null ? account.getUpdatedAt().getTime() : System.currentTimeMillis()));

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean update(BankAccount account) {
        String sql = "UPDATE " + TABLE_NAME
                + " SET balance = ?, status = ?, updatedAt = ? WHERE userId = ?";

        try (Connection conn = DbHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setBigDecimal(1, account.getBalance());
            stmt.setString(2, account.getStatus());
            stmt.setTimestamp(3, new java.sql.Timestamp(System.currentTimeMillis()));
            stmt.setString(4, account.getUserId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 将结果集映射为 BankAccount 对象。
     */
    private BankAccount mapResultSetToAccount(ResultSet rs) throws SQLException {
        BankAccount account = new BankAccount();
        account.setAccountId(rs.getString("accountId"));
        account.setUserId(rs.getString("userId"));
        account.setBalance(rs.getBigDecimal("balance"));
        account.setStatus(rs.getString("status"));
        account.setCreatedAt(rs.getTimestamp("createdAt"));
        account.setUpdatedAt(rs.getTimestamp("updatedAt"));
        return account;
    }
}
