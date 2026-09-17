package edu.seu.vcampus.client.view.library;

import edu.seu.vcampus.client.library.LibraryService;
import edu.seu.vcampus.common.library.LibraryPolicy;
import edu.seu.vcampus.common.library.dto.BookQuery;
import edu.seu.vcampus.common.library.entity.Book;
import edu.seu.vcampus.common.library.entity.BorrowRecord;
import edu.seu.vcampus.common.library.entity.BookReservation;
import edu.seu.vcampus.common.library.entity.PopularBorrow;
import edu.seu.vcampus.common.message.PageResponse;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import javax.swing.SwingUtilities;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

/** 页面只依赖业务 API；协议和真实会话在 client.library 测试中验证。 */
final class LibraryUiFixture {
    final LibraryService api = mock(LibraryService.class);
    LibraryPanel panel;

    LibraryUiFixture(String role) throws Exception {
        when(api.isLoggedIn()).thenReturn(true);
        when(api.borrowLimit()).thenReturn(LibraryPolicy.borrowLimit(role));
        when(api.canManageCatalog()).thenReturn(LibraryPolicy.canManage(role));
        when(api.listMyBorrows()).thenReturn(records(0, 0));
        when(api.listMyReservations()).thenReturn(Collections.<BookReservation>emptyList());
        when(api.listPopularBorrows()).thenReturn(Collections.<PopularBorrow>emptyList());
        when(api.searchBooks(any(BookQuery.class))).thenReturn(new PageResponse<Book>(
                Collections.singletonList(new Book("9787302423287", "Java", "Author",
                        "计算机", 4, 2)), 1, 1, 20));
        when(api.searchCatalog(any(BookQuery.class))).thenReturn(PageResponse.<Book>empty());
        ui(new Runnable() {
            @Override
            public void run() {
                panel = new LibraryPanel(api);
            }
        });
    }

    void refresh() throws Exception {
        ui(new Runnable() {
            @Override
            public void run() {
                panel.refresh();
            }
        });
    }

    static List<BorrowRecord> records(int active, int returned) {
        List<BorrowRecord> records = new ArrayList<BorrowRecord>();
        for (int i = 0; i < active + returned; i++) {
            BorrowRecord record = new BorrowRecord("uuid-001", "9787302423287", "Java",
                    new Date(0), new Date(1));
            record.setId((long) i + 1);
            if (i >= active) {
                record.setReturnedAt(new Date());
            }
            records.add(record);
        }
        return records;
    }

    static Component find(Container parent, String name) {
        for (Component child : parent.getComponents()) {
            if (name.equals(child.getName())) {
                return child;
            }
            if (child instanceof Container) {
                Component found = find((Container) child, name);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    static void ui(Runnable task) throws Exception {
        SwingUtilities.invokeAndWait(task);
    }

    static void await(final Runnable assertion) throws Exception {
        long deadline = System.nanoTime() + 5000000000L;
        final AssertionError[] error = new AssertionError[1];
        do {
            ui(new Runnable() {
                @Override
                public void run() {
                    try {
                        assertion.run();
                        error[0] = null;
                    } catch (AssertionError failure) {
                        error[0] = failure;
                    }
                }
            });
            if (error[0] == null) {
                return;
            }
            Thread.sleep(10);
        } while (System.nanoTime() < deadline);
        throw error[0];
    }
}
