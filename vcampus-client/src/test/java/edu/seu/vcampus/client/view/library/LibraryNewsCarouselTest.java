package edu.seu.vcampus.client.view.library;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** 验证活动资讯轮播的翻页入口。 */
class LibraryNewsCarouselTest {
    @Test
    void exposesPreviousAndNextControls() throws Exception {
        final LibraryNewsCarousel[] carousel = new LibraryNewsCarousel[1];
        LibraryUiFixture.ui(new Runnable() {
            @Override
            public void run() {
                carousel[0] = new LibraryNewsCarousel();
            }
        });
        assertNotNull(LibraryUiFixture.find(carousel[0], "libraryHomeNewsPrevious"));
        assertNotNull(LibraryUiFixture.find(carousel[0], "libraryHomeNewsNext"));
    }
}
