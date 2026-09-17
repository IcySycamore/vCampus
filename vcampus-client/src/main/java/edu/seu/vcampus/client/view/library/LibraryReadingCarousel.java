package edu.seu.vcampus.client.view.library;

import edu.seu.vcampus.client.view.component.RoundedButton;
import edu.seu.vcampus.client.view.theme.UiTheme;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;

/** 首页“爱上阅读”单本封面推荐轮播。 */
final class LibraryReadingCarousel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final Recommendation[] BOOKS = {
        new Recommendation("Effective Java", "Joshua Bloch", "计算机", new Color(34, 78, 116)),
        new Recommendation("代码整洁之道", "Robert C. Martin", "软件工程", new Color(45, 108, 96)),
        new Recommendation("平凡的世界", "路遥", "文学", new Color(143, 79, 58))};
    private final BookCover cover = new BookCover();
    private final JLabel page = new JLabel();
    private final Timer timer;
    private int index;

    LibraryReadingCarousel() {
        setName("libraryHomeReading");
        setLayout(new BorderLayout(0, 8));
        setOpaque(false);
        add(header(), BorderLayout.NORTH);
        cover.setBorder(BorderFactory.createEmptyBorder(8, 13, 12, 13));
        add(cover, BorderLayout.CENTER);
        showBook();
        timer = new Timer(7000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                change(1);
            }
        });
        addHierarchyListener(new HierarchyListener() {
            @Override
            public void hierarchyChanged(HierarchyEvent event) {
                if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                    updateTimer();
                }
            }
        });
    }

    private JPanel header() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("爱上阅读");
        title.setForeground(UiTheme.TEXT);
        title.setFont(UiTheme.font(Font.BOLD, 17F));
        header.add(title, BorderLayout.WEST);
        JPanel controls = new JPanel(new BorderLayout(6, 0));
        controls.setOpaque(false);
        controls.add(page, BorderLayout.WEST);
        controls.add(button("‹", -1), BorderLayout.CENTER);
        controls.add(button("›", 1), BorderLayout.EAST);
        header.add(controls, BorderLayout.EAST);
        return header;
    }

    private JButton button(String text, final int direction) {
        JButton button = new RoundedButton(text, new Color(225, 241, 248),
                UiTheme.NAVY, UiTheme.NAVY_LIGHT, 14);
        button.setName(direction < 0 ? "libraryHomeReadingPrevious" : "libraryHomeReadingNext");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                change(direction);
            }
        });
        return button;
    }

    private void change(int direction) {
        index = (index + direction + BOOKS.length) % BOOKS.length;
        showBook();
    }

    private void showBook() {
        cover.show(BOOKS[index]);
        page.setText((index + 1) + "/" + BOOKS.length);
    }

    private void updateTimer() {
        if (isShowing()) {
            timer.start();
        } else {
            timer.stop();
        }
    }

    private static final class Recommendation {
        private final String title;
        private final String author;
        private final String category;
        private final Color color;

        Recommendation(String title, String author, String category, Color color) {
            this.title = title;
            this.author = author;
            this.category = category;
            this.color = color;
        }
    }

    private static final class BookCover extends JPanel {
        private Recommendation book;

        void show(Recommendation book) {
            this.book = book;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (book == null) {
                return;
            }
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            int width = getWidth();
            int height = getHeight();
            g2.setColor(book.color);
            g2.fillRoundRect(12, 4, width - 24, height - 10, 18, 18);
            g2.setColor(new Color(255, 255, 255, 52));
            g2.fillOval(width / 2 - 42, 32, 118, 118);
            g2.setColor(Color.WHITE);
            g2.setFont(UiTheme.font(Font.BOLD, 14F));
            g2.drawString(book.category, 31, 38);
            g2.setFont(UiTheme.font(Font.BOLD, 23F));
            drawTitle(g2, book.title, 31, height / 2 - 6);
            g2.setFont(UiTheme.font(Font.PLAIN, 14F));
            g2.drawString(book.author, 31, height - 34);
            g2.dispose();
        }

        private void drawTitle(Graphics2D graphics, String title, int x, int y) {
            String[] words = title.split(" ");
            if (words.length == 1) {
                graphics.drawString(title, x, y);
            } else {
                graphics.drawString(words[0], x, y - 27);
                graphics.drawString(title.substring(words[0].length() + 1), x, y + 4);
            }
        }
    }
}
