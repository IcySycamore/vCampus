package edu.seu.vcampus.client.view.library;

import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.client.library.LibraryService;
import edu.seu.vcampus.client.view.UiTasks;
import edu.seu.vcampus.client.view.theme.UiFactory;
import edu.seu.vcampus.common.library.entity.BorrowRecord;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/** 我的借阅页，提供刷新、归还、续借和缴纳滞纳金操作。 */
final class LibraryBorrowPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final DefaultTableModel model = LibraryTableModels.create(new String[] {
            "记录号", "书名", "借阅日期", "应还日期", "续借次数", "滞纳金", "状态"
    });
    private final JTable table = new JTable(model);
    private final LibraryService api;
    private final javax.swing.JLabel status;
    private final Runnable afterChange;
    final LibraryQuotaControls quota;
    private int generation;
    private boolean changing;

    LibraryBorrowPanel(LibraryService api, javax.swing.JLabel status, Runnable afterChange) {
        this.api = api;
        this.status = status;
        this.afterChange = afterChange;
        quota = new LibraryQuotaControls(api);
        setLayout(new BorderLayout());
        JPanel actions = LibraryViewBuilder.toolbar();
        actions.add(button("刷新记录", "refresh", 0));
        actions.add(button("归还所选", "return", 1));
        actions.add(button("续借所选", "refresh", 2));
        actions.add(button("缴纳滞纳金", "bank", 3));
        add(LibraryViewBuilder.cardWithToolbar(table, actions), BorderLayout.CENTER);
    }

    void refresh() {
        if (api == null || !api.isLoggedIn()) {
            return;
        }
        final int current = ++generation;
        quota.loading();
        UiTasks.run(new UiTasks.Task<List<BorrowRecord>>() {
            @Override
            public List<BorrowRecord> run() {
                return api.listMyBorrows();
            }
        }, new UiTasks.Success<List<BorrowRecord>>() {
            @Override
            public void accept(List<BorrowRecord> records) {
                if (current == generation && !changing && api.isLoggedIn()) {
                    LibraryTableModels.showBorrows(model, records);
                    quota.show(records);
                    status.setText("  借阅记录已更新");
                }
            }
        }, failure());
    }

    private JButton button(String text, String icon, final int action) {
        JButton button = action == 1 ? UiFactory.primaryButton(text, icon)
                : UiFactory.secondaryButton(text, icon);
        String[] names = {"libraryBorrowRefresh", "libraryReturn",
            "libraryRenew", "libraryFinePay"};
        button.setName(names[action]);
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (action == 0) {
                    refresh();
                } else {
                    mutate(action);
                }
            }
        });
        return button;
    }

    private void mutate(final int action) {
        if (changing || table.getSelectedRow() < 0) {
            status.setText("  请先选择一条借阅记录");
            return;
        }
        final long id = ((Number) table.getModel().getValueAt(
                table.convertRowIndexToModel(table.getSelectedRow()), 0)).longValue();
        changing = true;
        ++generation;
        quota.changing(true);
        UiTasks.run(new UiTasks.Task<BorrowRecord>() {
            @Override
            public BorrowRecord run() {
                if (action == 1) {
                    return api.returnBook(id);
                }
                if (action == 2) {
                    return api.renewBook(id);
                }
                return api.payFine(id);
            }
        }, new UiTasks.Success<BorrowRecord>() {
            @Override
            public void accept(BorrowRecord record) {
                complete();
            }
        }, new UiTasks.Failure() {
            @Override
            public void accept(ApiException error) {
                changing = false;
                quota.changing(false);
                status.setText("  " + error.getMessage());
                refresh();
            }
        });
    }

    private void complete() {
        changing = false;
        quota.changing(false);
        status.setText("  操作成功");
        refresh();
        afterChange.run();
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
