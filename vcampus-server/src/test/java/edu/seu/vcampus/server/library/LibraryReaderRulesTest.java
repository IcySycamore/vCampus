package edu.seu.vcampus.server.library;

import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.library.entity.Book;
import edu.seu.vcampus.common.library.entity.BookReservation;
import edu.seu.vcampus.common.library.entity.BorrowRecord;
import edu.seu.vcampus.common.library.entity.LibraryAccount;
import edu.seu.vcampus.common.library.entity.ReservationStatus;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 验证预约、续借、逾期计费和银行缴费的核心事务规则。 */
class LibraryReaderRulesTest {
    private static final String ISBN = "9787302423287";
    private LibraryConnectionSource source;
    private Connection connection;
    private BookDao books;
    private BorrowDao borrows;
    private LibraryAccountDao accounts;
    private ReservationDao reservations;
    private LibraryService service;

    @BeforeEach
    void setUp() throws Exception {
        source = mock(LibraryConnectionSource.class);
        connection = mock(Connection.class);
        books = mock(BookDao.class);
        borrows = mock(BorrowDao.class);
        accounts = mock(LibraryAccountDao.class);
        reservations = mock(ReservationDao.class);
        when(source.getConnection()).thenReturn(connection);
        when(reservations.findExpiredReady(eq(connection), eq(ISBN), any(Timestamp.class)))
                .thenReturn(Collections.<BookReservation>emptyList());
        when(accounts.findByUserUuid("u1"))
                .thenReturn(new LibraryAccount("u1", 30, new Date()));
        service = new LibraryService(source, accounts, books, borrows, reservations);
    }

    @Test
    void unavailableBookCanBeReservedOnce() throws Exception {
        Book book = new Book(ISBN, "Java", "A", "C", 1, 0);
        when(books.findByIsbn(connection, ISBN)).thenReturn(book);
        when(reservations.insert(eq(connection), any(BookReservation.class))).thenReturn(7L);

        BookReservation result = service.reserve("u1", ISBN);

        assertEquals(Long.valueOf(7L), result.getId());
        assertEquals(ReservationStatus.WAITING, result.getStatus());
        verify(connection).commit();
    }

    @Test
    void returnFixesFineAndPromotesFirstReservation() throws Exception {
        long hour = 60L * 60L * 1000L;
        BorrowRecord loan = loan(new Date(System.currentTimeMillis() - 36L * hour));
        BookReservation waiting = new BookReservation("u2", ISBN, "Java", new Date());
        waiting.setId(8L);
        when(borrows.findActiveById(connection, 3L)).thenReturn(loan);
        when(borrows.markReturned(eq(connection), eq(3L), any(Timestamp.class),
                any(BigDecimal.class), eq(false))).thenReturn(true);
        when(books.adjustAvailable(connection, ISBN, 1)).thenReturn(true);
        when(books.adjustAvailable(connection, ISBN, -1)).thenReturn(true);
        when(borrows.insert(eq(connection), any(BorrowRecord.class))).thenReturn(9L);
        when(reservations.findFirstWaiting(connection, ISBN))
                .thenReturn(waiting).thenReturn(null);
        when(reservations.updateStatus(eq(connection), eq(8L),
                eq(ReservationStatus.FULFILLED), any(Timestamp.class),
                isNull(Timestamp.class)))
                        .thenReturn(true);

        BorrowRecord returned = service.returnBook("u1", 3L);

        assertEquals(new BigDecimal("0.20"), returned.getFineAmount());
        assertEquals(false, returned.isFinePaid());
        ArgumentCaptor<BorrowRecord> promoted = ArgumentCaptor.forClass(BorrowRecord.class);
        verify(borrows).insert(eq(connection), promoted.capture());
        assertEquals("u2", promoted.getValue().getUserId());
        assertEquals(ISBN, promoted.getValue().getIsbn());
        verify(reservations).updateStatus(eq(connection), eq(8L),
                eq(ReservationStatus.FULFILLED), any(Timestamp.class),
                isNull(Timestamp.class));
    }

    @Test
    void renewStartsAtOldDueDateAndStopsWhenReserved() throws Exception {
        Date oldDue = new Date(System.currentTimeMillis() + 86400000L);
        BorrowRecord loan = loan(oldDue);
        when(borrows.findActiveById(connection, 3L)).thenReturn(loan);
        when(borrows.renew(eq(connection), eq(3L), any(Timestamp.class), eq(1)))
                .thenReturn(true);

        BorrowRecord renewed = service.renew("u1", 3L);

        assertEquals(oldDue.getTime() + 30L * 86400000L, renewed.getDueAt().getTime());
        assertEquals(1, renewed.getRenewalCount());
        when(reservations.hasActiveForBook(connection, ISBN)).thenReturn(true);
        LibraryException failure = assertThrows(LibraryException.class,
                new org.junit.jupiter.api.function.Executable() {
                    @Override
                    public void execute() throws Exception {
                        service.renew("u1", 3L);
                    }
                });
        assertEquals(StatusCode.BAD_REQUEST, failure.getStatusCode());
    }

    @Test
    void finePaymentUsesStableReferenceAndMarksRecordPaid() throws Exception {
        BorrowRecord loan = loan(new Date(0));
        loan.setReturnedAt(new Date());
        loan.setFineAmount(new BigDecimal("1.20"));
        loan.setFinePaid(false);
        when(borrows.findById(connection, 3L)).thenReturn(loan);
        when(borrows.markFinePaid(connection, 3L, "bank-9")).thenReturn(true);
        LibraryFinePayment payment = mock(LibraryFinePayment.class);
        when(payment.pay("u1", new BigDecimal("1.20"), "LIBRARY_FINE:3"))
                .thenReturn("bank-9");

        BorrowRecord paid = service.payFine("u1", 3L, payment);

        assertSame(loan, paid);
        assertEquals(true, paid.isFinePaid());
        assertEquals("bank-9", paid.getFineTransactionId());
        verify(connection).commit();
    }

    private BorrowRecord loan(Date dueAt) {
        BorrowRecord record = new BorrowRecord("u1", ISBN, "Java", new Date(0), dueAt);
        record.setId(3L);
        return record;
    }
}
