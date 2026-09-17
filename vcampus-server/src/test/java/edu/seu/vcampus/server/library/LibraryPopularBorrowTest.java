package edu.seu.vcampus.server.library;

import edu.seu.vcampus.common.library.entity.BorrowRecord;
import edu.seu.vcampus.common.library.entity.PopularBorrow;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** 验证服务器根据每次借阅记录累计热门排行。 */
class LibraryPopularBorrowTest {
    @Test
    void countsEverySuccessfulBorrowRecord() throws Exception {
        BorrowDaoMemory borrows = new BorrowDaoMemory();
        borrows.insert(null, record("u1", "1", "Java"));
        borrows.insert(null, record("u2", "1", "Java"));
        borrows.insert(null, record("u1", "2", "数据库"));

        List<PopularBorrow> ranked = borrows.findPopular(5);

        assertEquals(2, ranked.size());
        assertEquals("Java", ranked.get(0).getTitle());
        assertEquals(2, ranked.get(0).getBorrowCount());
        assertEquals(1, ranked.get(1).getBorrowCount());
    }

    private BorrowRecord record(String user, String isbn, String title) {
        return new BorrowRecord(user, isbn, title, new Date(1L), new Date(2L));
    }
}
