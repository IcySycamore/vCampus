package edu.seu.vcampus.client.view.library;

import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.client.library.LibraryService;
import edu.seu.vcampus.client.view.UiTasks;
import edu.seu.vcampus.common.library.dto.BookQuery;
import edu.seu.vcampus.common.library.entity.Book;
import edu.seu.vcampus.common.message.PageResponse;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/** 统一图书查询页的查询、保存、录入和下架控制器。 */
final class LibraryCatalogPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final LibraryService api;
    private final Runnable onChanged;
    private final LibraryPager pager;
    private final LibraryCatalogView view;
    private final boolean manager;
    private boolean busy;
    LibraryCatalogPanel(LibraryService api, Runnable onChanged, JButton borrowButton,
            JButton reserveButton, ActionListener borrow, ActionListener reserve) {
        this.api = api;
        this.onChanged = onChanged;
        manager = api != null && api.canManageCatalog();
        pager = new LibraryPager("libraryCatalog", new Runnable() {
            @Override
            public void run() {
                refresh(false);
            }
        });
        pager.setVisible(false);
        ActionListener catalogAction = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                perform(Integer.parseInt(event.getActionCommand()));
            }
        };
        view = new LibraryCatalogView(manager, manager || api != null && api.borrowLimit() > 0,
                api != null && api.borrowLimit() > 0 ? borrowButton : null,
                api != null && api.borrowLimit() > 0 ? reserveButton : null,
                borrow, reserve, catalogAction, selection());
        setLayout(new BorderLayout());
        setName("libraryCatalogPanel");
        add(view, BorderLayout.CENTER);
        JPanel footer = new JPanel(new BorderLayout(0, 6));
        footer.setOpaque(false);
        footer.add(pager, BorderLayout.NORTH);
        footer.add(view.status, BorderLayout.SOUTH);
        add(footer, BorderLayout.SOUTH);
        updateControls();
    }
    void refresh() {
        refresh(false, view.isShowingResults());
    }
    private void refresh(boolean resetPage) {
        refresh(resetPage, true);
    }
    private void refresh(boolean resetPage, final boolean showResults) {
        if (busy || api == null || !api.isLoggedIn()) {
            return;
        }
        if (resetPage) {
            pager.firstPage();
        }
        final BookQuery query = showResults
                ? view.query(pager.getPageNumber(), pager.getPageSize())
                : new BookQuery("", "all", 1, 100);
        busy = true;
        pager.setVisible(showResults);
        if (showResults) {
            pager.loading();
        }
        updateControls();
        UiTasks.run(new UiTasks.Task<PageResponse<Book>>() {
            @Override
            public PageResponse<Book> run() {
                return showResults && manager
                        ? api.searchCatalog(query) : api.searchBooks(query);
            }
        }, new UiTasks.Success<PageResponse<Book>>() {
            @Override
            public void accept(PageResponse<Book> result) {
                busy = false;
                if (showResults) {
                    view.show(result);
                    pager.show(result);
                } else {
                    view.showDiscovery(result.getItems());
                }
                updateControls();
            }
        }, failure());
    }
    private void perform(int action) {
        if (busy) {
            return;
        }
        if (action == 0) {
            view.recordSearch();
            refresh(true);
        } else if (action == 4) {
            pager.setVisible(false);
            view.showDiscovery();
        } else if (action == 1 && manager) {
            pager.setVisible(false);
            view.showEditor();
            view.clearSelection();
            view.editor.startCreate();
            view.status.setText("正在录入新书；填写右侧资料后点击“保存资料”");
            updateControls();
        } else if (action == 2) {
            save();
        } else if (action == 3 && manager) {
            withdraw();
        }
    }
    private void save() {
        if (!view.editor.isActive()) {
            view.status.setText("请先选择一本图书；管理员也可点击“录入新书”");
            return;
        }
        final boolean editing = view.editor.isEditing();
        final UiTasks.Task<Book> snapshot = view.editor.snapshot();
        submit(new UiTasks.Task<Book>() {
            @Override
            public Book run() {
                Book book = snapshot.run();
                return editing ? api.updateBook(book) : api.createBook(book);
            }
        });
    }
    private void withdraw() {
        final Book selected = view.selected();
        if (selected != null && JOptionPane.showConfirmDialog(this,
                "下架后停止借出，已借图书仍可归还。", "确认下架",
                JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            submit(new UiTasks.Task<Book>() {
                @Override
                public Book run() {
                    return api.withdrawBook(selected.getIsbn());
                }
            });
        }
    }
    private void submit(UiTasks.Task<Book> task) {
        busy = true;
        updateControls();
        UiTasks.run(task, new UiTasks.Success<Book>() {
            @Override
            public void accept(Book book) {
                busy = false;
                refresh(false);
                onChanged.run();
            }
        }, failure());
    }
    private UiTasks.Failure failure() {
        return new UiTasks.Failure() {
            @Override
            public void accept(ApiException error) {
                busy = false;
                pager.failed();
                view.status.setText(error.getMessage());
                updateControls();
            }
        };
    }

    private ListSelectionListener selection() {
        return new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent event) {
                Book selected = view.selected();
                if (!event.getValueIsAdjusting() && !busy && selected != null) {
                    view.editor.edit(selected);
                    view.status.setText("已选择《" + selected.getTitle()
                            + "》；可修改右侧资料后保存");
                    updateControls();
                }
            }
        };
    }

    String selectedIsbn() {
        Book selected = view.selected();
        return selected == null ? null : selected.getIsbn();
    }

    private void updateControls() {
        view.updateControls(api != null && api.isLoggedIn() && !busy);
    }
}
