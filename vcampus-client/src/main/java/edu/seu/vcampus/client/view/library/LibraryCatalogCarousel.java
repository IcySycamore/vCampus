package edu.seu.vcampus.client.view.library;

import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.client.library.LibraryService;
import edu.seu.vcampus.client.view.UiTasks;
import edu.seu.vcampus.client.view.component.RoundedPanel;
import edu.seu.vcampus.client.view.theme.UiTheme;
import edu.seu.vcampus.common.library.dto.BookQuery;
import edu.seu.vcampus.common.library.entity.Book;
import edu.seu.vcampus.common.message.PageResponse;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.Timer;

/** 自动滚动展示首页馆藏。 */
final class LibraryCatalogCarousel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final Color CARD = new Color(255, 252, 246);
    private final LibraryService api;
    private final JPanel track = new JPanel();
    private final JScrollPane scroll;
    private final JLabel message = new JLabel("正在读取馆藏…");
    private final Timer timer;
    private int generation;

    LibraryCatalogCarousel(LibraryService api) {
        this.api = api;
        setName("libraryHomeCatalog");
        setLayout(new BorderLayout(0, 10));
        setOpaque(false);
        add(heading(), BorderLayout.NORTH);
        track.setName("libraryHomeCatalogTrack");
        track.setLayout(new BoxLayout(track, BoxLayout.X_AXIS));
        track.setOpaque(false);
        scroll = new JScrollPane(track, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setPreferredSize(new Dimension(0, 150));
        add(scroll, BorderLayout.CENTER);
        timer = new Timer(35, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                advance();
            }
        });
        showMessage(api == null ? "登录并连接服务器后浏览馆藏" : "正在读取馆藏…");
    }

    void refresh() {
        if (api == null || !api.isLoggedIn()) {
            showMessage("登录并连接服务器后浏览馆藏");
            return;
        }
        final int current = ++generation;
        showMessage("正在读取馆藏…");
        UiTasks.run(new UiTasks.Task<PageResponse<Book>>() {
            @Override
            public PageResponse<Book> run() {
                return api.searchBooks(new BookQuery("", "all", 1, 20));
            }
        }, new UiTasks.Success<PageResponse<Book>>() {
            @Override
            public void accept(PageResponse<Book> page) {
                if (current == generation && api.isLoggedIn()) {
                    showBooks(page.getItems());
                }
            }
        }, new UiTasks.Failure() {
            @Override
            public void accept(ApiException error) {
                if (current == generation) {
                    showMessage("馆藏速览加载失败，请稍后刷新");
                }
            }
        });
    }

    @Override
    public void addNotify() {
        super.addNotify();
        timer.start();
    }

    @Override
    public void removeNotify() {
        timer.stop();
        super.removeNotify();
    }

    private JPanel heading() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        JLabel title = new JLabel("馆藏速览");
        title.setForeground(UiTheme.TEXT);
        title.setFont(UiTheme.font(Font.BOLD, 17F));
        JLabel tip = new JLabel("精选馆藏自动滚动展示");
        tip.setForeground(UiTheme.MUTED);
        panel.add(title, BorderLayout.WEST);
        panel.add(tip, BorderLayout.EAST);
        return panel;
    }

    private void showBooks(List<Book> books) {
        track.removeAll();
        if (books.isEmpty()) {
            track.add(message);
            message.setText("暂无可展示馆藏");
        } else {
            for (Book book : books) {
                track.add(bookCard(book));
                track.add(Box.createHorizontalStrut(12));
            }
        }
        resetTrack();
    }

    private void showMessage(String text) {
        track.removeAll();
        message.setForeground(UiTheme.MUTED);
        message.setText(text);
        track.add(message);
        resetTrack();
    }

    private JPanel bookCard(Book book) {
        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 9), 18, CARD);
        Dimension size = new Dimension(225, 132);
        card.setPreferredSize(size);
        card.setMinimumSize(size);
        card.setMaximumSize(size);
        card.setBorder(BorderFactory.createEmptyBorder(13, 15, 13, 15));
        JLabel category = new JLabel(shorten(book.getCategory(), 16));
        category.setForeground(UiTheme.ACCENT);
        category.setFont(UiTheme.font(Font.BOLD, 12F));
        card.add(category, BorderLayout.NORTH);
        JPanel detail = new JPanel(new GridLayout(2, 1, 0, 5));
        detail.setOpaque(false);
        JLabel title = new JLabel(shorten(book.getTitle(), 18));
        title.setForeground(UiTheme.TEXT);
        title.setFont(UiTheme.font(Font.BOLD, 16F));
        JLabel author = new JLabel(shorten(book.getAuthor(), 20));
        author.setForeground(UiTheme.MUTED);
        detail.add(title);
        detail.add(author);
        card.add(detail, BorderLayout.CENTER);
        JLabel stock = new JLabel(book.getAvailableCopies() > 0
                ? "可借 " + book.getAvailableCopies() + " 本" : "暂不可借");
        stock.setForeground(book.getAvailableCopies() > 0
                ? UiTheme.SUCCESS : UiTheme.ACCENT);
        card.add(stock, BorderLayout.SOUTH);
        return card;
    }

    private void advance() {
        JScrollBar bar = scroll.getHorizontalScrollBar();
        int limit = bar.getMaximum() - bar.getVisibleAmount();
        if (limit > 0) {
            bar.setValue(bar.getValue() >= limit ? 0 : bar.getValue() + 1);
        }
    }

    private void resetTrack() {
        scroll.getHorizontalScrollBar().setValue(0);
        track.revalidate();
        track.repaint();
    }

    private String shorten(String value, int length) {
        String text = value == null ? "—" : value;
        return text.length() <= length ? text : text.substring(0, length - 1) + "…";
    }
}
