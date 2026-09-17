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
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
/** 可手动切换展示首页馆藏。 */
final class LibraryCatalogCarousel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final Color CARD = new Color(255, 252, 246);
    private final LibraryService api;
    private final JPanel track = new JPanel();
    private final JScrollPane scroll;
    private final JLabel message = new JLabel("正在读取馆藏…");
    private List<Book> books;
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
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.getHorizontalScrollBar().setName("libraryHomeCatalogScrollBar");
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setPreferredSize(new Dimension(0, 260));
        add(scroll, BorderLayout.CENTER);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                if (books != null && !books.isEmpty()) {
                    renderBooks();
                }
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
    private JPanel heading() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        JLabel title = new JLabel("馆藏速览");
        title.setForeground(UiTheme.TEXT);
        title.setFont(UiTheme.font(Font.BOLD, 17F));
        JLabel tip = new JLabel("全屏时卡片会自动放大");
        tip.setForeground(UiTheme.MUTED);
        panel.add(title, BorderLayout.WEST);
        JPanel controls = new JPanel(new BorderLayout(8, 0));
        controls.setOpaque(false);
        controls.add(tip, BorderLayout.WEST);
        controls.add(button("‹", -1), BorderLayout.CENTER);
        controls.add(button("›", 1), BorderLayout.EAST);
        panel.add(controls, BorderLayout.EAST);
        return panel;
    }
    private JButton button(String text, final int direction) {
        JButton button = new JButton(text);
        button.setName(direction < 0 ? "libraryHomeCatalogPrevious" : "libraryHomeCatalogNext");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                move(direction * 237);
            }
        });
        return button;
    }
    private void showBooks(List<Book> books) {
        this.books = books;
        renderBooks();
    }
    private void renderBooks() {
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
        books = null;
        track.removeAll();
        message.setForeground(UiTheme.MUTED);
        message.setText(text);
        track.add(message);
        resetTrack();
    }
    private JPanel bookCard(Book book) {
        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 9), 18, CARD);
        Dimension size = cardSize();
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
        JLabel title = new JLabel(shorten(book.getTitle(), 23));
        title.setForeground(UiTheme.TEXT);
        title.setFont(UiTheme.font(Font.BOLD, 16F));
        JLabel author = new JLabel(shorten(book.getAuthor(), 26));
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
    private void move(int amount) {
        JScrollBar bar = scroll.getHorizontalScrollBar();
        int limit = Math.max(0, bar.getMaximum() - bar.getVisibleAmount());
        if (limit > 0) {
            int target = bar.getValue() + amount;
            bar.setValue(target < 0 ? limit : target > limit ? 0 : target);
        }
    }
    private Dimension cardSize() {
        int width = scroll.getViewport().getWidth();
        int height = scroll.getViewport().getHeight();
        int cardWidth = width <= 0 ? 260 : Math.max(230, Math.min(380, (width - 18) / 2));
        int cardHeight = height <= 0 ? 210 : Math.max(180, Math.min(310, height - 12));
        return new Dimension(cardWidth, cardHeight);
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
