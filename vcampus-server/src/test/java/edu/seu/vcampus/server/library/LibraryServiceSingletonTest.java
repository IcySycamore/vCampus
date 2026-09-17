package edu.seu.vcampus.server.library;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

/** 验证完整图书馆服务在服务器进程内只初始化一次。 */
class LibraryServiceSingletonTest {
    @Test
    void rejectsIncompleteDependenciesDuringInitialization() {
        LibraryConnectionSource source = mock(LibraryConnectionSource.class);
        LibraryAccountDao accounts = mock(LibraryAccountDao.class);
        BookDao books = mock(BookDao.class);
        BorrowDao borrows = mock(BorrowDao.class);
        ReservationDao reservations = mock(ReservationDao.class);

        assertIncomplete(null, accounts, books, borrows, reservations);
        assertIncomplete(source, null, books, borrows, reservations);
        assertIncomplete(source, accounts, null, borrows, reservations);
        assertIncomplete(source, accounts, books, null, reservations);
        assertIncomplete(source, accounts, books, borrows, null);
    }

    @Test
    void completeServiceIsSingleton() {
        LibraryService first = LibraryService.getInstance(
                mock(LibraryConnectionSource.class), mock(LibraryAccountDao.class),
                mock(BookDao.class), mock(BorrowDao.class), mock(ReservationDao.class));
        LibraryService second = LibraryService.getInstance(
                mock(LibraryConnectionSource.class), mock(LibraryAccountDao.class),
                mock(BookDao.class), mock(BorrowDao.class), mock(ReservationDao.class));

        assertSame(first, second);
    }

    private void assertIncomplete(final LibraryConnectionSource source,
            final LibraryAccountDao accounts, final BookDao books,
            final BorrowDao borrows, final ReservationDao reservations) {
        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() {
                new LibraryService(source, accounts, books, borrows, reservations);
            }
        });
    }
}
