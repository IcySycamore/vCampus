package edu.seu.vcampus.server.library;

import edu.seu.vcampus.common.library.entity.BookReservation;
import edu.seu.vcampus.common.library.entity.ReservationStatus;
import edu.seu.vcampus.server.db.DatabaseAccessException;
import edu.seu.vcampus.server.db.DbHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static edu.seu.vcampus.server.db.JdbcSupport.closeQuietly;

/**
 * 【MySQL 版】图书预约数据访问：落表 {@code tblReservation}。
 *
 * <p>
 * 生产装配只走这一份；{@link ReservationDaoMemory} 只是不落库的测试替身。建表见
 * {@code sql/vCampus-extend.sql}；状态以枚举名落库（{@code WAITING} / {@code READY} / {@code FULFILLED} /
 * {@code CANCELLED} / {@code EXPIRED}）。
 *
 * <p>
 * 「有效预约」统一指 {@code WAITING} 或 {@code READY} 两条状态；带 {@link Connection} 的方法一律 复用传入连接（业务层管事务），传
 * {@code null} 时自行取用并关闭。
 */
public class ReservationDaoJdbc implements ReservationDao {

    /** 查询列清单。 */
    private static final String COLUMNS = "rvId, uUuid, bIsbn, bTitle, rvRequestedAt, rvReadyAt, rvExpiresAt, rvStatus";

    /** 有效状态过滤片段。 */
    private static final String ACTIVE = " rvStatus IN ('WAITING', 'READY')";

    @Override
    public List<BookReservation> findByUser(String userId) {
        List<BookReservation> found = new ArrayList<BookReservation>();
        if (userId == null) {
            return found;
        }
        String sql = "SELECT " + COLUMNS + " FROM tblReservation WHERE uUuid = ?"
                + " ORDER BY rvRequestedAt DESC";
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rows = null;
        try {
            connection = DbHelper.getConnection();
            statement = connection.prepareStatement(sql);
            statement.setString(1, userId);
            rows = statement.executeQuery();
            while (rows.next()) {
                found.add(toReservation(rows));
            }
            return found;
        } catch (SQLException e) {
            throw new DatabaseAccessException("查询用户预约失败", e);
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
        return exists(connection, "SELECT COUNT(*) FROM tblReservation WHERE uUuid = ?"
                + " AND bIsbn = ? AND" + ACTIVE, userId, isbn);
    }

    @Override
    public BookReservation findReady(Connection connection, String userId, String isbn)
            throws SQLException {
        if (userId == null || isbn == null) {
            return null;
        }
        return findOne(connection, "SELECT " + COLUMNS + " FROM tblReservation WHERE uUuid = ?"
                + " AND bIsbn = ? AND rvStatus = 'READY'", userId, isbn);
    }

    @Override
    public BookReservation findActiveById(Connection connection, long id) throws SQLException {
        return findOne(connection, "SELECT " + COLUMNS + " FROM tblReservation WHERE rvId = ?"
                + " AND" + ACTIVE, Long.valueOf(id));
    }

    @Override
    public List<BookReservation> findExpiredReady(Connection connection, String isbn, Timestamp now)
            throws SQLException {
        List<BookReservation> found = new ArrayList<BookReservation>();
        if (isbn == null) {
            return found;
        }
        boolean borrowed = connection == null;
        Connection conn = borrowed ? DbHelper.getConnection() : connection;
        PreparedStatement statement = null;
        ResultSet rows = null;
        try {
            statement = conn.prepareStatement("SELECT " + COLUMNS + " FROM tblReservation"
                    + " WHERE bIsbn = ? AND rvStatus = 'READY' AND rvExpiresAt < ?"
                    + " ORDER BY rvReadyAt ASC");
            statement.setString(1, isbn);
            statement.setTimestamp(2, now);
            rows = statement.executeQuery();
            while (rows.next()) {
                found.add(toReservation(rows));
            }
            return found;
        } finally {
            closeQuietly(rows);
            closeQuietly(statement);
            if (borrowed) {
                closeQuietly(conn);
            }
        }
    }

    @Override
    public BookReservation findFirstWaiting(Connection connection, String isbn)
            throws SQLException {
        if (isbn == null) {
            return null;
        }
        return findOne(connection, "SELECT " + COLUMNS + " FROM tblReservation WHERE bIsbn = ?"
                + " AND rvStatus = 'WAITING' ORDER BY rvRequestedAt ASC LIMIT 1", isbn);
    }

    @Override
    public boolean hasActiveForBook(Connection connection, String isbn) throws SQLException {
        if (isbn == null) {
            return false;
        }
        return exists(connection, "SELECT COUNT(*) FROM tblReservation WHERE bIsbn = ? AND"
                + ACTIVE, isbn);
    }

    @Override
    public long insert(Connection connection, BookReservation reservation) throws SQLException {
        if (reservation == null) {
            return 0L;
        }
        String sql = "INSERT INTO tblReservation (rvUuid, uUuid, bIsbn, bTitle, rvRequestedAt,"
                + " rvStatus) VALUES (?, ?, ?, ?, ?, ?)";
        boolean borrowed = connection == null;
        Connection conn = borrowed ? DbHelper.getConnection() : connection;
        PreparedStatement statement = null;
        ResultSet keys = null;
        try {
            statement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, UUID.randomUUID().toString());// 主键按约定用 uuid
            statement.setString(2, reservation.getUserId());
            statement.setString(3, reservation.getIsbn());
            statement.setString(4, reservation.getBookTitle());
            statement.setTimestamp(5, reservation.getRequestedAt() == null
                    ? new Timestamp(System.currentTimeMillis())
                    : new Timestamp(reservation.getRequestedAt().getTime()));
            statement.setString(6, ReservationStatus.WAITING.name());
            if (statement.executeUpdate() <= 0) {
                return 0L;
            }
            keys = statement.getGeneratedKeys();
            return keys.next() ? keys.getLong(1) : 0L;
        } finally {
            closeQuietly(keys);
            closeQuietly(statement);
            if (borrowed) {
                closeQuietly(conn);
            }
        }
    }

