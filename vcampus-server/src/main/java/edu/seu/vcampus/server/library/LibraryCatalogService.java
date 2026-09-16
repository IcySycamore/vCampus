package edu.seu.vcampus.server.library;

import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.library.entity.Book;
import edu.seu.vcampus.common.library.dto.BookRef;
import edu.seu.vcampus.common.library.dto.BookQuery;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.message.PageResponse;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;

/** 处理管理员馆藏事务；权限由 LibraryMessageHandler 使用真实会话校验。 */
public final class LibraryCatalogService {
    private final DataSource m_source;
    private final BookDao m_books;

    /**
     * 注入数据库负责人提供的接口实现。
     * @param source 数据源
     * @param books 图书 DAO
     */
    public LibraryCatalogService(DataSource source, BookDao books) {
        if (source == null || books == null) {
            throw new IllegalArgumentException("catalog dependencies must not be null");
        }
        m_source = source;
        m_books = books;
    }

    PageResponse<Book> search(BookQuery query) throws SQLException {
        PageResponse<Book> page = m_books.search(query);
        if (page == null) {
            throw new SQLException("book page must not be null");
        }
        for (Book book : page.getItems()) {
            if (book == null || book.isWithdrawn()) {
                throw new SQLException("public book page contains invalid catalog data");
            }
        }
        return page;
    }

    Object handle(Message request) throws SQLException, LibraryException {
        int command = request.getCommand();
        if (command == Command.LIBRARY_CATALOG_SEARCH) {
            BookQuery query = LibraryRequestValidator.search(request.getData());
            return m_books.searchCatalog(query);
        }
        BookRef reference = command == Command.LIBRARY_WITHDRAW_BOOK
                ? LibraryRequestValidator.book(request.getData()) : null;
        Book desired = reference == null ? LibraryCatalogValidator.book(request.getData()) : null;
        String isbn = reference == null ? desired.getIsbn() : reference.getIsbn();
        try (Connection connection = m_source.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Book current = m_books.findByIsbn(connection, isbn);
                Book result = mutate(connection, command, isbn, desired, current);
                connection.commit();
                return result;
            } catch (SQLException | LibraryException | RuntimeException exception) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackFailure) {
                    exception.addSuppressed(rollbackFailure);
                }
                throw exception;
            }
        }
    }

    private Book mutate(Connection connection, int command, String isbn, Book desired, Book current)
            throws SQLException, LibraryException {
        if (command == Command.LIBRARY_CREATE_BOOK) {
            if (current != null || !m_books.insertBook(connection, desired)) {
                throw new LibraryException(StatusCode.BAD_REQUEST, "该 ISBN 已存在，不能重复录入");
            }
            return desired;
        }
        if (current == null) {
            throw new LibraryException(StatusCode.NOT_FOUND, "图书不存在，请刷新馆藏");
        }
        if (command == Command.LIBRARY_UPDATE_BOOK) {
            if (current.getTotalCopies() < 0 || current.getAvailableCopies() < 0
                    || current.getAvailableCopies() > current.getTotalCopies()) {
                throw new SQLException("inconsistent inventory");
            }
            int borrowed = current.getTotalCopies() - current.getAvailableCopies();
            if (desired.getTotalCopies() < borrowed) {
                throw new LibraryException(StatusCode.BAD_REQUEST,
                        "馆藏总数不能少于当前未归还数量：" + borrowed + " 本");
            }
            desired.setAvailableCopies(desired.getTotalCopies() - borrowed);
            desired.setWithdrawn(current.isWithdrawn());
            if (!m_books.updateBook(connection, desired)) {
                throw new SQLException("catalog update failed");
            }
            return desired;
        }
        if (command != Command.LIBRARY_WITHDRAW_BOOK) {
            throw new LibraryException(StatusCode.BAD_REQUEST, "未知的馆藏管理命令");
        }
        if (!current.isWithdrawn() && !m_books.withdrawBook(connection, isbn)) {
            throw new SQLException("catalog withdrawal failed");
        }
        Book withdrawn = new Book(current.getIsbn(), current.getTitle(), current.getAuthor(),
                current.getCategory(), current.getTotalCopies(), current.getAvailableCopies());
        withdrawn.setWithdrawn(true);
        return withdrawn;
    }
}
