package edu.seu.vcampus.server.library;

import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.library.entity.Book;
import edu.seu.vcampus.common.library.entity.BookReservation;
import edu.seu.vcampus.common.library.entity.BorrowRecord;
import edu.seu.vcampus.common.library.entity.LibraryAccount;
import edu.seu.vcampus.common.library.dto.BorrowRequest;
import edu.seu.vcampus.common.library.dto.RecordRef;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.server.user.SessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import javax.sql.DataSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 图书馆业务事务与协议测试。
 */
class LibraryServiceTest {

    private LibraryConnectionSource dataSource;
    private Connection connection;
    private BookDao bookDao;
    private BorrowDao borrowDao;
    private LibraryAccountDao accountDao;
    private ReservationDao reservationDao;
    private LibraryService service;
    private SessionManager sessions;
    private String token;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = mock(LibraryConnectionSource.class);
        connection = mock(Connection.class);
        bookDao = mock(BookDao.class);
        borrowDao = mock(BorrowDao.class);
        accountDao = mock(LibraryAccountDao.class);
        reservationDao = mock(ReservationDao.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(borrowDao.findByUser("001")).thenReturn(Collections.<BorrowRecord>emptyList());
        when(accountDao.findByUserUuid("001"))
                .thenReturn(new LibraryAccount("001", 30, new Date()));
        when(reservationDao.findExpiredReady(eq(connection), any(String.class),
                any(Timestamp.class)))
                .thenReturn(Collections.<BookReservation>emptyList());
        service = new LibraryService(dataSource, accountDao, bookDao, borrowDao, reservationDao);
        sessions = new SessionManager();
        token = sessions.create("001", "login-001", "学生");
    }

    @Test
    void borrowingUpdatesStockAndCommits() throws Exception {
        Book book = new Book("978-7-302-42328-7", "Java", "Author", "计算机", 2, 1);
        when(bookDao.findByIsbn(connection, "978-7-302-42328-7")).thenReturn(book);
        when(bookDao.adjustAvailable(connection, "978-7-302-42328-7", -1)).thenReturn(true);
        when(borrowDao.insert(eq(connection), any(BorrowRecord.class))).thenReturn(9L);

        BorrowRecord record = service.borrow("001", "978-7-302-42328-7");

        assertEquals(Long.valueOf(9L), record.getId());
        assertEquals("001", record.getUserId());
        assertEquals("978-7-302-42328-7", record.getIsbn());
        assertEquals("Java", record.getBookTitle());
        Calendar due = Calendar.getInstance();
        due.setTime(record.getBorrowedAt());
        due.add(Calendar.DAY_OF_MONTH, 30);
        assertEquals(due.getTime(), record.getDueAt());
        assertNull(record.getReturnedAt());
        verify(connection).setAutoCommit(false);
        verify(connection).commit();
        verify(connection).close();
    }

    @Test
    void messageReturnsConflictForDuplicateBorrow() throws Exception {
        when(bookDao.findByIsbn(connection, "978-7-302-42328-7"))
                .thenReturn(new Book("978-7-302-42328-7", "Java", "A", "C", 1, 1));
        when(borrowDao.hasActive(connection, "001", "978-7-302-42328-7")).thenReturn(true);
        Message request = new Message(Command.LIBRARY_BORROW,
                new BorrowRequest("978-7-302-42328-7"));
        request.setSender("001");
        request.setToken(token);

        Message response = new LibraryMessageHandler(service, sessions).createResponse(request);

        assertEquals(StatusCode.BAD_REQUEST, response.getStatusCode());
        verify(connection).rollback();
        verify(connection, never()).commit();
        verify(bookDao, never()).adjustAvailable(connection, "978-7-302-42328-7", -1);
    }

    @Test
    void failedInsertRollsBackStockChangeAndReturnsServerError() throws Exception {
        when(bookDao.findByIsbn(connection, "978-7-302-42328-7"))
                .thenReturn(new Book("978-7-302-42328-7", "Java", "A", "C", 1, 1));
        when(bookDao.adjustAvailable(connection, "978-7-302-42328-7", -1)).thenReturn(true);
        when(borrowDao.insert(eq(connection), any(BorrowRecord.class)))
                .thenThrow(new SQLException("insert failed"));
        Message request = new Message(Command.LIBRARY_BORROW,
                new BorrowRequest("978-7-302-42328-7"));
        request.setSender("001");
        request.setToken(token);

        Message response = new LibraryMessageHandler(service, sessions).createResponse(request);

        assertEquals(StatusCode.INTERNAL_ERROR, response.getStatusCode());
        assertEquals("图书馆服务暂时不可用", response.getData());
        verify(bookDao).adjustAvailable(connection, "978-7-302-42328-7", -1);
        verify(connection).rollback();
        verify(connection, never()).commit();
        verify(connection).close();
    }

    @Test
    void returningUpdatesRecordAndStockInSameTransaction() throws Exception {
        BorrowRecord record = activeRecord();
        when(borrowDao.findActiveById(connection, 9L)).thenReturn(record);
        when(borrowDao.markReturned(eq(connection), eq(9L), any(Timestamp.class),
                any(java.math.BigDecimal.class), anyBoolean()))
                .thenReturn(true);
        when(bookDao.adjustAvailable(connection, "978-7-302-42328-7", 1)).thenReturn(true);

        BorrowRecord returned = service.returnBook("001", 9L);

        assertSame(record, returned);
        assertNotNull(returned.getReturnedAt());
        verify(borrowDao).markReturned(eq(connection), eq(9L),
                eq(new Timestamp(returned.getReturnedAt().getTime())),
                any(java.math.BigDecimal.class), anyBoolean());
        verify(bookDao).adjustAvailable(connection, "978-7-302-42328-7", 1);
        verify(connection).setAutoCommit(false);
        verify(connection).commit();
        verify(connection).close();
    }

    @Test
    void failedReturnStockUpdateRollsBackRecordChange() throws Exception {
        BorrowRecord record = activeRecord();
        when(borrowDao.findActiveById(connection, 9L)).thenReturn(record);
        when(borrowDao.markReturned(eq(connection), eq(9L), any(Timestamp.class),
                any(java.math.BigDecimal.class), anyBoolean()))
                .thenReturn(true);
        when(bookDao.adjustAvailable(connection, "978-7-302-42328-7", 1)).thenReturn(false);

        assertThrows(SQLException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() throws Exception {
                service.returnBook("001", 9L);
            }
        });

        assertNull(record.getReturnedAt());
        verify(connection).rollback();
        verify(connection, never()).commit();
        verify(connection).close();
    }

    @Test
    void returningAnotherUsersRecordIsForbidden() throws Exception {
        when(borrowDao.findActiveById(connection, 9L)).thenReturn(activeRecord());
        Message request = new Message(Command.LIBRARY_RETURN, new RecordRef(9L));
        request.setSender("002");
        request.setToken(sessions.create("002", "login-002", "学生"));

        Message response = new LibraryMessageHandler(service, sessions).createResponse(request);

        assertEquals(StatusCode.FORBIDDEN, response.getStatusCode());
        verify(borrowDao).findActiveById(connection, 9L);
        verify(borrowDao, never()).markReturned(eq(connection), eq(9L),
                any(Timestamp.class), any(java.math.BigDecimal.class), anyBoolean());
        verify(connection).rollback();
        verify(connection, never()).commit();
    }

    private BorrowRecord activeRecord() {
        BorrowRecord record = new BorrowRecord("001", "978-7-302-42328-7", "Java",
                new Date(), new Date(System.currentTimeMillis() + 86400000L));
        record.setId(9L);
        return record;
    }
}
