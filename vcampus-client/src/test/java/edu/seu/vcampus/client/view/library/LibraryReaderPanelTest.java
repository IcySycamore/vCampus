package edu.seu.vcampus.client.view.library;

import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
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
        assertEquals("图书查询", tabs.getTitleAt(1));
        assertEquals("我的预约", tabs.getTitleAt(3));
    }

    @Test
    void tablesShareSortingAndPersonalListsExposeKeywordFilters() throws Exception {
        LibraryUiFixture fixture = new LibraryUiFixture("学生");

        JTable catalog = table(fixture, "catalogTable");
        JTable borrows = table(fixture, "libraryBorrowTable");
        JTable reservations = table(fixture, "libraryReservationTable");
        assertNotNull(catalog.getRowSorter());
        assertNotNull(borrows.getRowSorter());
        assertNotNull(reservations.getRowSorter());
        assertNotNull(LibraryUiFixture.find(fixture.panel, "libraryBorrowFilter"));
        assertNotNull(LibraryUiFixture.find(fixture.panel, "libraryReservationFilter"));
    }

    @Test
    void returnedLoansDisappearAndCurrentLoansCanBeFilteredAndSorted() throws Exception {
        final LibraryUiFixture fixture = new LibraryUiFixture("学生");
        fixture.setBorrows(LibraryUiFixture.records(10, 1));
        fixture.refresh();
        LibraryUiFixture.await(new Runnable() {
            @Override
            public void run() {
                JTable table = table(fixture, "libraryBorrowTable");
                assertEquals(10, table.getRowCount());
            }
        });
        LibraryUiFixture.ui(new Runnable() {
            @Override
            public void run() {
                JTable table = table(fixture, "libraryBorrowTable");
                table.getRowSorter().toggleSortOrder(0);
                table.getRowSorter().toggleSortOrder(0);
                assertEquals(10L, table.getValueAt(0, 0));
                JTextField filter = (JTextField) LibraryUiFixture.find(
                        fixture.panel, "libraryBorrowFilter");
                filter.setText("不存在的书");
                assertEquals(0, table.getRowCount());
                filter.setText("Java");
                assertEquals(10, table.getRowCount());
            }
        });
    }

    private static JTable table(LibraryUiFixture fixture, String name) {
        return (JTable) LibraryUiFixture.find(fixture.panel, name);
    }
}
