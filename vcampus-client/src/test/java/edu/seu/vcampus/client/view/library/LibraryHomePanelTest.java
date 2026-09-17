package edu.seu.vcampus.client.view.library;

import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;

/** 验证图书馆首页入口、借阅概览和馆藏速览。 */
class LibraryHomePanelTest {
    @Test
    void homeIsFirstTabAndShowsBorrowStatusAndCatalog() throws Exception {
        final LibraryUiFixture fixture = new LibraryUiFixture("学生");
        doReturn(LibraryUiFixture.records(2, 1)).when(fixture.api).listMyBorrows();
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
            }
        });
    }

    private String text(LibraryUiFixture fixture, String name) {
        return ((JLabel) LibraryUiFixture.find(fixture.panel, name)).getText();
    }
}
