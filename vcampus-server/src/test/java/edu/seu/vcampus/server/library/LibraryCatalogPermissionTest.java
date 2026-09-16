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
import static org.mockito.Mockito.verifyNoInteractions;

/** 四个管理命令均验证会话角色，并在写库前校验图书资料。 */
class LibraryCatalogPermissionTest {
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"学生", "教师", "teacher", "", "other"})
    void nonAdminCannotUseManagementCommandsEvenWithForgedSender(String role) throws Exception {
        LibraryCatalogFixture f = new LibraryCatalogFixture();
        for (int command = Command.LIBRARY_CREATE_BOOK;
                command <= Command.LIBRARY_CATALOG_SEARCH; command++) {
            Message response = f.send(command, LibraryCatalogFixture.book(1, 1), role);
            assertEquals(StatusCode.FORBIDDEN, response.getStatusCode());
        }
        verifyNoInteractions(f.source, f.books, f.borrows);
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
