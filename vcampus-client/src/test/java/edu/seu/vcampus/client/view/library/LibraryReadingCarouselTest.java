package edu.seu.vcampus.client.view.library;

import edu.seu.vcampus.client.view.component.RoundedOutlineBorder;
import javax.swing.JButton;
import javax.swing.border.CompoundBorder;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        assertRounded(LibraryUiFixture.find(carousel[0], "libraryHomeReadingPrevious"));
        assertRounded(LibraryUiFixture.find(carousel[0], "libraryHomeReadingNext"));
    }

    private void assertRounded(java.awt.Component component) {
        assertNotNull(component);
        JButton button = (JButton) component;
        assertTrue(button.getBorder() instanceof CompoundBorder);
        assertTrue(((CompoundBorder) button.getBorder()).getOutsideBorder()
                instanceof RoundedOutlineBorder);
    }
}
