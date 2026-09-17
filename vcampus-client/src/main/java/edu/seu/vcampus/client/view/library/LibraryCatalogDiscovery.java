package edu.seu.vcampus.client.view.library;

import edu.seu.vcampus.client.view.component.RoundedPanel;
import edu.seu.vcampus.client.view.theme.UiTheme;
import edu.seu.vcampus.common.library.entity.Book;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** 查询前展示的热门检索词与热门借阅入口。 */
final class LibraryCatalogDiscovery extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final int MAX_ITEMS = 8;
    private final JPanel terms = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
    private final JPanel popular = new JPanel();
    private final ActionListener search;
    private final Map<String, Integer> searches = new LinkedHashMap<String, Integer>();
    private List<Book> latest = Collections.emptyList();

    LibraryCatalogDiscovery(ActionListener search) {
        this.search = search;
        setName("libraryCatalogDiscovery");
        setOpaque(false);
        setLayout(new BorderLayout(0, 22));
        add(introduction(), BorderLayout.NORTH);
        JPanel cards = new JPanel(new GridLayout(1, 2, 18, 0));
        cards.setOpaque(false);
        cards.add(card("热门检索词", "点击词条即可检索", terms));
        popular.setOpaque(false);
        popular.setLayout(new BoxLayout(popular, BoxLayout.Y_AXIS));
        cards.add(card("热门借阅", "按当前借出量与馆藏量推荐", popular));
        add(cards, BorderLayout.CENTER);
    }

    void showBooks(List<Book> books) {
        List<Book> ranked = rank(books);
        latest = ranked;
        showTerms(ranked);
        popular.removeAll();
        int size = Math.min(MAX_ITEMS, ranked.size());
        for (int index = 0; index < size; index++) {
            Book book = ranked.get(index);
            int borrowed = book.getTotalCopies() - book.getAvailableCopies();
            String text = (index + 1) + ". 《" + book.getTitle() + "》 · "
                    + book.getAuthor() + "    当前借出 " + borrowed + " 本";
            popular.add(link(text, book.getTitle(), "libraryPopularBook" + index));
        }
        if (ranked.isEmpty()) {
            popular.add(empty("暂无热门借阅数据"));
        }
        revalidate();
        repaint();
    }

    void recordSearch(String keyword) {
        String value = keyword == null ? "" : keyword.trim();
        if (value.length() > 0) {
            Integer count = searches.get(value);
            searches.put(value, count == null ? 1 : count + 1);
            showTerms(latest);
        }
    }

    private JPanel introduction() {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setOpaque(false);
        JLabel title = new JLabel("发现馆藏");
        title.setFont(UiTheme.font(Font.BOLD, 24F));
        title.setForeground(UiTheme.NAVY);
        JLabel hint = new JLabel("输入书名、作者或 ISBN；搜索后展示完整馆藏资料");
        hint.setForeground(UiTheme.MUTED);
        panel.add(title, BorderLayout.NORTH);
        panel.add(hint, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel card(String title, String hint, JPanel content) {
        RoundedPanel panel = new RoundedPanel(new BorderLayout(0, 12), 18,
                new java.awt.Color(255, 255, 255, 155));
        panel.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        JLabel label = new JLabel(title);
        label.setFont(UiTheme.font(Font.BOLD, 18F));
        label.setForeground(UiTheme.NAVY);
        JLabel note = new JLabel(hint);
        note.setForeground(UiTheme.MUTED);
        heading.add(label, BorderLayout.WEST);
        heading.add(note, BorderLayout.EAST);
        panel.add(heading, BorderLayout.NORTH);
        content.setOpaque(false);
        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    private void showTerms(List<Book> books) {
        terms.removeAll();
        Set<String> values = new LinkedHashSet<String>();
        values.addAll(searches.keySet());
        for (Book book : books) {
            add(values, book.getCategory());
            add(values, book.getTitle());
        }
        int index = 0;
        for (String value : values) {
            if (index >= MAX_ITEMS) {
                break;
            }
            terms.add(link(value, value, "libraryHotTerm" + index));
            index++;
        }
        if (values.isEmpty()) {
            terms.add(empty("暂无热门检索词"));
        }
    }

    private JButton link(String text, String value, String name) {
        JButton button = new JButton(text);
        button.setName(name);
        button.setActionCommand(value);
        button.setFont(UiTheme.font(Font.PLAIN, 14F));
        button.setForeground(UiTheme.NAVY_LIGHT);
        button.setContentAreaFilled(false);
        button.setBorder(BorderFactory.createEmptyBorder(7, 4, 7, 4));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setHorizontalAlignment(JButton.LEFT);
        button.addActionListener(search);
        return button;
    }

    private JLabel empty(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(UiTheme.MUTED);
        return label;
    }

    private List<Book> rank(List<Book> books) {
        List<Book> ranked = new ArrayList<Book>(books);
        Collections.sort(ranked, new Comparator<Book>() {
            @Override
            public int compare(Book left, Book right) {
                int leftBorrowed = left.getTotalCopies() - left.getAvailableCopies();
                int rightBorrowed = right.getTotalCopies() - right.getAvailableCopies();
                int difference = rightBorrowed - leftBorrowed;
                return difference != 0 ? difference
                        : right.getTotalCopies() - left.getTotalCopies();
            }
        });
        return ranked;
    }

    private void add(Set<String> values, String value) {
        if (value != null && value.trim().length() > 0) {
            values.add(value.trim());
        }
    }
}
