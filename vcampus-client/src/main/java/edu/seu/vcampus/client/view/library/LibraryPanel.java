package edu.seu.vcampus.client.view.library;

import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.client.library.LibraryService;
import edu.seu.vcampus.client.view.UiTasks;
import edu.seu.vcampus.client.view.theme.UiFactory;
import edu.seu.vcampus.client.view.theme.UiIcons;
import edu.seu.vcampus.client.view.theme.UiTheme;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTabbedPane;
import javax.swing.table.DefaultTableModel;

/** 图书馆页面：提供检索、借阅、续借、预约、归还和滞纳金缴纳。 */
public class LibraryPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final String[] SEARCH_FIELDS = {"all", "title", "author", "isbn"};
    private final JTextField keyword = new JTextField(22);
    private final JComboBox<String> field = new JComboBox<String>(
            new String[] {"全部字段", "书名", "作者", "ISBN"});
    private final DefaultTableModel bookModel = LibraryTableModels.create(
            new String[] {"ISBN", "书名", "作者", "分类", "可借数量"});
    private final JTable bookTable = new JTable(bookModel);
    private final JLabel status = new JLabel("  当前为离线预览");
    private final JButton reserveButton = UiFactory.secondaryButton("预约所选", "borrow");
    private final LibraryService api;
    private final LibraryPager pager;
    private final LibraryBookSearch bookSearch;
    private final LibraryBorrowPanel borrows;
    private final LibraryReservationPanel reservations;
    private final LibraryCatalogPanel catalog;
    private boolean changing;

    /** 创建离线预览页面。 */
    public LibraryPanel() {
        this(null);
    }

    /**
     * 创建图书馆页面。
     * @param api 图书馆 API；null 表示离线预览
     */
    public LibraryPanel(LibraryService api) {
        this.api = api;
        reserveButton.setName("libraryReserve");
        reserveButton.setEnabled(api != null && api.borrowLimit() > 0);
        pager = new LibraryPager("libraryBooks", new Runnable() {
            @Override
            public void run() {
                search(false);
            }
        });
        bookSearch = new LibraryBookSearch(api, bookModel, pager, status);
        Runnable afterChange = new Runnable() {
            @Override
            public void run() {
                search(false);
            }
        };
        borrows = new LibraryBorrowPanel(api, status, afterChange);
        reservations = new LibraryReservationPanel(api, status, afterChange);
        setLayout(new BorderLayout(0, 18));
        setBackground(UiTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(30, 34, 26, 34));
        add(LibraryViewBuilder.createHeading(), BorderLayout.NORTH);
        LibraryViewBuilder builder = new LibraryViewBuilder(keyword, field, bookTable,
                borrows.quota.borrowButton, reserveButton, pager);
        JTabbedPane tabs = builder.createTabs(action(0), action(1), action(2));
        if (api != null && api.borrowLimit() > 0) {
            tabs.addTab("我的借阅", UiIcons.load("borrow", 18), borrows);
            tabs.addTab("我的预约", UiIcons.load("borrow", 18), reservations);
        }
        catalog = api != null && api.canManageCatalog()
                ? new LibraryCatalogPanel(api, afterChange) : null;
        if (catalog != null) {
            tabs.addTab("馆藏管理", catalog);
        }
        add(tabs, BorderLayout.CENTER);
        add(LibraryViewBuilder.createFooter(status, borrows.quota.label), BorderLayout.SOUTH);
    }

    /** 进入页面时刷新馆藏、借阅和预约记录。 */
    public void refresh() {
        if (available()) {
            search(false);
            refreshReader();
            if (catalog != null) {
                catalog.refresh();
            }
        }
    }

    private boolean available() {
        return api != null && api.isLoggedIn();
    }

    private ActionListener action(final int action) {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (!available()) {
                    status.setText("  请登录后操作");
                } else if (action == 0) {
                    search(true);
                } else {
                    mutate(action == 2);
                }
            }
        };
    }

    private void search(boolean resetPage) {
        if (resetPage) {
            pager.firstPage();
        }
        bookSearch.load(keyword.getText().trim(), SEARCH_FIELDS[field.getSelectedIndex()]);
    }

    private void mutate(final boolean reserve) {
        if (changing || (!reserve && !borrows.quota.canBorrow())
                || bookTable.getSelectedRow() < 0) {
            status.setText("  请先选择图书，并确认借阅额度已加载");
            return;
        }
        final String isbn = (String) bookModel.getValueAt(
                bookTable.convertRowIndexToModel(bookTable.getSelectedRow()), 0);
        changing = true;
        borrows.quota.changing(true);
        UiTasks.run(new UiTasks.Task<Object>() {
            @Override
            public Object run() {
                return reserve ? api.reserveBook(isbn) : api.borrowBook(isbn);
            }
        }, new UiTasks.Success<Object>() {
            @Override
            public void accept(Object ignored) {
                changed(reserve ? "预约申请已提交" : "借阅成功");
            }
        }, new UiTasks.Failure() {
            @Override
            public void accept(ApiException error) {
                changed(error.getMessage());
            }
        });
    }

    private void changed(String message) {
        changing = false;
        borrows.quota.changing(false);
        status.setText("  " + message);
        if (available()) {
            search(false);
            refreshReader();
        }
    }

    private void refreshReader() {
        if (api.borrowLimit() > 0) {
            borrows.refresh();
            reservations.refresh();
        }
    }
}
