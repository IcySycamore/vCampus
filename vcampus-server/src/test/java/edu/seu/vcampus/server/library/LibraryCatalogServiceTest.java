package edu.seu.vcampus.server.library;

import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.library.entity.Book;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.message.PageResponse;
import edu.seu.vcampus.common.library.dto.BookQuery;
import edu.seu.vcampus.common.library.dto.BookRef;
import java.sql.SQLException;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static edu.seu.vcampus.server.library.LibraryCatalogFixture.ISBN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** 管理员录入、库存推导、逻辑下架和失败回滚的业务测试。 */
class LibraryCatalogServiceTest {
    private LibraryCatalogFixture f;

    @BeforeEach
    void setUp() throws Exception {
        f = new LibraryCatalogFixture();
    }

    @ParameterizedTest
    @ValueSource(strings = {"管理员", "admin", "ADMIN"})
    void createIgnoresForgedAvailableAndWithdrawal(String role) throws Exception {
        when(f.books.insertBook(eq(f.connection), any(Book.class))).thenReturn(true);
        Book input = LibraryCatalogFixture.book(4, 999);
        input.setWithdrawn(true);
        Message response = f.send(Command.LIBRARY_CREATE_BOOK, input, role);
        assertEquals(StatusCode.SUCCESS, response.getStatusCode());
        Book saved = (Book) response.getData();
        assertEquals(4, saved.getAvailableCopies());
        assertEquals("Java", saved.getTitle());
        assertFalse(saved.isWithdrawn());
        assertEquals(999, input.getAvailableCopies());
        assertEquals(Long.valueOf(88L), response.getUid());
        verify(f.connection).commit();
        verify(f.connection).close();
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 3, 8})
    void updatePreservesLoansAndWithdrawal(int total) throws Exception {
        Book current = LibraryCatalogFixture.book(5, 3);
        current.setWithdrawn(true);
        when(f.books.findByIsbn(f.connection, ISBN)).thenReturn(current);
        when(f.books.updateBook(eq(f.connection), any(Book.class))).thenReturn(true);
        Message response = f.send(Command.LIBRARY_UPDATE_BOOK,
                LibraryCatalogFixture.book(total, 999), "admin");
        assertEquals(StatusCode.SUCCESS, response.getStatusCode());
        Book saved = (Book) response.getData();
        assertEquals(total - 2, saved.getAvailableCopies());
        assertTrue(saved.isWithdrawn());
        verify(f.connection).commit();
    }

    @Test
    void cannotReduceTotalBelowOutstandingLoans() throws Exception {
        when(f.books.findByIsbn(f.connection, ISBN)).thenReturn(LibraryCatalogFixture.book(5, 3));
        Message response = f.send(Command.LIBRARY_UPDATE_BOOK,
                LibraryCatalogFixture.book(1, 1), "管理员");
        assertEquals(StatusCode.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getData().toString().contains("未归还数量：2"));
        verify(f.books, never()).updateBook(eq(f.connection), any(Book.class));
        verify(f.connection).rollback();
    }

    @Test
    void duplicateIsbnDoesNotInsert() throws Exception {
        when(f.books.findByIsbn(f.connection, ISBN)).thenReturn(LibraryCatalogFixture.book(5, 3));
        assertEquals(StatusCode.BAD_REQUEST, f.send(Command.LIBRARY_CREATE_BOOK,
                LibraryCatalogFixture.book(1, 1), "admin").getStatusCode());
        verify(f.books, never()).insertBook(eq(f.connection), any(Book.class));
        verify(f.connection).rollback();
    }

    @Test
    void concurrentDuplicateInsertRollsBack() throws Exception {
        when(f.books.insertBook(eq(f.connection), any(Book.class))).thenReturn(false);
        assertEquals(StatusCode.BAD_REQUEST, f.send(Command.LIBRARY_CREATE_BOOK,
                LibraryCatalogFixture.book(1, 1), "admin").getStatusCode());
        verify(f.connection).rollback();
    }

    @Test
    void withdrawalRetainsOutstandingLoansAndDoesNotMutateSnapshot() throws Exception {
        Book current = LibraryCatalogFixture.book(5, 3);
        when(f.books.findByIsbn(f.connection, ISBN)).thenReturn(current);
        when(f.books.withdrawBook(f.connection, ISBN)).thenReturn(true);
        Message response = f.send(Command.LIBRARY_WITHDRAW_BOOK, new BookRef(ISBN), "admin");
        assertEquals(StatusCode.SUCCESS, response.getStatusCode());
        Book result = (Book) response.getData();
        assertTrue(result.isWithdrawn());
        assertEquals(5, result.getTotalCopies());
        assertEquals(3, result.getAvailableCopies());
        assertFalse(current.isWithdrawn());
        verify(f.connection).commit();
    }

    @Test
    void withdrawalRequiresExplicitBookReference() {
        Message response = f.send(Command.LIBRARY_WITHDRAW_BOOK, ISBN, "admin");
        assertEquals(StatusCode.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getData().toString().contains("BookRef"));
        verifyNoInteractions(f.books);
    }

    @Test
    void failedWriteRollsBackWithoutCommitting() throws Exception {
        when(f.books.insertBook(eq(f.connection), any(Book.class)))
                .thenThrow(new SQLException("database detail"));
        Message response = f.send(Command.LIBRARY_CREATE_BOOK,
                LibraryCatalogFixture.book(1, 1), "admin");
        assertEquals(StatusCode.INTERNAL_ERROR, response.getStatusCode());
        assertEquals("图书馆服务暂时不可用", response.getData());
        verify(f.connection).rollback();
        verify(f.connection, never()).commit();
        verify(f.connection).close();
    }

    @Test
    void catalogIncludesWithdrawnBooksAndMissingBookReturns404() throws Exception {
        Book withdrawn = LibraryCatalogFixture.book(5, 3);
        withdrawn.setWithdrawn(true);
        when(f.books.searchCatalog(any(BookQuery.class))).thenReturn(new PageResponse<Book>(
                Collections.singletonList(withdrawn), 1, 1, 20));
        assertEquals(StatusCode.SUCCESS, f.send(Command.LIBRARY_CATALOG_SEARCH,
                new BookQuery(), "admin").getStatusCode());
        verify(f.books).searchCatalog(any(BookQuery.class));
        assertEquals(StatusCode.NOT_FOUND,
                f.send(Command.LIBRARY_WITHDRAW_BOOK, new BookRef(ISBN), "admin").getStatusCode());
    }
}
