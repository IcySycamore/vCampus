package edu.seu.vcampus.client.view.library;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** 验证单本封面推荐轮播的翻页入口。 */
class LibraryReadingCarouselTest {
    @Test
    void exposesPreviousAndNextControls() throws Exception {
        final LibraryReadingCarousel[] carousel = new LibraryReadingCarousel[1];
        LibraryUiFixture.ui(new Runnable() {
            @Override
            public void run() {
                carousel[0] = new LibraryReadingCarousel();
            }
        });
        assertNotNull(LibraryUiFixture.find(carousel[0], "libraryHomeReadingPrevious"));
        assertNotNull(LibraryUiFixture.find(carousel[0], "libraryHomeReadingNext"));
    }
}
