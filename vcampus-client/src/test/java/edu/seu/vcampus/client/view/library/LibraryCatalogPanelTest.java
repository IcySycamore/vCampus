package edu.seu.vcampus.client.view.library;

import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.library.entity.Book;
import edu.seu.vcampus.common.library.dto.BookQuery;
import edu.seu.vcampus.common.message.PageResponse;
import java.util.Collections;
import javax.swing.JButton;
import javax.swing.JTabbedPane;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 验证查询与管理员图书管理页面相互独立。 */
class LibraryCatalogPanelTest {
    @Test
    void libraryTabsUseTheRedRoundedNavigationStyle() throws Exception {
        final LibraryUiFixture fixture = new LibraryUiFixture("学生");
        LibraryUiFixture.ui(new Runnable() {
            @Override
            public void run() {
                JTabbedPane tabs = (JTabbedPane) LibraryUiFixture.find(
                        fixture.panel, "libraryTabs");
                assertTrue(tabs.getUI() instanceof LibraryTabbedPaneUI);
                assertEquals(0, tabs.getSelectedIndex());
            }
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"学生", "student", "教师", "teacher"})
    void searchShowsCatalogWithoutEditorOrManagementActions(String role) throws Exception {
        LibraryUiFixture fixture = new LibraryUiFixture(role);
        assertTrue(LibraryUiFixture.find(fixture.panel, "libraryCatalog") != null);
        assertTrue(LibraryUiFixture.find(fixture.panel, "catalogAction0") != null);
        assertNull(LibraryUiFixture.find(fixture.panel, "catalogAction2"));
        assertNull(LibraryUiFixture.find(fixture.panel, "catalogAction1"));
        assertNull(LibraryUiFixture.find(fixture.panel, "catalogAction3"));
        assertNull(LibraryUiFixture.find(fixture.panel, "catalogIsbn"));
        assertNull(LibraryUiFixture.find(fixture.panel, "libraryCatalogSplit"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"管理员", "admin", "ADMIN"})
    void displaysWithdrawnBooksAndRestoresEditorAfterRejectedUpdate(String role) throws Exception {
        final LibraryUiFixture fixture = new LibraryUiFixture(role);
        final Book book = book();
        book.setWithdrawn(true);
        when(fixture.api.searchCatalog(any(BookQuery.class))).thenReturn(
                new PageResponse<Book>(Collections.singletonList(book), 1, 1, 20));
        when(fixture.api.updateBook(any(Book.class))).thenThrow(
                new ApiException(StatusCode.BAD_REQUEST, "馆藏总数不能少于未归还数量"));
        click(fixture, "manageAction", 0);
        LibraryUiFixture.await(new Runnable() {
            @Override
            public void run() {
                JTable table = (JTable) LibraryUiFixture.find(
                        fixture.panel, "managementTable");
                assertEquals(1, table.getRowCount());
                assertEquals("已下架", table.getValueAt(0, 6));
                table.setRowSelectionInterval(0, 0);
                assertFalse(((JTextField) LibraryUiFixture.find(
                        fixture.panel, "catalogIsbn")).isEditable());
                ((JSpinner) LibraryUiFixture.find(fixture.panel, "catalogTotal")).setValue(6);
            }
        });
        click(fixture, "manageAction", 2);
        LibraryUiFixture.await(new Runnable() {
            @Override
            public void run() {
                ArgumentCaptor<Book> update = ArgumentCaptor.forClass(Book.class);
                verify(fixture.api).updateBook(update.capture());
                assertEquals(6, update.getValue().getTotalCopies());
                assertTrue(((JButton) LibraryUiFixture.find(
                        fixture.panel, "manageAction2")).isEnabled());
            }
        });
    }

    @Test
    void createsBookAndRefreshesCatalog() throws Exception {
        final LibraryUiFixture fixture = new LibraryUiFixture("admin");
        when(fixture.api.createBook(any(Book.class))).thenReturn(book());
        when(fixture.api.searchCatalog(any(BookQuery.class))).thenReturn(
                new PageResponse<Book>(Collections.singletonList(book()), 1, 1, 20));
        LibraryUiFixture.ui(new Runnable() {
            @Override
            public void run() {
                ((JButton) LibraryUiFixture.find(
                        fixture.panel, "manageAction1")).doClick();
                fill(fixture, "catalogIsbn", "9787302423287");
                fill(fixture, "catalogTitle", "Java");
                fill(fixture, "catalogAuthor", "Author");
                fill(fixture, "catalogCategory", "计算机");
                ((JSpinner) LibraryUiFixture.find(fixture.panel, "catalogTotal")).setValue(4);
            }
        });
        click(fixture, "manageAction", 2);
        LibraryUiFixture.await(new Runnable() {
            @Override
            public void run() {
                verify(fixture.api).createBook(any(Book.class));
                JTable table = (JTable) LibraryUiFixture.find(
                        fixture.panel, "managementTable");
                assertEquals(1, table.getRowCount());
            }
        });
    }

    @Test
    void studentSearchUsesTheFullWidthCatalogTable() throws Exception {
        final LibraryUiFixture fixture = new LibraryUiFixture("学生");
        click(fixture, "catalogAction", 0);
        LibraryUiFixture.await(new Runnable() {
            @Override
            public void run() {
                JTable table = (JTable) LibraryUiFixture.find(fixture.panel, "catalogTable");
                assertEquals(JTable.AUTO_RESIZE_ALL_COLUMNS, table.getAutoResizeMode());
                assertEquals(1, table.getRowCount());
                table.setRowSelectionInterval(0, 0);
                JScrollPane scroll = (JScrollPane) LibraryUiFixture.find(
                        fixture.panel, "libraryCatalogScroll");
                assertEquals(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER,
                        scroll.getHorizontalScrollBarPolicy());
                assertNull(LibraryUiFixture.find(fixture.panel, "libraryCatalogSplit"));
            }
        });
    }

    private void fill(LibraryUiFixture fixture, String name, String text) {
        ((JTextField) LibraryUiFixture.find(fixture.panel, name)).setText(text);
    }

    private void click(final LibraryUiFixture fixture, final String prefix,
            final int action) throws Exception {
        LibraryUiFixture.ui(new Runnable() {
            @Override
            public void run() {
                ((JButton) LibraryUiFixture.find(
                        fixture.panel, prefix + action)).doClick();
            }
        });
    }

    private Book book() {
        return new Book("9787302423287", "Java", "Author", "计算机", 4, 2);
    }

}
