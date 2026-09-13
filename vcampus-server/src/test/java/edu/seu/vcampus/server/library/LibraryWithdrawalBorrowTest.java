package edu.seu.vcampus.server.library;

import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.library.entity.Book;
import edu.seu.vcampus.common.library.entity.BorrowRecord;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import static edu.seu.vcampus.server.library.LibraryCatalogFixture.ISBN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 下架阻止新借阅，既有借阅仍能归还并恢复库存。 */
class LibraryWithdrawalBorrowTest {
    @Test
    void readerSearchExcludesWithdrawnBooks() throws Exception {
        LibraryCatalogFixture f = new LibraryCatalogFixture();
        Book visible = LibraryCatalogFixture.book(5, 3);
        Book withdrawn = LibraryCatalogFixture.book(5, 3);
        withdrawn.setWithdrawn(true);
        when(f.books.search("", "all")).thenReturn(Arrays.asList(visible, withdrawn));
        List<?> result = (List<?>) f.send(Command.LIBRARY_SEARCH,
                new String[] {""}, "学生").getData();
        assertEquals(1, result.size());
    }

    @Test
    void withdrawnBookCannotBeBorrowedButCanStillBeReturned() throws Exception {
        LibraryCatalogFixture f = new LibraryCatalogFixture();
        Book book = LibraryCatalogFixture.book(5, 3);
        book.setWithdrawn(true);
        when(f.books.findByIsbn(f.connection, ISBN)).thenReturn(book);
        assertEquals(StatusCode.BAD_REQUEST,
                f.send(Command.LIBRARY_BORROW, ISBN, "学生").getStatusCode());
        verify(f.books, never()).adjustAvailable(f.connection, ISBN, -1);
        verify(f.borrows, never()).insert(eq(f.connection), any(BorrowRecord.class));

        BorrowRecord record = new BorrowRecord("001", ISBN, "Java", new Date(), new Date());
        record.setId(9L);
        when(f.borrows.findActiveById(f.connection, "001", 9L)).thenReturn(record);
        when(f.borrows.markReturned(eq(f.connection), eq(9L), any(Timestamp.class)))
                .thenReturn(true);
        when(f.books.adjustAvailable(f.connection, ISBN, 1)).thenReturn(true);
        assertEquals(StatusCode.SUCCESS,
                f.send(Command.LIBRARY_RETURN, 9L, "学生").getStatusCode());
        verify(f.books).adjustAvailable(f.connection, ISBN, 1);
        verify(f.connection).commit();
    }
}
