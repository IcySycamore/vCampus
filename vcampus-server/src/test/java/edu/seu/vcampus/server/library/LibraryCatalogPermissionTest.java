package edu.seu.vcampus.server.library;

import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.library.entity.Book;
import edu.seu.vcampus.common.message.Message;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** 普通读者可保存资料，录入、下架和已下架查询仍只对管理员开放。 */
class LibraryCatalogPermissionTest {
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"学生", "教师", "teacher", "", "other"})
    void nonAdminCannotUseAdminOnlyCommandsEvenWithForgedSender(String role) throws Exception {
        LibraryCatalogFixture f = new LibraryCatalogFixture();
        int[] commands = {Command.LIBRARY_CREATE_BOOK, Command.LIBRARY_WITHDRAW_BOOK,
            Command.LIBRARY_CATALOG_SEARCH};
        for (int command : commands) {
            Message response = f.send(command, LibraryCatalogFixture.book(1, 1), role);
            assertEquals(StatusCode.FORBIDDEN, response.getStatusCode());
        }
        verifyNoInteractions(f.source, f.books, f.borrows);
    }

    @ParameterizedTest
    @ValueSource(strings = {"学生", "student", "教师", "teacher"})
    void readersCanSaveSelectedBookMetadata(String role) throws Exception {
        LibraryCatalogFixture f = new LibraryCatalogFixture();
        when(f.books.findByIsbn(f.connection, LibraryCatalogFixture.ISBN))
                .thenReturn(LibraryCatalogFixture.book(4, 4));
        when(f.books.updateBook(eq(f.connection), any(Book.class))).thenReturn(true);
        Message response = f.send(Command.LIBRARY_UPDATE_BOOK,
                LibraryCatalogFixture.book(4, 4), role);
        assertEquals(StatusCode.SUCCESS, response.getStatusCode());
    }

    @Test
    void rejectsMalformedMetadataBeforeOpeningDatabase() throws Exception {
        LibraryCatalogFixture f = new LibraryCatalogFixture();
        Book emptyTitle = LibraryCatalogFixture.book(1, 1);
        emptyTitle.setTitle(" ");
        Book longAuthor = LibraryCatalogFixture.book(1, 1);
        longAuthor.setAuthor(new String(new char[101]).replace('\0', 'a'));
        Book emptyCategory = LibraryCatalogFixture.book(1, 1);
        emptyCategory.setCategory(null);
        for (Object invalid : new Object[] {null, "book", emptyTitle, longAuthor, emptyCategory,
                LibraryCatalogFixture.book(-1, 1)}) {
            assertEquals(StatusCode.BAD_REQUEST,
                    f.send(Command.LIBRARY_CREATE_BOOK, invalid, "admin").getStatusCode());
        }
        verifyNoInteractions(f.source, f.books);
    }
}
