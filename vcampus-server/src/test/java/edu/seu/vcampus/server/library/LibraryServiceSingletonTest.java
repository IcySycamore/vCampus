package edu.seu.vcampus.server.library;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

/**
 * 验证完整图书馆服务在服务器进程内只初始化一次，且装配入口只有图书馆模块一处。
 *
 * <p>
 * 单例拿在模块手上（{@link LibraryModule#service()}）而不是服务自己身上：先前服务自带 {@code getInstance(依赖...)}
 * 做「首次调用即定型」的单例，谁先调用谁决定实例 —— 测试先跑就 会把整个进程的图书馆服务钉成一个 Mock，而且失败得很安静。现在依赖由模块提供，顺序不再敏感。
 */
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

    /** 构造只做接线，不碰数据库，因此这里无需可用库即可断言单例。 */
    @Test
    void productionServiceIsSingleton() {
        assertSame(LibraryModule.service(), LibraryModule.service());
    }

    /** 四个 DAO 与连接来源同样各自只有一份，否则演示种子会写进别的实例。 */
    @Test
    void productionDaosAreSingletons() {
        assertSame(LibraryModule.source(), LibraryModule.source());
        assertSame(LibraryModule.accountDao(), LibraryModule.accountDao());
        assertSame(LibraryModule.bookDao(), LibraryModule.bookDao());
        assertSame(LibraryModule.borrowDao(), LibraryModule.borrowDao());
        assertSame(LibraryModule.reservationDao(), LibraryModule.reservationDao());
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
