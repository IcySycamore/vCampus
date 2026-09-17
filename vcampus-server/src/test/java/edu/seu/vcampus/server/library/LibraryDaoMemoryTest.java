package edu.seu.vcampus.server.library;

import edu.seu.vcampus.common.library.dto.BookQuery;
import edu.seu.vcampus.common.library.entity.Book;
import edu.seu.vcampus.common.library.entity.BookReservation;
import edu.seu.vcampus.common.library.entity.BorrowRecord;
import edu.seu.vcampus.common.library.entity.LibraryAccount;
import edu.seu.vcampus.common.library.entity.ReservationStatus;
import edu.seu.vcampus.common.message.PageResponse;
import java.sql.Connection;
import java.sql.Timestamp;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 图书馆四个 DAO 内存占位实现的协作测试。 */
class LibraryDaoMemoryTest {
    private LibraryConnectionSourceMemory m_source;
    private LibraryAccountDaoMemory m_accounts;
    private BookDaoMemory m_books;
    private BorrowDaoMemory m_borrows;
    private ReservationDaoMemory m_reservations;
    private Connection m_connection;

    @BeforeEach
    void setUp() throws Exception {
        m_source = new LibraryConnectionSourceMemory();
        m_accounts = new LibraryAccountDaoMemory();
        m_books = new BookDaoMemory();
        m_borrows = new BorrowDaoMemory();
        m_reservations = new ReservationDaoMemory();
        m_connection = m_source.getConnection();
    }

    @Test
    void catalogSearchAndWithdrawalUseMemoryDao() throws Exception {
        assertTrue(m_books.insertBook(m_connection,
                new Book("2", "Beta", "Author B", "C", 2, 2)));
        assertTrue(m_books.insertBook(m_connection,
                new Book("1", "Alpha", "Author A", "C", 1, 1)));
        PageResponse<Book> first = m_books.search(new BookQuery("author", "author", 1, 1));
        assertEquals(2L, first.getTotal());
        assertEquals("Alpha", first.getItems().get(0).getTitle());
        assertTrue(m_books.withdrawBook(m_connection, "1"));
        assertEquals(1L, m_books.search(new BookQuery()).getTotal());
        assertEquals(2L, m_books.searchCatalog(new BookQuery()).getTotal());
    }

    @Test
    void sampleCatalogContainsBorrowableBooks() {
        BookDaoMemory samples = BookDaoMemory.withSampleBooks();
        PageResponse<Book> page = samples.search(new BookQuery());
        assertEquals(7L, page.getTotal());
        for (Book book : page.getItems()) {
            assertTrue(book.getAvailableCopies() > 0);
            assertEquals(book.getTotalCopies(), book.getAvailableCopies());
        }
    }

    @Test
    void serviceBorrowsAndReturnsWithMemoryDaos() throws Exception {
        createAccount("reader");
        assertTrue(m_books.insertBook(m_connection,
                new Book("isbn", "Java", "Author", "C", 1, 1)));
        LibraryService service = service();
        BorrowRecord borrowed = service.borrow("reader", "isbn");
        assertNotNull(borrowed.getId());
        assertEquals(1, service.listBorrows("reader").size());
        assertEquals(0, m_books.findByIsbn(m_connection, "isbn").getAvailableCopies());
        BorrowRecord returned = service.returnBook("reader", borrowed.getId());
        assertTrue(returned.isReturned());
        assertEquals(1, m_books.findByIsbn(m_connection, "isbn").getAvailableCopies());
    }

    @Test
    void readyReservationCanBeCancelledAndReleasesStock() throws Exception {
        createAccount("reader");
        assertTrue(m_books.insertBook(m_connection,
                new Book("isbn", "Java", "Author", "C", 1, 0)));
        LibraryService service = service();
        BookReservation waiting = service.reserve("reader", "isbn");
        Date now = new Date();
        assertTrue(m_reservations.updateStatus(m_connection, waiting.getId(),
                ReservationStatus.READY, new Timestamp(now.getTime()),
                new Timestamp(now.getTime() + 100000L)));
        BookReservation cancelled = service.cancelReservation("reader", waiting.getId());
        assertEquals(ReservationStatus.CANCELLED, cancelled.getStatus());
        assertEquals(1, m_books.findByIsbn(m_connection, "isbn").getAvailableCopies());
        assertNull(m_reservations.findActiveById(m_connection, waiting.getId()));
    }

    @Test
    void accountMemoryDaoReturnsCopiesAndSoftDeletes() throws Exception {
        createAccount("reader");
        LibraryAccount first = m_accounts.findByUserUuid("reader");
        first.setBorrowLimit(1);
        assertEquals(30, m_accounts.findByUserUuid("reader").getBorrowLimit());
        assertTrue(m_accounts.softDelete("reader", new Date()));
        assertFalse(m_accounts.findByUserUuid("reader").isOperational());
    }

    private void createAccount(String user) throws Exception {
        assertTrue(m_accounts.insert(new LibraryAccount(user, 30, new Date())));
    }

    private LibraryService service() {
        return new LibraryService(m_source, m_accounts, m_books,
                m_borrows, m_reservations);
    }
}
