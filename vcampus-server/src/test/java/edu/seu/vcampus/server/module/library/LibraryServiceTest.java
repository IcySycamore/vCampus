package edu.seu.vcampus.server.module.library;

import edu.seu.vcampus.common.entity.Book;
import edu.seu.vcampus.common.entity.BorrowRecord;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.message.MessageType;
import edu.seu.vcampus.server.auth.SessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 图书馆业务事务与协议测试。
 */
class LibraryServiceTest {

    private DataSource dataSource;
    private Connection connection;
    private BookDao bookDao;
    private BorrowDao borrowDao;
    private LibraryService service;
    private SessionManager sessions;
    private String token;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = mock(DataSource.class);
        connection = mock(Connection.class);
        bookDao = mock(BookDao.class);
        borrowDao = mock(BorrowDao.class);
        when(dataSource.getConnection()).thenReturn(connection);
        service = new LibraryService(dataSource, bookDao, borrowDao);
        sessions = new SessionManager();
        token = sessions.create("uuid-001", "001", "学生");
    }

    @Test
    void borrowingUpdatesStockAndCommits() throws Exception {
        Book book = new Book("978-7", "Java", "Author", "计算机", 2, 1);
        when(bookDao.findByIsbn(connection, "978-7")).thenReturn(book);
        when(bookDao.adjustAvailable(connection, "978-7", -1)).thenReturn(true);
        when(borrowDao.insert(eq(connection), any(BorrowRecord.class))).thenReturn(9L);

        BorrowRecord record = service.borrow("001", "978-7");

        assertEquals(Long.valueOf(9L), record.getId());
        assertEquals("001", record.getUserId());
        assertEquals("978-7", record.getIsbn());
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
        when(bookDao.findByIsbn(connection, "978-7"))
                .thenReturn(new Book("978-7", "Java", "A", "C", 1, 1));
        when(borrowDao.hasActive(connection, "001", "978-7")).thenReturn(true);
        Message request = new Message(MessageType.LIBRARY_BORROW, "978-7");
        request.setSender("001");
        request.setToken(token);

        Message response = new LibraryMessageHandler(service, sessions).handle(request);

        assertEquals(MessageType.BAD_REQUEST, response.getStatusCode());
        verify(connection).rollback();
        verify(connection, never()).commit();
        verify(bookDao, never()).adjustAvailable(connection, "978-7", -1);
    }

    @Test
    void failedInsertRollsBackStockChangeAndReturnsServerError() throws Exception {
        when(bookDao.findByIsbn(connection, "978-7"))
                .thenReturn(new Book("978-7", "Java", "A", "C", 1, 1));
        when(bookDao.adjustAvailable(connection, "978-7", -1)).thenReturn(true);
        when(borrowDao.insert(eq(connection), any(BorrowRecord.class)))
                .thenThrow(new SQLException("insert failed"));
        Message request = new Message(MessageType.LIBRARY_BORROW, "978-7");
        request.setSender("001");
        request.setToken(token);

        Message response = new LibraryMessageHandler(service, sessions).handle(request);

        assertEquals(MessageType.SERVER_ERROR, response.getStatusCode());
        assertEquals("图书馆服务暂时不可用", response.getData());
        verify(bookDao).adjustAvailable(connection, "978-7", -1);
        verify(connection).rollback();
        verify(connection, never()).commit();
        verify(connection).close();
    }

    @Test
    void returningUpdatesRecordAndStockInSameTransaction() throws Exception {
        BorrowRecord record = activeRecord();
        when(borrowDao.findActiveById(connection, "001", 9L)).thenReturn(record);
        when(borrowDao.markReturned(eq(connection), eq(9L), any(Timestamp.class)))
                .thenReturn(true);
        when(bookDao.adjustAvailable(connection, "978-7", 1)).thenReturn(true);

        BorrowRecord returned = service.returnBook("001", 9L);

        assertSame(record, returned);
        assertNotNull(returned.getReturnedAt());
        verify(borrowDao).markReturned(connection, 9L,
                new Timestamp(returned.getReturnedAt().getTime()));
        verify(bookDao).adjustAvailable(connection, "978-7", 1);
        verify(connection).setAutoCommit(false);
        verify(connection).commit();
        verify(connection).close();
    }

    @Test
    void failedReturnStockUpdateRollsBackRecordChange() throws Exception {
        BorrowRecord record = activeRecord();
        when(borrowDao.findActiveById(connection, "001", 9L)).thenReturn(record);
        when(borrowDao.markReturned(eq(connection), eq(9L), any(Timestamp.class)))
                .thenReturn(true);
        when(bookDao.adjustAvailable(connection, "978-7", 1)).thenReturn(false);

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
    void returnRequiresAnActiveRecordOwnedByTheUser() throws Exception {
        Message request = new Message(MessageType.LIBRARY_RETURN, Long.valueOf(9L));
        request.setSender("002");
        request.setToken(sessions.create("uuid-002", "002", "学生"));

        Message response = new LibraryMessageHandler(service, sessions).handle(request);

        assertEquals(MessageType.NOT_FOUND, response.getStatusCode());
        verify(borrowDao).findActiveById(connection, "002", 9L);
        verify(borrowDao, never()).markReturned(eq(connection), eq(9L), any(Timestamp.class));
        verify(connection).rollback();
        verify(connection, never()).commit();
    }

    private BorrowRecord activeRecord() {
        BorrowRecord record = new BorrowRecord("001", "978-7", "Java", new Date(), new Date());
        record.setId(9L);
        return record;
    }
}
