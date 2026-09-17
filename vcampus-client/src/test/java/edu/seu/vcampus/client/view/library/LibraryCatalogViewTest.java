package edu.seu.vcampus.client.view.library;

import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验证查询页无表单，管理员使用相邻管理页。 */
class LibraryCatalogViewTest {
    @Test
    void queryPageUsesFullWidthTableWithoutEditor() throws Exception {
        LibraryUiFixture fixture = new LibraryUiFixture("学生");
        assertNotNull(LibraryUiFixture.find(fixture.panel, "catalogTable"));
        assertNull(LibraryUiFixture.find(fixture.panel, "libraryCatalogSplit"));
        assertNull(LibraryUiFixture.find(fixture.panel, "catalogEditorMode"));
    }

    @Test
    void adminGetsManagementTabImmediatelyAfterQuery() throws Exception {
        LibraryUiFixture fixture = new LibraryUiFixture("管理员");
        JTabbedPane tabs = (JTabbedPane) LibraryUiFixture.find(fixture.panel, "libraryTabs");
        assertEquals("图书查询", tabs.getTitleAt(1));
        assertEquals("图书管理", tabs.getTitleAt(2));
        assertNull(LibraryUiFixture.find(fixture.panel, "libraryCatalogSplit"));
        assertTrue(LibraryUiFixture.find(fixture.panel,
                "libraryManagementSplit") instanceof JSplitPane);
        assertNotNull(LibraryUiFixture.find(fixture.panel, "managementTable"));
        assertNotNull(LibraryUiFixture.find(fixture.panel, "catalogEditorMode"));
        assertNotNull(LibraryUiFixture.find(fixture.panel, "libraryManagement"));
    }
}
