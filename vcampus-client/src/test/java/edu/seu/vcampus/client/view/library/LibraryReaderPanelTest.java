package edu.seu.vcampus.client.view.library;

import javax.swing.JTabbedPane;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** 读者页面公开预约、续借、归还和罚款操作入口。 */
class LibraryReaderPanelTest {
    @Test
    void studentSeesHomeAndAllReaderActions() throws Exception {
        LibraryUiFixture fixture = new LibraryUiFixture("学生");

        assertNotNull(LibraryUiFixture.find(fixture.panel, "libraryReserve"));
        assertNotNull(LibraryUiFixture.find(fixture.panel, "libraryReturn"));
        assertNotNull(LibraryUiFixture.find(fixture.panel, "libraryRenew"));
        assertNotNull(LibraryUiFixture.find(fixture.panel, "libraryFinePay"));
        assertNotNull(LibraryUiFixture.find(fixture.panel, "libraryReservationCancel"));
        JTabbedPane tabs = (JTabbedPane) LibraryUiFixture.find(
                fixture.panel, "libraryTabs");
        assertEquals(4, tabs.getTabCount());
        assertEquals("图书馆首页", tabs.getTitleAt(0));
        assertEquals("我的预约", tabs.getTitleAt(3));
    }
}
