package edu.seu.vcampus.server.module.library;

import edu.seu.vcampus.common.entity.Book;
import edu.seu.vcampus.common.entity.BorrowRecord;
import edu.seu.vcampus.common.message.MessageType;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.sql.DataSource;

/**
 * 图书检索、借书、还书及消息路由服务。
 */
public class LibraryService {

    private static final int LOAN_DAYS = 30;
    private final DataSource dataSource;
    private final BookDao bookDao;
    private final BorrowDao borrowDao;

    /**
     * 使用指定数据源创建图书馆服务。
     *
     * @param dataSource 数据源
     */
    public LibraryService(DataSource dataSource) {
        this(dataSource, new BookDao(dataSource), new BorrowDao(dataSource));
    }

    LibraryService(DataSource dataSource, BookDao bookDao, BorrowDao borrowDao) {
        if (dataSource == null || bookDao == null || borrowDao == null) {
            throw new IllegalArgumentException("library dependencies must not be null");
        }
        this.dataSource = dataSource;
        this.bookDao = bookDao;
        this.borrowDao = borrowDao;
    }

    /**
     * 检索图书。
     *
     * @param keyword 关键词
     * @param field 检索字段
     * @return 匹配图书
     * @throws SQLException 数据访问失败
     */
    public List<Book> search(String keyword, String field) throws SQLException {
        return bookDao.search(keyword, field);
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
                    throw new LibraryException(MessageType.NOT_FOUND, "图书不存在");
                }
                if (borrowDao.hasActive(connection, validUser, validIsbn)) {
                    throw new LibraryException(MessageType.BAD_REQUEST,
                            "不能重复借阅同一本书");
                }
                if (!bookDao.adjustAvailable(connection, validIsbn, -1)) {
                    throw new LibraryException(MessageType.BAD_REQUEST,
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
                BorrowRecord record = borrowDao.findActiveById(
                        connection, validUser, recordId);
                if (record == null) {
                    throw new LibraryException(MessageType.NOT_FOUND, "借阅记录不存在或已归还");
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
