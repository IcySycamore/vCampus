package edu.seu.vcampus.server.module.library;

import edu.seu.vcampus.common.entity.Book;
import edu.seu.vcampus.common.entity.BorrowRecord;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.message.MessageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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

    @BeforeEach
    void setUp() throws Exception {
        dataSource = mock(DataSource.class);
        connection = mock(Connection.class);
        bookDao = mock(BookDao.class);
        borrowDao = mock(BorrowDao.class);
        when(dataSource.getConnection()).thenReturn(connection);
        service = new LibraryService(dataSource, bookDao, borrowDao);
    }

    @Test
    void borrowingUpdatesStockAndCommits() throws Exception {
        Book book = new Book("978-7", "Java", "Author", "计算机", 2, 1);
        when(bookDao.findByIsbn(connection, "978-7")).thenReturn(book);
        when(bookDao.adjustAvailable(connection, "978-7", -1)).thenReturn(true);
        when(borrowDao.insert(eq(connection), any(BorrowRecord.class))).thenReturn(9L);

        BorrowRecord record = service.borrow("001", "978-7");

        assertEquals(Long.valueOf(9L), record.getId());
        assertNotNull(record.getDueAt());
        verify(connection).commit();
    }

    @Test
    void messageReturnsConflictForDuplicateBorrow() throws Exception {
        when(bookDao.findByIsbn(connection, "978-7"))
                .thenReturn(new Book("978-7", "Java", "A", "C", 1, 1));
        when(borrowDao.hasActive(connection, "001", "978-7")).thenReturn(true);
        Message request = new Message(MessageType.LIBRARY_BORROW, "978-7");
        request.setSender("001");

        Message response = new LibraryMessageHandler(service).handle(request);

        assertEquals(MessageType.BAD_REQUEST, response.getStatusCode());
        verify(connection).rollback();
    }
}
