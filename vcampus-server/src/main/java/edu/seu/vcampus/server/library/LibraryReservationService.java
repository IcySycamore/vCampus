package edu.seu.vcampus.server.library;

import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.library.LibraryPolicy;
import edu.seu.vcampus.common.library.entity.Book;
import edu.seu.vcampus.common.library.entity.BookReservation;
import edu.seu.vcampus.common.library.entity.ReservationStatus;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 管理预约排队、到馆保留、取消与过期流转。 */
final class LibraryReservationService {
    private final LibraryConnectionSource dataSource;
    private final BookDao books;
    private final BorrowDao borrows;
    private final ReservationDao reservations;
    private final LibraryAccountService accounts;

    LibraryReservationService(LibraryConnectionSource dataSource, BookDao books,
            BorrowDao borrows, ReservationDao reservations,
            LibraryAccountService accounts) {
        LibraryValues.requireDependencies("reservation", dataSource, books,
                borrows, reservations, accounts);
        this.dataSource = dataSource;
        this.books = books;
        this.borrows = borrows;
        this.reservations = reservations;
        this.accounts = accounts;
    }

    BookReservation reserve(String userId, String isbn) throws SQLException, LibraryException {
        String user = LibraryValues.text(userId, "用户 ID");
        String bookIsbn = LibraryValues.text(isbn, "ISBN");
        accounts.ensureOperational(user);
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Book book = books.findByIsbn(connection, bookIsbn);
                requireReservable(book);
                reconcile(connection, bookIsbn, new Date());
                book = books.findByIsbn(connection, bookIsbn);
                if (book.getAvailableCopies() > 0) {
                    throw badRequest("该书当前有可借馆藏，请直接借阅");
                }
                if (borrows.hasActive(connection, user, bookIsbn)) {
                    throw badRequest("不能预约自己正在借阅的图书");
                }
                if (reservations.hasActive(connection, user, bookIsbn)) {
                    throw badRequest("不能重复预约同一本书");
                }
                BookReservation reservation = new BookReservation(user, bookIsbn,
                        book.getTitle(), new Date());
                reservation.setId(reservations.insert(connection, reservation));
                connection.commit();
                return reservation;
            } catch (SQLException exception) {
                LibraryValues.rollback(connection, exception);
                throw exception;
            } catch (LibraryException exception) {
                LibraryValues.rollback(connection, exception);
                throw exception;
            }
        }
    }

    List<BookReservation> list(String userId)
            throws SQLException, LibraryException {
        String user = LibraryValues.text(userId, "用户 ID");
        List<BookReservation> found = reservations.findByUser(user);
        if (found == null) {
            throw new SQLException("reservation records must not be null");
        }
        Set<String> activeBooks = new HashSet<String>();
        for (BookReservation reservation : found) {
            if (reservation.isActive()) {
                activeBooks.add(reservation.getIsbn());
            }
        }
        for (String isbn : activeBooks) {
            reconcileInTransaction(isbn);
        }
        return activeBooks.isEmpty() ? found : reservations.findByUser(user);
    }

    BookReservation cancel(String userId, long reservationId)
            throws SQLException, LibraryException {
        String user = LibraryValues.text(userId, "用户 ID");
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                BookReservation reservation = reservations.findActiveById(
                        connection, reservationId);
                if (reservation == null) {
                    throw new LibraryException(StatusCode.NOT_FOUND,
                            "预约不存在或已经结束");
                }
                if (!user.equals(reservation.getUserId())) {
                    throw new LibraryException(StatusCode.FORBIDDEN,
                            "不能取消其他用户的预约");
                }
                books.findByIsbn(connection, reservation.getIsbn());
                reconcile(connection, reservation.getIsbn(), new Date());
                reservation = reservations.findActiveById(connection, reservationId);
                if (reservation == null) {
                    throw badRequest("预约保留期已经结束");
                }
                update(connection, reservation, ReservationStatus.CANCELLED, null, null);
                if (reservation.getStatus() == ReservationStatus.READY) {
                    if (!books.adjustAvailable(connection, reservation.getIsbn(), 1)) {
                        throw new SQLException("reserved stock release failed");
                    }
                    promote(connection, reservation.getIsbn(), new Date());
                }
                connection.commit();
                reservation.setStatus(ReservationStatus.CANCELLED);
                return reservation;
            } catch (SQLException exception) {
                LibraryValues.rollback(connection, exception);
                throw exception;
            } catch (LibraryException exception) {
                LibraryValues.rollback(connection, exception);
                throw exception;
            }
        }
    }

    void reconcile(Connection connection, String isbn, Date now)
            throws SQLException {
        Timestamp current = new Timestamp(now.getTime());
        List<BookReservation> expired = reservations.findExpiredReady(
                connection, isbn, current);
        if (expired == null) {
            throw new SQLException("expired reservation records must not be null");
        }
        for (BookReservation reservation : expired) {
            update(connection, reservation, ReservationStatus.EXPIRED, null, null);
            if (!books.adjustAvailable(connection, isbn, 1)) {
                throw new SQLException("expired reserved stock release failed");
            }
        }
        promote(connection, isbn, now);
    }

    void afterAvailable(Connection connection, String isbn, Date now)
            throws SQLException {
        reconcile(connection, isbn, now);
    }

    BookReservation readyFor(Connection connection, String userId, String isbn)
            throws SQLException {
        return reservations.findReady(connection, userId, isbn);
    }

    boolean hasDemand(Connection connection, String isbn) throws SQLException {
        return reservations.hasActiveForBook(connection, isbn);
    }

    void fulfill(Connection connection, BookReservation reservation) throws SQLException {
        update(connection, reservation, ReservationStatus.FULFILLED, null, null);
    }

    private void reconcileInTransaction(String isbn) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                books.findByIsbn(connection, isbn);
                reconcile(connection, isbn, new Date());
                connection.commit();
            } catch (SQLException exception) {
                LibraryValues.rollback(connection, exception);
                throw exception;
            }
        }
    }

    private void promote(Connection connection, String isbn, Date now)
            throws SQLException {
        while (true) {
            BookReservation waiting = reservations.findFirstWaiting(connection, isbn);
            if (waiting == null || !books.adjustAvailable(connection, isbn, -1)) {
                return;
            }
            Date expires = LibraryValues.addDays(now, LibraryPolicy.RESERVATION_HOLD_DAYS);
            update(connection, waiting, ReservationStatus.READY,
                    new Timestamp(now.getTime()), new Timestamp(expires.getTime()));
        }
    }

    private void update(Connection connection, BookReservation reservation,
            ReservationStatus status, Timestamp readyAt, Timestamp expiresAt)
            throws SQLException {
        if (!reservations.updateStatus(connection, reservation.getId(),
                status, readyAt, expiresAt)) {
            throw new SQLException("reservation status update failed");
        }
    }

    private void requireReservable(Book book) throws LibraryException {
        if (book == null) {
            throw new LibraryException(StatusCode.NOT_FOUND, "图书不存在");
        }
        if (book.isWithdrawn()) {
            throw badRequest("该图书已下架，不能预约");
        }
    }

    private LibraryException badRequest(String message) {
        return new LibraryException(StatusCode.BAD_REQUEST, message);
    }
}
