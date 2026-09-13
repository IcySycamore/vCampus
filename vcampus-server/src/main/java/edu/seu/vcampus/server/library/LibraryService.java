package edu.seu.vcampus.server.library;

import edu.seu.vcampus.common.library.entity.Book;
import edu.seu.vcampus.common.library.entity.BorrowRecord;
import edu.seu.vcampus.common.library.dto.BookQuery;
import edu.seu.vcampus.common.message.PageResponse;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.sql.DataSource;
import edu.seu.vcampus.common.constant.StatusCode;

/**
 * 图书检索、借书、还书及消息路由服务。
 */
public class LibraryService {

    private static final int LOAN_DAYS = 30;
    private final DataSource dataSource;
    private final BookDao bookDao;
    private final BorrowDao borrowDao;
    private final LibraryCatalogService catalog;

    /**
     * 注入数据库负责人提供的数据源和 DAO 实现，创建图书馆服务。
     * 两个 DAO 必须支持该数据源提供的连接，借还书事务由本服务统一管理。
     *
     * @param dataSource 数据源
     * @param bookDao 图书数据访问接口实现
     * @param borrowDao 借阅记录数据访问接口实现
     */
    public LibraryService(DataSource dataSource, BookDao bookDao, BorrowDao borrowDao) {
        if (dataSource == null || bookDao == null || borrowDao == null) {
            throw new IllegalArgumentException("library dependencies must not be null");
        }
        this.dataSource = dataSource;
        this.bookDao = bookDao;
        this.borrowDao = borrowDao;
        this.catalog = new LibraryCatalogService(dataSource, bookDao);
    }

    /** @return 图书馆藏管理服务，由消息处理器校验管理员身份后调用 */
    public LibraryCatalogService getCatalog() {
        return catalog;
    }

    /**
     * 检索图书。
     *
     * @param query 已校验的分页查询条件
     * @return 匹配图书分页
     * @throws SQLException 数据访问失败
     */
    public PageResponse<Book> search(BookQuery query) throws SQLException {
        PageResponse<Book> page = bookDao.search(query);
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

    /**
     * 查询用户借阅历史。
     *
     * @param userId 用户 ID
     * @return 借阅记录
     * @throws SQLException 数据访问失败
     */
    public List<BorrowRecord> listBorrows(String userId) throws SQLException {
        return borrowDao.findByUser(requireText(userId, "用户 ID"));
    }

    /**
     * 借出一本图书。
     *
     * @param userId 用户 ID
     * @param isbn ISBN
     * @return 新借阅记录
     * @throws SQLException 数据访问失败
     * @throws LibraryException 图书不存在、无库存或重复借阅
     */
    public BorrowRecord borrow(String userId, String isbn)
            throws SQLException, LibraryException {
        String validUser = requireText(userId, "用户 ID");
        String validIsbn = requireText(isbn, "ISBN");
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Book book = bookDao.findByIsbn(connection, validIsbn);
                if (book == null) {
                    throw new LibraryException(StatusCode.NOT_FOUND, "图书不存在");
                }
                if (book.isWithdrawn()) {
                    throw new LibraryException(StatusCode.BAD_REQUEST, "该图书已下架，无法借阅");
                }
                if (borrowDao.hasActive(connection, validUser, validIsbn)) {
                    throw new LibraryException(StatusCode.BAD_REQUEST,
                            "不能重复借阅同一本书");
                }
                if (!bookDao.adjustAvailable(connection, validIsbn, -1)) {
                    throw new LibraryException(StatusCode.BAD_REQUEST,
                            "该书暂无可借馆藏");
                }
                Date now = new Date();
                BorrowRecord record = new BorrowRecord(validUser, validIsbn,
                        book.getTitle(), now, dueDate(now));
                record.setId(borrowDao.insert(connection, record));
                connection.commit();
                return record;
            } catch (SQLException exception) {
                rollback(connection, exception);
                throw exception;
            } catch (LibraryException exception) {
                rollback(connection, exception);
                throw exception;
            }
        }
    }

    /**
     * 归还一本图书。
     *
     * @param userId 用户 ID
     * @param recordId 借阅记录号
     * @return 已更新的记录
     * @throws SQLException 数据访问失败
     * @throws LibraryException 记录不存在或已归还
     */
    public BorrowRecord returnBook(String userId, long recordId)
            throws SQLException, LibraryException {
        String validUser = requireText(userId, "用户 ID");
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                BorrowRecord record = borrowDao.findActiveById(connection, recordId);
                if (record == null) {
                    throw new LibraryException(StatusCode.NOT_FOUND, "借阅记录不存在或已归还");
                }
                if (!validUser.equals(record.getUserId())) {
                    throw new LibraryException(StatusCode.FORBIDDEN, "不能归还其他用户的借阅记录");
                }
                Timestamp returnedAt = new Timestamp(System.currentTimeMillis());
                if (!borrowDao.markReturned(connection, recordId, returnedAt)
                        || !bookDao.adjustAvailable(connection, record.getIsbn(), 1)) {
                    throw new SQLException("return update was not completed");
                }
                connection.commit();
                record.setReturnedAt(returnedAt);
                return record;
            } catch (SQLException exception) {
                rollback(connection, exception);
                throw exception;
            } catch (LibraryException exception) {
                rollback(connection, exception);
                throw exception;
            }
        }
    }

    private String requireText(String value, String label) {
        if (value == null || value.trim().length() == 0) {
            throw new IllegalArgumentException(label + "不能为空");
        }
        return value.trim();
    }

    private Date dueDate(Date borrowedAt) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(borrowedAt);
        calendar.add(Calendar.DAY_OF_MONTH, LOAN_DAYS);
        return calendar.getTime();
    }

    private void rollback(Connection connection, Exception cause) throws SQLException {
        try {
            connection.rollback();
        } catch (SQLException rollbackFailure) {
            cause.addSuppressed(rollbackFailure);
        }
    }
}
