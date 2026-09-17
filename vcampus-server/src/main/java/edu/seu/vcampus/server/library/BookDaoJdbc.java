package edu.seu.vcampus.server.library;

import edu.seu.vcampus.common.library.dto.BookQuery;
import edu.seu.vcampus.common.library.entity.Book;
import edu.seu.vcampus.common.message.PageResponse;
import edu.seu.vcampus.server.db.DatabaseAccessException;
import edu.seu.vcampus.server.db.DbHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 【MySQL 版】馆藏数据访问：落表 {@code tblBook}（含扩展列 {@code bWithdrawn}）。
 *
 * <p>
 * 与 {@link BookDaoMemory} 实现同一个 {@link BookDao}，可在装配处按开关二选一。
 *
 * <p>
 * <b>连接归属</b>：接口里带 {@link Connection} 的方法由业务层管理事务，本实现一律复用传入连接、 不提交也不关闭；传
 * {@code null}（内存数据源路径）时自行取连接并在方法结束关闭。这样两种装配 方式共用同一份实现，不必写两套。
 *
 * <p>
 * <b>可借数量</b>：{@link #adjustAvailable} 用一条带边界条件的 UPDATE 完成"读-改-写"，
 * 并发下不会丢更新，也不会把数量改到总数之外；下架图书禁止负向扣减（仍允许归还）。
 */
public class BookDaoJdbc implements BookDao {

    /** 查询列清单。 */
    private static final String COLUMNS = "bIsbn, bTitle, bAuthor, bCategory, bTotal, bAvailable, bWithdrawn";

    @Override
    public PageResponse<Book> search(BookQuery query) {
        return paged(query, false);
    }

    @Override
    public PageResponse<Book> searchCatalog(BookQuery query) {
        return paged(query, true);
    }

    @Override
    public Book findByIsbn(Connection connection, String isbn) throws SQLException {
        if (isbn == null) {
            return null;
        }
        boolean borrowed = connection == null;
        Connection conn = borrowed ? DbHelper.getConnection() : connection;
        PreparedStatement statement = null;
        ResultSet rows = null;
        try {
            statement = conn.prepareStatement("SELECT " + COLUMNS
                    + " FROM tblBook WHERE bIsbn = ?");
            statement.setString(1, isbn);
            rows = statement.executeQuery();
            return rows.next() ? toBook(rows) : null;
        } finally {
            closeQuietly(rows);
            closeQuietly(statement);
            if (borrowed) {
                closeQuietly(conn);
            }
        }
    }

    @Override
    public boolean adjustAvailable(Connection connection, String isbn, int change)
            throws SQLException {
        if (isbn == null || change == 0) {
            return false;
        }
        // 一条 UPDATE 完成原子增减：结果必须落在 [0, bTotal] 内；下架图书只允许正向（归还）
        String sql = "UPDATE tblBook SET bAvailable = bAvailable + ?"
                + " WHERE bIsbn = ? AND bAvailable + ? >= 0 AND bAvailable + ? <= bTotal"
                + " AND (bWithdrawn = 0 OR ? > 0)";
        boolean borrowed = connection == null;
        Connection conn = borrowed ? DbHelper.getConnection() : connection;
        PreparedStatement statement = null;
        try {
            statement = conn.prepareStatement(sql);
            statement.setInt(1, change);
            statement.setString(2, isbn);
            statement.setInt(3, change);
            statement.setInt(4, change);
            statement.setInt(5, change);
            return statement.executeUpdate() > 0;
        } finally {
            closeQuietly(statement);
            if (borrowed) {
                closeQuietly(conn);
            }
        }
    }

    @Override
    public boolean insertBook(Connection connection, Book book) throws SQLException {
        if (book == null) {
            return false;
        }
        String sql = "INSERT INTO tblBook (bIsbn, bTitle, bAuthor, bCategory, bTotal,"
                + " bAvailable, bWithdrawn) VALUES (?, ?, ?, ?, ?, ?, ?)";
        boolean borrowed = connection == null;
        Connection conn = borrowed ? DbHelper.getConnection() : connection;
        PreparedStatement statement = null;
        try {
            statement = conn.prepareStatement(sql);
            statement.setString(1, book.getIsbn());
            statement.setString(2, book.getTitle());
            statement.setString(3, book.getAuthor());
            statement.setString(4, book.getCategory());
            statement.setInt(5, book.getTotalCopies());
            statement.setInt(6, book.getAvailableCopies());
            statement.setInt(7, book.isWithdrawn() ? 1 : 0);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            if (isDuplicate(e)) {
                return false;// ISBN 已存在：按接口约定返回 false，不算异常
            }
            throw e;
        } finally {
            closeQuietly(statement);
            if (borrowed) {
                closeQuietly(conn);
            }
        }
    }

    @Override
    public boolean updateBook(Connection connection, Book book) throws SQLException {
        if (book == null) {
            return false;
        }
        // 不动下架状态：下架/恢复由 withdrawBook 单独控制（表结构与实体都只提供下架）
        String sql = "UPDATE tblBook SET bTitle = ?, bAuthor = ?, bCategory = ?, bTotal = ?,"
                + " bAvailable = ? WHERE bIsbn = ?";
        boolean borrowed = connection == null;
        Connection conn = borrowed ? DbHelper.getConnection() : connection;
        PreparedStatement statement = null;
        try {
            statement = conn.prepareStatement(sql);
            statement.setString(1, book.getTitle());
            statement.setString(2, book.getAuthor());
            statement.setString(3, book.getCategory());
            statement.setInt(4, book.getTotalCopies());
            statement.setInt(5, book.getAvailableCopies());
            statement.setString(6, book.getIsbn());
            return statement.executeUpdate() > 0;
        } finally {
            closeQuietly(statement);
            if (borrowed) {
                closeQuietly(conn);
            }
        }
    }

    @Override
    public boolean withdrawBook(Connection connection, String isbn) throws SQLException {
        if (isbn == null) {
            return false;
        }
        boolean borrowed = connection == null;
        Connection conn = borrowed ? DbHelper.getConnection() : connection;
        PreparedStatement statement = null;
        try {
            statement = conn.prepareStatement(
                    "UPDATE tblBook SET bWithdrawn = 1 WHERE bIsbn = ? AND bWithdrawn = 0");
            statement.setString(1, isbn);
            return statement.executeUpdate() > 0;
        } finally {
            closeQuietly(statement);
            if (borrowed) {
                closeQuietly(conn);
            }
        }
    }

    /**
     * 分页检索。
     *
     * @param query            查询条件；null 视为默认条件
     * @param includeWithdrawn 是否包含已下架馆藏（管理员检索）
     * @return 分页结果
     */
    private PageResponse<Book> paged(BookQuery query, boolean includeWithdrawn) {
        BookQuery condition = query == null ? new BookQuery() : query;
        StringBuilder sql = new StringBuilder("SELECT " + COLUMNS + " FROM tblBook WHERE 1 = 1");
        List<String> params = new ArrayList<String>();
        appendFilter(sql, params, condition, includeWithdrawn);
        sql.append(" ORDER BY bTitle");
        sql.append(" LIMIT ? OFFSET ?");

        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rows = null;
        try {
            connection = DbHelper.getConnection();
            long total = count(condition, includeWithdrawn);
            statement = connection.prepareStatement(sql.toString());
            int index = 1;
            for (String param : params) {
                statement.setString(index++, param);
            }
            statement.setInt(index++, condition.getPageSize());
            statement.setInt(index, PageResponse.offsetOf(condition.getPageNumber(),
                    condition.getPageSize()));
            rows = statement.executeQuery();
            List<Book> items = new ArrayList<Book>();
            while (rows.next()) {
                items.add(toBook(rows));
            }
            return new PageResponse<Book>(items, total, condition.getPageNumber(),
                    condition.getPageSize());
        } catch (SQLException e) {
            throw new DatabaseAccessException("检索馆藏失败", e);
        } finally {
            closeQuietly(rows);
            closeQuietly(statement);
            closeQuietly(connection);
        }
    }

    /**
     * 统计满足条件的记录数。
     *
     * @param condition        查询条件
     * @param includeWithdrawn 是否含已下架
     * @return 记录总数
     * @throws SQLException 查询失败
     */
    private long count(BookQuery condition, boolean includeWithdrawn) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM tblBook WHERE 1 = 1");
        List<String> params = new ArrayList<String>();
        appendFilter(sql, params, condition, includeWithdrawn);
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rows = null;
        try {
            connection = DbHelper.getConnection();
            statement = connection.prepareStatement(sql.toString());
            for (int i = 0; i < params.size(); i++) {
                statement.setString(i + 1, params.get(i));
            }
            rows = statement.executeQuery();
            return rows.next() ? rows.getLong(1) : 0L;
        } finally {
            closeQuietly(rows);
            closeQuietly(statement);
            closeQuietly(connection);
        }
    }

    /**
     * 拼关键词与下架过滤条件。
     *
     * @param sql              目标语句
     * @param params           参数收集器
     * @param condition        查询条件
     * @param includeWithdrawn 是否含已下架
     */
    private void appendFilter(StringBuilder sql, List<String> params, BookQuery condition,
            boolean includeWithdrawn) {
        if (!includeWithdrawn) {
            sql.append(" AND bWithdrawn = 0");
        }
        String keyword = condition.getKeyword();
        if (keyword == null || keyword.trim().length() == 0) {
            return;
        }
        String like = "%" + keyword.trim() + "%";
        String field = condition.getField() == null ? "all" : condition.getField();
        if ("title".equals(field)) {
            sql.append(" AND bTitle LIKE ?");
            params.add(like);
        } else if ("author".equals(field)) {
            sql.append(" AND bAuthor LIKE ?");
            params.add(like);
        } else if ("isbn".equals(field)) {
            sql.append(" AND bIsbn LIKE ?");
            params.add(like);
        } else {
            sql.append(" AND (bTitle LIKE ? OR bAuthor LIKE ? OR bIsbn LIKE ?"
                    + " OR bCategory LIKE ?)");
            params.add(like);
            params.add(like);
            params.add(like);
            params.add(like);
        }
    }

    /**
     * 结果行 → 图书实体。
     *
     * @param rows 已定位到某行的结果集
     * @return 图书
     * @throws SQLException 读取失败
     */
    private static Book toBook(ResultSet rows) throws SQLException {
        Book book = new Book(rows.getString("bIsbn"), rows.getString("bTitle"),
                rows.getString("bAuthor"), rows.getString("bCategory"), rows.getInt("bTotal"),
                rows.getInt("bAvailable"));
        book.setWithdrawn(rows.getBoolean("bWithdrawn"));
        return book;
    }

    /**
     * 判断是否为唯一键冲突。
     *
     * @param e 数据库异常
     * @return 冲突返回 true
     */
    private static boolean isDuplicate(SQLException e) {
        return e.getErrorCode() == 1062;
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
            // 关闭失败不影响业务结果
        }
    }
}
