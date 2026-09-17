package edu.seu.vcampus.client.view.library;

import java.awt.Component;
import java.awt.Container;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验证图书馆首页入口、借阅概览和馆藏速览。 */
class LibraryHomePanelTest {
    @Test
    void homeIsFirstTabAndShowsBorrowStatusAndCatalog() throws Exception {
        final LibraryUiFixture fixture = new LibraryUiFixture("学生");
        fixture.setBorrows(LibraryUiFixture.records(2, 1));
        fixture.refresh();
        LibraryUiFixture.await(new Runnable() {
            @Override
            public void run() {
                JTabbedPane tabs = (JTabbedPane) LibraryUiFixture.find(
                        fixture.panel, "libraryTabs");
                assertEquals("图书馆首页", tabs.getTitleAt(0));
                assertEquals(0, tabs.getSelectedIndex());
                assertEquals("2", text(fixture, "libraryHomeBorrowed"));
                assertEquals("2", text(fixture, "libraryHomeOverdue"));
                assertEquals("28", text(fixture, "libraryHomeRemaining"));
                assertTrue(LibraryUiFixture.find(fixture.panel,
                        "libraryHomeNews") != null);
                assertTrue(LibraryUiFixture.find(fixture.panel,
                        "libraryHomeReading") != null);
                assertTrue(LibraryUiFixture.find(fixture.panel,
                        "libraryHomePopular") != null);
                assertTrue(LibraryUiFixture.find(fixture.panel,
                        "libraryHomeRules") != null);
            }
        });
    }

    @Test
    void newsAndReadingUseAlignedMediaFrames() throws Exception {
        final LibraryHomePanel[] home = new LibraryHomePanel[1];
        LibraryUiFixture.ui(new Runnable() {
            @Override
            public void run() {
                home[0] = new LibraryHomePanel(null);
                home[0].setSize(1180, 700);
                layout(home[0]);
            }
        });

        Component news = LibraryUiFixture.find(home[0], "libraryHomeNewsMedia");
        Component reading = LibraryUiFixture.find(home[0], "libraryHomeReadingMedia");
        Component newsPanel = LibraryUiFixture.find(home[0], "libraryHomeNews");
        Component readingPanel = LibraryUiFixture.find(home[0], "libraryHomeReading");
        assertNotNull(news);
        assertNotNull(reading);
        assertTrue(news.getHeight() > 0);
        assertEquals(news.getHeight(), reading.getHeight());
        assertTrue(newsPanel.getWidth() > readingPanel.getWidth());
    }

    private static void layout(Container container) {
        container.doLayout();
        for (Component component : container.getComponents()) {
            if (component instanceof Container) {
                layout((Container) component);
            }
        }
    }

    private String text(LibraryUiFixture fixture, String name) {
        return ((JLabel) LibraryUiFixture.find(fixture.panel, name)).getText();
    }
}
