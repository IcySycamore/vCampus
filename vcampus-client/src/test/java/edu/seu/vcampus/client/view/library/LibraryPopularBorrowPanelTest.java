package edu.seu.vcampus.client.view.library;

import edu.seu.vcampus.client.library.LibraryService;
import edu.seu.vcampus.common.library.entity.PopularBorrow;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 验证首页展示服务器返回的热门借阅排行。 */
class LibraryPopularBorrowPanelTest {
    @Test
    void rendersServerRanking() throws Exception {
        LibraryService api = mock(LibraryService.class);
        when(api.isLoggedIn()).thenReturn(true);
        when(api.listPopularBorrows()).thenReturn(Arrays.asList(
                new PopularBorrow("1", "Java", 8),
                new PopularBorrow("2", "数据库", 5)));
        final LibraryPopularBorrowPanel panel = new LibraryPopularBorrowPanel(api);
        panel.refresh();
        LibraryUiFixture.await(new Runnable() {
            @Override
            public void run() {
                assertNotNull(LibraryUiFixture.find(panel, "libraryPopularRow1"));
                assertNotNull(LibraryUiFixture.find(panel, "libraryPopularRow2"));
            }
        });
    }
}
