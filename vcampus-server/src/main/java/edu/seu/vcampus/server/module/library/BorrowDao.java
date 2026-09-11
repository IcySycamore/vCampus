package edu.seu.vcampus.server.module.library;

import edu.seu.vcampus.common.entity.BorrowRecord;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;

/**
 * 图书借阅记录的数据访问对象。
 */
public class BorrowDao {

    private final DataSource dataSource;

    /**
     * 创建借阅 DAO。
     *
     * @param dataSource 数据源
     */
    public BorrowDao(DataSource dataSource) {
        if (dataSource == null) {
            throw new IllegalArgumentException("dataSource must not be null");
        }
        this.dataSource = dataSource;
    }

    /**
     * 查询用户的全部借阅记录。
     *
     * @param userId 用户 ID
     * @return 借阅记录，最新记录在前
     * @throws SQLException 数据访问失败
     */
    public List<BorrowRecord> findByUser(String userId) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            return findByUser(connection, userId);
        }
    }

    List<BorrowRecord> findByUser(Connection connection, String userId) throws SQLException {
        String sql = "SELECT rId,uId,bIsbn,bTitle,rBorrowedAt,rDueAt,rReturnedAt "
                + "FROM tblBorrow WHERE uId=? ORDER BY rBorrowedAt DESC";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                List<BorrowRecord> records = new ArrayList<BorrowRecord>();
                while (result.next()) {
                    records.add(map(result));
                }
                return records;
            }
        }
    }

    boolean hasActive(Connection connection, String userId, String isbn) throws SQLException {
        String sql = "SELECT 1 FROM tblBorrow WHERE uId=? AND bIsbn=? AND rReturnedAt IS NULL";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, userId);
            statement.setString(2, isbn);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    long insert(Connection connection, BorrowRecord record) throws SQLException {
        String sql = "INSERT INTO tblBorrow "
                + "(uId,bIsbn,bTitle,rBorrowedAt,rDueAt,rReturnedAt) VALUES (?,?,?,?,?,NULL)";
        try (PreparedStatement statement = connection.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, record.getUserId());
            statement.setString(2, record.getIsbn());
            statement.setString(3, record.getBookTitle());
            statement.setTimestamp(4, new Timestamp(record.getBorrowedAt().getTime()));
            statement.setTimestamp(5, new Timestamp(record.getDueAt().getTime()));
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("borrow record key was not generated");
                }
                return keys.getLong(1);
            }
        }
    }

    BorrowRecord findActiveById(Connection connection, String userId, long id)
            throws SQLException {
        String sql = "SELECT rId,uId,bIsbn,bTitle,rBorrowedAt,rDueAt,rReturnedAt "
                + "FROM tblBorrow WHERE rId=? AND uId=? AND rReturnedAt IS NULL";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            statement.setString(2, userId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? map(result) : null;
            }
        }
    }

    boolean markReturned(Connection connection, long id, Timestamp returnedAt)
            throws SQLException {
        String sql = "UPDATE tblBorrow SET rReturnedAt=? WHERE rId=? AND rReturnedAt IS NULL";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, returnedAt);
            statement.setLong(2, id);
            return statement.executeUpdate() == 1;
        }
    }

    private BorrowRecord map(ResultSet result) throws SQLException {
        BorrowRecord record = new BorrowRecord();
        record.setId(result.getLong("rId"));
        record.setUserId(result.getString("uId"));
        record.setIsbn(result.getString("bIsbn"));
        record.setBookTitle(result.getString("bTitle"));
        record.setBorrowedAt(result.getTimestamp("rBorrowedAt"));
        record.setDueAt(result.getTimestamp("rDueAt"));
        record.setReturnedAt(result.getTimestamp("rReturnedAt"));
        return record;
    }
}