    @Override
    public boolean updateStatus(Connection connection, long id, ReservationStatus status,
            Timestamp readyAt, Timestamp expiresAt) throws SQLException {
        if (status == null) {
            return false;
        }
        String sql = "UPDATE tblReservation SET rvStatus = ?, rvReadyAt = ?, rvExpiresAt = ?"
                + " WHERE rvId = ?";
        boolean borrowed = connection == null;
        Connection conn = borrowed ? DbHelper.getConnection() : connection;
        PreparedStatement statement = null;
        try {
            statement = conn.prepareStatement(sql);
            statement.setString(1, status.name());
            statement.setTimestamp(2, readyAt);
            statement.setTimestamp(3, expiresAt);
            statement.setLong(4, id);
            return statement.executeUpdate() > 0;
        } finally {
            closeQuietly(statement);
            if (borrowed) {
                closeQuietly(conn);
            }
        }
    }

    /**
     * 判断是否存在满足条件的记录。
     *
     * @param connection 事务连接；null 表示自行取用
     * @param sql        统计语句
     * @param params     绑定参数
     * @return 存在返回 true
     * @throws SQLException 查询失败
     */
    private boolean exists(Connection connection, String sql, String... params)
            throws SQLException {
        boolean borrowed = connection == null;
        Connection conn = borrowed ? DbHelper.getConnection() : connection;
        PreparedStatement statement = null;
        ResultSet rows = null;
        try {
            statement = conn.prepareStatement(sql);
            for (int i = 0; i < params.length; i++) {
                statement.setString(i + 1, params[i]);
            }
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

    /**
     * 查一条预约。
     *
     * @param connection 事务连接；null 表示自行取用
     * @param sql        查询语句
     * @param params     绑定参数
     * @return 预约；无记录返回 null
     * @throws SQLException 查询失败
     */
    private BookReservation findOne(Connection connection, String sql, Object... params)
            throws SQLException {
        boolean borrowed = connection == null;
        Connection conn = borrowed ? DbHelper.getConnection() : connection;
        PreparedStatement statement = null;
        ResultSet rows = null;
        try {
            statement = conn.prepareStatement(sql);
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }
            rows = statement.executeQuery();
            return rows.next() ? toReservation(rows) : null;
        } finally {
            closeQuietly(rows);
            closeQuietly(statement);
            if (borrowed) {
                closeQuietly(conn);
            }
        }
    }

    /**
     * 结果行 → 预约记录。
     *
     * @param rows 已定位到某行的结果集
     * @return 预约记录
     * @throws SQLException 读取失败
     */
    private static BookReservation toReservation(ResultSet rows) throws SQLException {
        BookReservation reservation = new BookReservation(rows.getString("uUuid"),
                rows.getString("bIsbn"), rows.getString("bTitle"),
                rows.getTimestamp("rvRequestedAt"));
        reservation.setId(Long.valueOf(rows.getLong("rvId")));
        reservation.setReadyAt(rows.getTimestamp("rvReadyAt"));
        reservation.setExpiresAt(rows.getTimestamp("rvExpiresAt"));
        reservation.setStatus(status(rows.getString("rvStatus")));
        return reservation;
    }

    /**
     * 库中文本 → 预约状态。
     *
     * @param text 库中取值；null 或无法识别时视作等待中
     * @return 预约状态
     */
    private static ReservationStatus status(String text) {
        if (text == null) {
            return ReservationStatus.WAITING;
        }
        try {
            return ReservationStatus.valueOf(text);
        } catch (IllegalArgumentException e) {
            return ReservationStatus.WAITING;
        }
    }

}
