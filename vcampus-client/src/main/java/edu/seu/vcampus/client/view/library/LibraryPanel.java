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
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

/** 图书馆页面：提供检索、借阅、续借、预约、归还和滞纳金缴纳。 */
public class LibraryPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final JLabel status = new JLabel("  当前为离线预览");
    private final JButton reserveButton = UiFactory.secondaryButton("预约所选", "borrow");
    private final LibraryService api;
    private final LibraryHomePanel home;
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
        home = new LibraryHomePanel(api);
        Runnable afterChange = new Runnable() {
            @Override
            public void run() {
                if (catalog != null) {
                    catalog.refresh();
                }
                home.refreshCatalog();
            }
        };
        borrows = new LibraryBorrowPanel(api, status, afterChange, home);
        reservations = new LibraryReservationPanel(api, status, afterChange);
        setLayout(new BorderLayout(0, 18));
        setBackground(UiTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(30, 34, 26, 34));
        add(LibraryViewBuilder.createHeading(), BorderLayout.NORTH);
        catalog = new LibraryCatalogPanel(api, afterChange, borrows.quota.borrowButton,
                reserveButton, action(false), action(true));
        LibraryViewBuilder builder = new LibraryViewBuilder();
        JTabbedPane tabs = builder.createTabs(home, catalog);
        if (api != null && api.borrowLimit() > 0) {
            tabs.addTab("我的借阅", UiIcons.load("borrow", 18), borrows);
            tabs.addTab("我的预约", UiIcons.load("borrow", 18), reservations);
        }
        add(tabs, BorderLayout.CENTER);
        add(LibraryViewBuilder.createFooter(status, borrows.quota.label), BorderLayout.SOUTH);
    }

    /** 进入页面时刷新馆藏、借阅和预约记录。 */
    public void refresh() {
        if (available()) {
            home.refreshCatalog();
            catalog.refresh();
            refreshReader();
        }
    }

    private boolean available() {
        return api != null && api.isLoggedIn();
    }

    private ActionListener action(final boolean reserve) {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (!available()) {
                    status.setText("  请登录后操作");
                } else {
                    mutate(reserve);
                }
            }
        };
    }

    private void mutate(final boolean reserve) {
        final String isbn = catalog.selectedIsbn();
        if (changing || (!reserve && !borrows.quota.canBorrow()) || isbn == null) {
            status.setText("  请先选择图书，并确认借阅额度已加载");
            return;
        }
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
            catalog.refresh();
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
