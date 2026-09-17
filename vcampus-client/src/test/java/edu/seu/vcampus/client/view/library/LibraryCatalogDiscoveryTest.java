package edu.seu.vcampus.client.view.library;

import edu.seu.vcampus.common.library.entity.Book;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JButton;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验证检索首页根据真实馆藏生成热门入口并可直接发起检索。 */
class LibraryCatalogDiscoveryTest {
    @Test
    void ranksBorrowedBooksAndTurnsRecommendationsIntoSearches() throws Exception {
        final AtomicReference<String> selected = new AtomicReference<String>();
        final LibraryCatalogDiscovery discovery = new LibraryCatalogDiscovery(
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent event) {
                        selected.set(event.getActionCommand());
                    }
                });
        LibraryUiFixture.ui(new Runnable() {
            @Override
            public void run() {
                discovery.showBooks(Arrays.asList(
                        new Book("1", "Java入门", "作者甲", "计算机", 10, 9),
                        new Book("2", "算法设计", "作者乙", "软件工程", 6, 2)));
            }
        });

        JButton popular = (JButton) LibraryUiFixture.find(discovery, "libraryPopularBook0");
        JButton term = (JButton) LibraryUiFixture.find(discovery, "libraryHotTerm0");
        assertNotNull(popular);
        assertNotNull(term);
        assertTrue(popular.getText().contains("算法设计"));
        LibraryUiFixture.ui(new Runnable() {
            @Override
            public void run() {
                ((JButton) LibraryUiFixture.find(
                        discovery, "libraryPopularBook0")).doClick();
            }
        });
        assertEquals("算法设计", selected.get());
    }
}
