package edu.seu.vcampus.server.module.library;

import edu.seu.vcampus.common.entity.Book;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;

/**
 * 图书馆藏的数据访问对象。
 */
public class BookDao {

    private final DataSource dataSource;

    /**
     * 创建图书 DAO。
     *
     * @param dataSource 数据源
     */
    public BookDao(DataSource dataSource) {
        if (dataSource == null) {
            throw new IllegalArgumentException("dataSource must not be null");
        }
        this.dataSource = dataSource;
    }

    /**
     * 按书名、作者、分类或全部字段进行模糊检索。
     *
     * @param keyword 关键词
     * @param field title、author、category 或 all
     * @return 匹配图书
     * @throws SQLException 数据访问失败
     */
    public List<Book> search(String keyword, String field) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            return search(connection, keyword, field);
        }
    }

    List<Book> search(Connection connection, String keyword, String field) throws SQLException {
        String normalizedField = normalizeField(field);
        String where = "all".equals(normalizedField)
                ? "(bTitle LIKE ? OR bAuthor LIKE ? OR bCategory LIKE ?)"
                : column(normalizedField) + " LIKE ?";
        String sql = "SELECT bIsbn,bTitle,bAuthor,bCategory,bTotal,bAvailable "
                + "FROM tblBook WHERE " + where + " ORDER BY bTitle";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            String pattern = "%" + safe(keyword) + "%";
            statement.setString(1, pattern);
            if ("all".equals(normalizedField)) {
                statement.setString(2, pattern);
                statement.setString(3, pattern);
            }
            try (ResultSet result = statement.executeQuery()) {
                List<Book> books = new ArrayList<Book>();
                while (result.next()) {
                    books.add(map(result));
                }
                return books;
            }
        }
    }

    Book findByIsbn(Connection connection, String isbn) throws SQLException {
        String sql = "SELECT bIsbn,bTitle,bAuthor,bCategory,bTotal,bAvailable "
                + "FROM tblBook WHERE bIsbn=?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, isbn);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? map(result) : null;
            }
        }
    }

    boolean adjustAvailable(Connection connection, String isbn, int change) throws SQLException {
        String sql = "UPDATE tblBook SET bAvailable=bAvailable+? WHERE bIsbn=? "
                + "AND bAvailable+? BETWEEN 0 AND bTotal";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, change);
            statement.setString(2, isbn);
            statement.setInt(3, change);
            return statement.executeUpdate() == 1;
        }
    }

    private Book map(ResultSet result) throws SQLException {
        return new Book(result.getString("bIsbn"), result.getString("bTitle"),
                result.getString("bAuthor"), result.getString("bCategory"),
                result.getInt("bTotal"), result.getInt("bAvailable"));
    }

    private String normalizeField(String field) {
        if ("title".equals(field) || "author".equals(field) || "category".equals(field)) {
            return field;
        }
        return "all";
    }

    private String column(String field) {
        if ("author".equals(field)) {
            return "bAuthor";
        }
        if ("category".equals(field)) {
            return "bCategory";
        }
        return "bTitle";
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
