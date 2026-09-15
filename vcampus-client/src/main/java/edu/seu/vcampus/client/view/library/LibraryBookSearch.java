package edu.seu.vcampus.client.view.library;

import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.client.library.LibraryService;
import edu.seu.vcampus.client.view.UiTasks;
import edu.seu.vcampus.common.library.dto.BookQuery;
import edu.seu.vcampus.common.library.entity.Book;
import edu.seu.vcampus.common.message.PageResponse;
import javax.swing.JLabel;
import javax.swing.table.DefaultTableModel;

/** 异步加载普通馆藏分页，并丢弃迟到的旧查询结果。 */
final class LibraryBookSearch {
    private final LibraryService api;
    private final DefaultTableModel model;
    private final LibraryPager pager;
    private final JLabel status;
    private int generation;

    LibraryBookSearch(LibraryService api, DefaultTableModel model,
            LibraryPager pager, JLabel status) {
        this.api = api;
        this.model = model;
        this.pager = pager;
        this.status = status;
    }

    void load(String keyword, String field) {
        final BookQuery query = new BookQuery(keyword, field,
                pager.getPageNumber(), pager.getPageSize());
        final int current = ++generation;
        pager.loading();
        UiTasks.run(new UiTasks.Task<PageResponse<Book>>() {
            @Override
            public PageResponse<Book> run() {
                return api.searchBooks(query);
            }
        }, new UiTasks.Success<PageResponse<Book>>() {
            @Override
            public void accept(PageResponse<Book> page) {
                if (current == generation && api.isLoggedIn()) {
                    LibraryTableModels.showBooks(model, page.getItems());
                    pager.show(page);
                }
            }
        }, new UiTasks.Failure() {
            @Override
            public void accept(ApiException error) {
                if (current == generation) {
                    pager.failed();
                    status.setText("  " + error.getMessage());
                }
            }
        });
    }
}
