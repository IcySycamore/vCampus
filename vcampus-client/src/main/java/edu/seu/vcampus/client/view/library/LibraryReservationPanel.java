package edu.seu.vcampus.client.view.library;

import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.client.library.LibraryService;
import edu.seu.vcampus.client.view.UiTasks;
import edu.seu.vcampus.client.view.theme.UiFactory;
import edu.seu.vcampus.common.library.entity.BookReservation;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/** 我的预约页，显示排队与到馆保留状态并支持取消。 */
final class LibraryReservationPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final DefaultTableModel model = LibraryTableModels.create(new String[] {
            "预约号", "书名", "ISBN", "申请日期", "保留截止", "状态"
    });
    private final JTable table = new JTable(model);
    private final LibraryService api;
    private final JLabel status;
    private final Runnable afterChange;
    private boolean changing;

    LibraryReservationPanel(LibraryService api, JLabel status, Runnable afterChange) {
        this.api = api;
        this.status = status;
        this.afterChange = afterChange;
        setLayout(new BorderLayout());
        table.setName("libraryReservationTable");
        JPanel actions = LibraryViewBuilder.toolbar();
        LibraryViewBuilder.addKeywordFilter(actions, table, "libraryReservationFilter");
        actions.add(button("刷新预约", "refresh", false));
        actions.add(button("取消所选", "return", true));
        add(LibraryViewBuilder.cardWithToolbar(table, actions), BorderLayout.CENTER);
    }

    void refresh() {
        if (api == null || !api.isLoggedIn() || changing) {
            return;
        }
        UiTasks.run(new UiTasks.Task<List<BookReservation>>() {
            @Override
            public List<BookReservation> run() {
                return api.listMyReservations();
            }
        }, new UiTasks.Success<List<BookReservation>>() {
            @Override
            public void accept(List<BookReservation> records) {
                LibraryTableModels.showReservations(model, records);
                status.setText("  预约记录已更新");
            }
        }, failure());
    }

    private JButton button(String text, String icon, final boolean cancel) {
        JButton button = cancel ? UiFactory.primaryButton(text, icon)
                : UiFactory.secondaryButton(text, icon);
        button.setName(cancel ? "libraryReservationCancel" : "libraryReservationRefresh");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (cancel) {
                    cancel();
                } else {
                    refresh();
                }
            }
        });
        return button;
    }

    private void cancel() {
        if (changing || table.getSelectedRow() < 0) {
            status.setText("  请先选择一条有效预约");
            return;
        }
        final long id = ((Number) table.getModel().getValueAt(
                table.convertRowIndexToModel(table.getSelectedRow()), 0)).longValue();
        changing = true;
        UiTasks.run(new UiTasks.Task<BookReservation>() {
            @Override
            public BookReservation run() {
                return api.cancelReservation(id);
            }
        }, new UiTasks.Success<BookReservation>() {
            @Override
            public void accept(BookReservation reservation) {
                changing = false;
                status.setText("  预约已取消");
                refresh();
                afterChange.run();
            }
        }, new UiTasks.Failure() {
            @Override
            public void accept(ApiException error) {
                changing = false;
                status.setText("  " + error.getMessage());
                refresh();
            }
        });
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
