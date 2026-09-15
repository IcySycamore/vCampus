package edu.seu.vcampus.server.library;

import edu.seu.vcampus.common.library.entity.BookReservation;
import edu.seu.vcampus.common.library.entity.ReservationStatus;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/** 预约 DAO 的内存占位实现。 */
public final class ReservationDaoMemory implements ReservationDao {
    private final Map<Long, BookReservation> m_reservations =
            new LinkedHashMap<Long, BookReservation>();
    private final AtomicLong m_next_id = new AtomicLong(1L);

    @Override
    public synchronized List<BookReservation> findByUser(String userId) {
        List<BookReservation> found = new ArrayList<BookReservation>();
        for (BookReservation reservation : m_reservations.values()) {
            if (same(userId, reservation.getUserId())) {
                found.add(LibraryMemoryCopies.reservation(reservation));
            }
        }
        Collections.sort(found, requestedComparator(false));
        return found;
    }

    @Override
    public synchronized boolean hasActive(Connection connection, String userId, String isbn) {
        for (BookReservation reservation : m_reservations.values()) {
            if (reservation.isActive() && same(userId, reservation.getUserId())
                    && same(isbn, reservation.getIsbn())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized BookReservation findReady(Connection connection, String userId,
            String isbn) {
        for (BookReservation reservation : m_reservations.values()) {
            if (reservation.getStatus() == ReservationStatus.READY
                    && same(userId, reservation.getUserId())
                    && same(isbn, reservation.getIsbn())) {
                return LibraryMemoryCopies.reservation(reservation);
            }
        }
        return null;
    }

    @Override
    public synchronized BookReservation findActiveById(Connection connection, long id) {
        BookReservation reservation = m_reservations.get(id);
        return reservation == null || !reservation.isActive()
                ? null : LibraryMemoryCopies.reservation(reservation);
    }

    @Override
    public synchronized List<BookReservation> findExpiredReady(Connection connection,
            String isbn, Timestamp now) {
        List<BookReservation> found = new ArrayList<BookReservation>();
        for (BookReservation reservation : m_reservations.values()) {
            if (reservation.getStatus() == ReservationStatus.READY
                    && same(isbn, reservation.getIsbn())
                    && reservation.getExpiresAt() != null && now != null
                    && reservation.getExpiresAt().getTime() <= now.getTime()) {
                found.add(LibraryMemoryCopies.reservation(reservation));
            }
        }
        Collections.sort(found, readyComparator());
        return found;
    }

    @Override
    public synchronized BookReservation findFirstWaiting(Connection connection, String isbn) {
        List<BookReservation> waiting = new ArrayList<BookReservation>();
        for (BookReservation reservation : m_reservations.values()) {
            if (reservation.getStatus() == ReservationStatus.WAITING
                    && same(isbn, reservation.getIsbn())) {
                waiting.add(reservation);
            }
        }
        if (waiting.isEmpty()) {
            return null;
        }
        Collections.sort(waiting, requestedComparator(true));
        return LibraryMemoryCopies.reservation(waiting.get(0));
    }

    @Override
    public synchronized boolean hasActiveForBook(Connection connection, String isbn) {
        for (BookReservation reservation : m_reservations.values()) {
            if (reservation.isActive() && same(isbn, reservation.getIsbn())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized long insert(Connection connection, BookReservation reservation)
            throws SQLException {
        if (reservation == null || hasActive(connection, reservation.getUserId(),
                reservation.getIsbn())) {
            throw new SQLException("duplicate or invalid active reservation");
        }
        long id = m_next_id.getAndIncrement();
        reservation.setId(id);
        m_reservations.put(id, LibraryMemoryCopies.reservation(reservation));
        return id;
    }

    @Override
    public synchronized boolean updateStatus(Connection connection, long id,
            ReservationStatus status, Timestamp readyAt, Timestamp expiresAt) {
        BookReservation reservation = m_reservations.get(id);
        if (reservation == null || status == null) {
            return false;
        }
        reservation.setStatus(status);
        reservation.setReadyAt(readyAt);
        reservation.setExpiresAt(expiresAt);
        return true;
    }

    private Comparator<BookReservation> requestedComparator(final boolean ascending) {
        return new Comparator<BookReservation>() {
            @Override
            public int compare(BookReservation left, BookReservation right) {
                long first = left.getRequestedAt() == null ? 0L
                        : left.getRequestedAt().getTime();
                long second = right.getRequestedAt() == null ? 0L
                        : right.getRequestedAt().getTime();
                int result = first == second ? 0 : first < second ? -1 : 1;
                return ascending ? result : -result;
            }
        };
    }

    private Comparator<BookReservation> readyComparator() {
        return new Comparator<BookReservation>() {
            @Override
            public int compare(BookReservation left, BookReservation right) {
                long first = left.getReadyAt() == null ? 0L : left.getReadyAt().getTime();
                long second = right.getReadyAt() == null ? 0L : right.getReadyAt().getTime();
                return first == second ? 0 : first < second ? -1 : 1;
            }
        };
    }

    private boolean same(String left, String right) {
        return left == null ? right == null : left.equals(right);
    }
}
