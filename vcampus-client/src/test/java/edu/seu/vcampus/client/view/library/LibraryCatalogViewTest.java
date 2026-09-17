package edu.seu.vcampus.client.view.library;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JSplitPane;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验证统一查询页的分栏布局与明确编辑状态。 */
class LibraryCatalogViewTest {
    @Test
    void startsInSelectionModeAndExposesOneSharedSplitView() throws Exception {
        LibraryUiFixture fixture = new LibraryUiFixture("学生");
        assertNotNull(LibraryUiFixture.find(fixture.panel, "libraryCatalogDiscovery"));
        assertNotNull(LibraryUiFixture.find(fixture.panel, "libraryCatalogSplit"));
        assertTrue(LibraryUiFixture.find(fixture.panel,
                "libraryCatalogSplit") instanceof JSplitPane);
        JLabel mode = (JLabel) LibraryUiFixture.find(fixture.panel, "catalogEditorMode");
        assertTrue(mode.getText().contains("选择"));
        JButton save = (JButton) LibraryUiFixture.find(fixture.panel, "catalogAction2");
        assertFalse(save.isEnabled());
    }

    @Test
    void onlyShowsCompleteBookInformationAfterSearch() throws Exception {
        final LibraryUiFixture fixture = new LibraryUiFixture("学生");
        final JSplitPane split = (JSplitPane) LibraryUiFixture.find(
                fixture.panel, "libraryCatalogSplit");
        assertFalse(split.getParent().isVisible());
        LibraryUiFixture.ui(new Runnable() {
            @Override
            public void run() {
                ((JButton) LibraryUiFixture.find(
                        fixture.panel, "catalogAction0")).doClick();
            }
        });
        LibraryUiFixture.await(new Runnable() {
            @Override
            public void run() {
                assertTrue(split.getParent().isVisible());
            }
        });
    }
}
