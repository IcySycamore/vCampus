package edu.seu.vcampus.client.view.library;

import edu.seu.vcampus.client.library.LibraryService;
import edu.seu.vcampus.common.library.dto.BookQuery;
import edu.seu.vcampus.common.library.entity.Book;
import edu.seu.vcampus.common.message.PageResponse;
import java.util.Arrays;
import javax.swing.JPanel;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 馆藏轮播异步加载和卡片渲染测试。 */
class LibraryCatalogCarouselTest {
    @Test
    void refreshRendersEveryCatalogBook() throws Exception {
        final LibraryService api = mock(LibraryService.class);
        when(api.isLoggedIn()).thenReturn(true);
        when(api.searchBooks(any(BookQuery.class))).thenReturn(new PageResponse<Book>(
                Arrays.asList(book("1", "Java"), book("2", "数据库")), 2, 1, 20));
        final LibraryCatalogCarousel[] carousel = new LibraryCatalogCarousel[1];
        LibraryUiFixture.ui(new Runnable() {
            @Override
            public void run() {
                carousel[0] = new LibraryCatalogCarousel(api);
                carousel[0].refresh();
            }
        });
        LibraryUiFixture.await(new Runnable() {
            @Override
            public void run() {
                JPanel track = (JPanel) LibraryUiFixture.find(
                        carousel[0], "libraryHomeCatalogTrack");
                assertEquals(4, track.getComponentCount());
            }
        });
    }

    private Book book(String isbn, String title) {
        return new Book(isbn, title, "作者", "计算机", 2, 2);
    }
}
