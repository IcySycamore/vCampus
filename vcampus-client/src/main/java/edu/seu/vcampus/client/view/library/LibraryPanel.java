package edu.seu.vcampus.client.view.library;

import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.client.library.LibraryService;
import edu.seu.vcampus.client.view.UiTasks;
import edu.seu.vcampus.client.view.theme.UiTheme;
import edu.seu.vcampus.common.library.entity.Book;
import edu.seu.vcampus.common.library.entity.BorrowRecord;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTabbedPane;
import javax.swing.table.DefaultTableModel;

/** 图书馆页面：只调用模块 API，后台调度与错误回填由 UiTasks 负责。 */
public class LibraryPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final String[] SEARCH_FIELDS = {"all", "title", "author", "category"};
    private final JTextField keyword = new JTextField(22);
    private final JComboBox<String> field = new JComboBox<String>(
            new String[] {"全部字段", "书名", "作者", "分类"});
    private final DefaultTableModel bookModel = LibraryTableModels.create(
            new String[] {"ISBN", "书名", "作者", "分类", "可借数量"});
    private final DefaultTableModel borrowModel = LibraryTableModels.create(
            new String[] {"记录号", "书名", "借阅日期", "应还日期", "状态"});
    private final JTable bookTable = new JTable(bookModel);
    private final JTable borrowTable = new JTable(borrowModel);
    private final JLabel status = new JLabel("  当前为离线预览");
    private final LibraryService api;
    private final LibraryQuotaControls quota;
    private final LibraryCatalogPanel catalog;
    private int queryGeneration;
    private int searchGeneration;
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
        quota = new LibraryQuotaControls(api);
        setLayout(new BorderLayout(0, 18));
        setBackground(UiTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(30, 34, 26, 34));
        add(LibraryViewBuilder.createHeading(), BorderLayout.NORTH);
        LibraryViewBuilder builder = new LibraryViewBuilder(keyword, field,
                bookTable, borrowTable, quota.borrowButton);
        JTabbedPane tabs = builder.createTabs(action(0), action(1), action(2), action(3));
        catalog = api != null && api.canManageCatalog()
                ? new LibraryCatalogPanel(api, new Runnable() {
                    @Override
                    public void run() {
                        search();
                    }
                }) : null;
        if (catalog != null) {
            tabs.addTab("馆藏管理", catalog);
        }
        add(tabs, BorderLayout.CENTER);
        add(LibraryViewBuilder.createFooter(status, quota.label), BorderLayout.SOUTH);
    }

    /** 进入页面时刷新馆藏与本人的借阅记录。 */
    public void refresh() {
        if (available()) {
            search();
            loadBorrows();
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
                    search();
                } else if (action == 2) {
                    loadBorrows();
                } else {
                    change(action == 1);
                }
            }
        };
    }

    private void search() {
        final String text = keyword.getText().trim();
        final String scope = SEARCH_FIELDS[field.getSelectedIndex()];
        final int generation = ++searchGeneration;
        UiTasks.run(new UiTasks.Task<List<Book>>() {
            @Override
            public List<Book> run() {
                return api.searchBooks(text, scope);
            }
        }, new UiTasks.Success<List<Book>>() {
            @Override
            public void accept(List<Book> books) {
                if (generation == searchGeneration && available()) {
                    LibraryTableModels.showBooks(bookModel, books);
                }
            }
        }, failure());
    }

    private void loadBorrows() {
        final int generation = ++queryGeneration;
        quota.loading();
        UiTasks.run(new UiTasks.Task<List<BorrowRecord>>() {
            @Override
            public List<BorrowRecord> run() {
                return api.listMyBorrows();
            }
        }, new UiTasks.Success<List<BorrowRecord>>() {
            @Override
            public void accept(List<BorrowRecord> records) {
                if (generation == queryGeneration && !changing && available()) {
                    LibraryTableModels.showBorrows(borrowModel, records);
                    quota.show(records);
                    status.setText("  借阅记录已更新");
                }
            }
        }, failure());
    }

    private void change(final boolean borrow) {
        JTable table = borrow ? bookTable : borrowTable;
        if (changing || (borrow && !quota.canBorrow()) || table.getSelectedRow() < 0) {
            status.setText("  请先选择记录，并确认借阅额度已加载");
            return;
        }
        final Object key = table.getModel().getValueAt(
                table.convertRowIndexToModel(table.getSelectedRow()), 0);
        changing = true;
        ++queryGeneration;
        quota.changing(true);
        UiTasks.run(new UiTasks.Task<BorrowRecord>() {
            @Override
            public BorrowRecord run() {
                return borrow ? api.borrowBook((String) key)
                        : api.returnBook(((Number) key).longValue());
            }
        }, new UiTasks.Success<BorrowRecord>() {
            @Override
            public void accept(BorrowRecord record) {
                changed();
                search();
            }
        }, new UiTasks.Failure() {
            @Override
            public void accept(ApiException error) {
                changed();
                status.setText("  " + error.getMessage());
            }
        });
    }

    private void changed() {
        changing = false;
        quota.changing(false);
        if (available()) {
            loadBorrows();
        }
    }

    private UiTasks.Failure failure() {
        return new UiTasks.Failure() {
            @Override
            public void accept(ApiException error) {
                status.setText("  " + error.getMessage());
            }
        };
    }
}
