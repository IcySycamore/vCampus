package edu.seu.vcampus.server.library;

import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.library.entity.PopularBorrow;
import edu.seu.vcampus.common.message.Message;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/** 验证所有登录身份都能读取服务器热门借阅排行。 */
class LibraryPopularBorrowCommandTest {
    @Test
    void returnsAggregatedRankingForReader() throws Exception {
        LibraryCatalogFixture fixture = new LibraryCatalogFixture();
        when(fixture.borrows.findPopular(5)).thenReturn(Collections.singletonList(
                new PopularBorrow(LibraryCatalogFixture.ISBN, "Java", 12)));

        Message response = fixture.send(Command.LIBRARY_POPULAR_BORROWS, null, "学生");

        assertEquals(StatusCode.SUCCESS, response.getStatusCode());
        PopularBorrow item = (PopularBorrow) ((List<?>) response.getData()).get(0);
        assertEquals(12, item.getBorrowCount());
    }
}
