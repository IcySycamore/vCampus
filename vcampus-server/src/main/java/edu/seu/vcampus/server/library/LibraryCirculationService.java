package edu.seu.vcampus.server.library;

import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.library.LibraryPolicy;
import edu.seu.vcampus.common.library.entity.Book;
import edu.seu.vcampus.common.library.entity.BookReservation;
import edu.seu.vcampus.common.library.entity.BorrowRecord;
import edu.seu.vcampus.common.library.entity.PopularBorrow;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

/** 处理借书、还书、续借和滞纳金结清事务。 */
final class LibraryCirculationService {
    private final LibraryConnectionSource m_data_source;
    private final BookDao m_books;
    private final BorrowDao m_borrows;
    private final LibraryReservationService m_reservations;
    private final LibraryBorrowEligibility m_eligibility;

    LibraryCirculationService(LibraryConnectionSource dataSource, BookDao books,
            BorrowDao borrows, LibraryReservationService reservations,
            LibraryAccountService accounts) {
        LibraryValues.requireDependencies("circulation", dataSource, books,
                borrows, reservations, accounts);
        m_data_source = dataSource;
        m_books = books;
        m_borrows = borrows;
        m_reservations = reservations;
        m_eligibility = new LibraryBorrowEligibility(borrows, accounts);
    }

    List<BorrowRecord> listBorrows(String userId) throws SQLException {
        return m_borrows.findByUser(LibraryValues.text(userId, "用户 ID"));
    }

    List<PopularBorrow> listPopular(int limit) throws SQLException {
        return m_borrows.findPopular(limit);
    }

    BorrowRecord borrow(String userId, String isbn)
            throws SQLException, LibraryException {
        String user = LibraryValues.text(userId, "用户 ID");
        String bookIsbn = LibraryValues.text(isbn, "ISBN");
        m_eligibility.requireEligible(user, new Date());
        try (Connection connection = m_data_source.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Book book = requireBook(connection, bookIsbn);
                if (m_borrows.hasActive(connection, user, bookIsbn)) {
                    throw badRequest("不能重复借阅同一本书");
                }
                BookReservation ready = readyReservation(connection, user,
                        bookIsbn, new Date());
                if (ready == null && !m_books.adjustAvailable(connection, bookIsbn, -1)) {
                    throw badRequest("该书暂无可借馆藏，可提交预约申请");
                }
                BorrowRecord record = newRecord(user, book);
                record.setId(m_borrows.insert(connection, record));
                if (ready != null) {
                    m_reservations.fulfill(connection, ready);
                }
                connection.commit();
                return record;
            } catch (SQLException exception) {
                LibraryValues.rollback(connection, exception);
                throw exception;
            } catch (LibraryException exception) {
                LibraryValues.rollback(connection, exception);
                throw exception;
            }
        }
    }

    BorrowRecord returnBook(String userId, long recordId)
            throws SQLException, LibraryException {
        String user = LibraryValues.text(userId, "用户 ID");
        try (Connection connection = m_data_source.getConnection()) {
            connection.setAutoCommit(false);
            try {
                BorrowRecord record = ownedActive(connection, user, recordId,
                        "借阅记录不存在或已归还");
                Timestamp returnedAt = new Timestamp(System.currentTimeMillis());
                BigDecimal fine = LibraryPolicy.overdueFine(record.getDueAt(), returnedAt);
                boolean paid = fine.compareTo(BigDecimal.ZERO) == 0;
                if (!m_borrows.markReturned(connection, recordId, returnedAt, fine, paid)
                        || !m_books.adjustAvailable(connection, record.getIsbn(), 1)) {
                    throw new SQLException("return update was not completed");
                }
                m_reservations.afterAvailable(connection, record.getIsbn(), returnedAt);
                connection.commit();
                record.setReturnedAt(returnedAt);
                record.setFineAmount(fine);
                record.setFinePaid(paid);
                return record;
            } catch (SQLException exception) {
                LibraryValues.rollback(connection, exception);
                throw exception;
            } catch (LibraryException exception) {
                LibraryValues.rollback(connection, exception);
                throw exception;
            }
        }
    }

    BorrowRecord renew(String userId, long recordId)
            throws SQLException, LibraryException {
        String user = LibraryValues.text(userId, "用户 ID");
        m_eligibility.requireOperational(user);
        try (Connection connection = m_data_source.getConnection()) {
            connection.setAutoCommit(false);
            try {
                BorrowRecord record = ownedActive(connection, user, recordId,
                        "借阅记录不存在或已归还");
                Date now = new Date();
                if (now.after(record.getDueAt())) {
                    throw badRequest("图书已经逾期，请先归还并缴纳滞纳金");
                }
                if (record.getRenewalCount() >= LibraryPolicy.MAX_RENEWALS) {
                    throw badRequest("每本图书最多续借 2 次");
                }
                m_books.findByIsbn(connection, record.getIsbn());
                m_reservations.reconcile(connection, record.getIsbn(), now);
                if (m_reservations.hasDemand(connection, record.getIsbn())) {
                    throw badRequest("已有读者预约该书，暂不能续借");
                }
                Date dueAt = LibraryValues.addDays(record.getDueAt(),
                        LibraryPolicy.RENEWAL_DAYS);
                int renewalCount = record.getRenewalCount() + 1;
                if (!m_borrows.renew(connection, recordId,
                        new Timestamp(dueAt.getTime()), renewalCount)) {
                    throw new SQLException("renew update was not completed");
                }
                connection.commit();
                record.setDueAt(dueAt);
                record.setRenewalCount(renewalCount);
                return record;
            } catch (SQLException exception) {
                LibraryValues.rollback(connection, exception);
                throw exception;
            } catch (LibraryException exception) {
                LibraryValues.rollback(connection, exception);
                throw exception;
            }
        }
    }

    private Book requireBook(Connection connection, String isbn)
            throws SQLException, LibraryException {
        Book book = m_books.findByIsbn(connection, isbn);
        if (book == null) {
            throw new LibraryException(StatusCode.NOT_FOUND, "图书不存在");
        }
        if (book.isWithdrawn()) {
            throw badRequest("该图书已下架，无法借阅");
        }
        return book;
    }

    private BorrowRecord newRecord(String user, Book book) {
        Date now = new Date();
        return new BorrowRecord(user, book.getIsbn(), book.getTitle(),
                now, LibraryValues.dueDate(now));
    }

    private BookReservation readyReservation(Connection connection, String user,
            String isbn, Date now) throws SQLException, LibraryException {
        m_reservations.reconcile(connection, isbn, now);
        return m_reservations.readyFor(connection, user, isbn);
    }

    private BorrowRecord ownedActive(Connection connection, String user, long id,
            String missing) throws SQLException, LibraryException {
        BorrowRecord record = m_borrows.findActiveById(connection, id);
        if (record == null) {
            throw new LibraryException(StatusCode.NOT_FOUND, missing);
        }
        ensureOwner(record, user);
        return record;
    }

    private void ensureOwner(BorrowRecord record, String user) throws LibraryException {
        if (!user.equals(record.getUserId())) {
            throw new LibraryException(StatusCode.FORBIDDEN, "不能操作其他用户的借阅记录");
        }
    }

    private LibraryException badRequest(String message) {
        return new LibraryException(StatusCode.BAD_REQUEST, message);
    }
}
