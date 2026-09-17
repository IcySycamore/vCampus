package edu.seu.vcampus.client.view.library;

import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.client.library.LibraryService;
import edu.seu.vcampus.client.view.UiTasks;
import edu.seu.vcampus.client.view.component.RoundedPanel;
import edu.seu.vcampus.client.view.theme.UiTheme;
import edu.seu.vcampus.common.library.entity.PopularBorrow;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/** 首页热门借阅排行，数据来自服务器累计借阅记录。 */
final class LibraryPopularBorrowPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final Color CARD = new Color(255, 252, 246);
    private final LibraryService api;
    private final JPanel list = new JPanel(new GridLayout(0, 1, 0, 7));
    private int generation;

    LibraryPopularBorrowPanel(LibraryService api) {
        this.api = api;
        setName("libraryHomePopular");
        setLayout(new BorderLayout(0, 9));
        setOpaque(false);
        JLabel title = new JLabel("热门借阅");
        title.setFont(UiTheme.font(Font.BOLD, 17F));
        title.setForeground(UiTheme.TEXT);
        add(title, BorderLayout.NORTH);
        list.setOpaque(false);
        add(list, BorderLayout.CENTER);
        showMessage(api == null ? "连接服务器后查看热门借阅" : "正在读取借阅排行…");
    }

    void refresh() {
        if (api == null || !api.isLoggedIn()) {
            showMessage("连接服务器后查看热门借阅");
            return;
        }
        final int current = ++generation;
        showMessage("正在读取借阅排行…");
        UiTasks.run(new UiTasks.Task<List<PopularBorrow>>() {
            @Override
            public List<PopularBorrow> run() {
                return api.listPopularBorrows();
            }
        }, new UiTasks.Success<List<PopularBorrow>>() {
            @Override
            public void accept(List<PopularBorrow> items) {
                if (current == generation) {
                    showItems(items);
                }
            }
        }, new UiTasks.Failure() {
            @Override
            public void accept(ApiException error) {
                if (current == generation) {
                    showMessage("热门借阅加载失败，请稍后刷新");
                }
            }
        });
    }

    private void showItems(List<PopularBorrow> items) {
        list.removeAll();
        if (items.isEmpty()) {
            showMessage("暂无借阅排行，完成借阅后将自动统计");
            return;
        }
        for (int index = 0; index < items.size(); index++) {
            list.add(row(index + 1, items.get(index)));
        }
        refreshList();
    }

    private JPanel row(int rank, PopularBorrow item) {
        RoundedPanel row = new RoundedPanel(new BorderLayout(10, 0), 14, CARD);
        row.setName("libraryPopularRow" + rank);
        row.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 12));
        JLabel number = new JLabel(String.valueOf(rank), SwingConstants.CENTER);
        number.setForeground(rank <= 3 ? UiTheme.ACCENT : UiTheme.MUTED);
        number.setFont(UiTheme.font(Font.BOLD, 15F));
        row.add(number, BorderLayout.WEST);
        JLabel title = new JLabel(item.getTitle());
        title.setForeground(UiTheme.TEXT);
        title.setFont(UiTheme.font(Font.BOLD, 14F));
        row.add(title, BorderLayout.CENTER);
        JLabel count = new JLabel("借阅 " + item.getBorrowCount() + " 次");
        count.setForeground(UiTheme.SUCCESS);
        row.add(count, BorderLayout.EAST);
        return row;
    }

    private void showMessage(String text) {
        list.removeAll();
        JLabel label = new JLabel(text);
        label.setName("libraryHomePopularMessage");
        label.setForeground(UiTheme.MUTED);
        list.add(label);
        refreshList();
    }

    private void refreshList() {
        list.revalidate();
        list.repaint();
    }
}
