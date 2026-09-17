package edu.seu.vcampus.client.view.library;

import edu.seu.vcampus.client.view.theme.UiTheme;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
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

    /** 书封宽高比（宽/高），绘制时按它居中内接，不随容器变形。 */
    private static final double COVER_RATIO = 0.68D;
    private static final Recommendation[] BOOKS = {
            new Recommendation("Effective Java", "Joshua Bloch", "计算机", new Color(34, 78, 116)),
            new Recommendation("代码整洁之道", "Robert C. Martin", "软件工程", new Color(45, 108, 96)),
            new Recommendation("平凡的世界", "路遥", "文学", new Color(143, 79, 58)) };
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
        JButton button = new JButton(text);
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
            int availWidth = getWidth() - 26;
            int availHeight = getHeight() - 22;
            if (availWidth <= 0 || availHeight <= 0) {
                return;
            }
            // 书封按固定宽高比居中绘制：容器任意拉伸都不变形（原来直接吃满 getWidth/getHeight，
            // 固定像素的圆形与文字偏移会跟着跑偏）。
            int width = availWidth;
            int height = (int) Math.round(width / COVER_RATIO);
            if (height > availHeight) {
                height = availHeight;
                width = (int) Math.round(height * COVER_RATIO);
            }
            int x = (getWidth() - width) / 2;
            int y = (getHeight() - height) / 2;
            float scale = Math.min(1F, width / 240F);
            int padding = Math.max(10, Math.round(19 * scale));

            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(book.color);
            g2.fillRoundRect(x, y, width, height, 18, 18);
            // 文字只许画在封面内：封面窄的时候原来会溢出到卡片外面（书名右边被裁、作者压边）
            g2.clipRect(x, y, width, height);
            int circle = Math.max(40, Math.round(height * 0.34F));
            g2.setColor(new Color(255, 255, 255, 52));
            g2.fillOval(x + (width - circle) / 2, y + Math.round(height * 0.16F), circle, circle);
            g2.setColor(Color.WHITE);
            int textWidth = Math.max(40, width - padding * 2);
            String[] titleLines = titleLines(book.title);
            g2.setFont(UiTheme.font(Font.BOLD, fitFontSize(g2, new String[] { book.category },
                    textWidth, Math.max(11, Math.round(14 * scale)))));
            g2.drawString(book.category, x + padding, y + Math.round(34 * scale));
            int titleSize = fitFontSize(g2, titleLines, textWidth,
                    Math.max(15, Math.round(23 * scale)));
            g2.setFont(UiTheme.font(Font.BOLD, titleSize));
            drawTitleLines(g2, titleLines, x + padding, y + height / 2 - 6,
                    (int) Math.round(titleSize * 1.25D));
            g2.setFont(UiTheme.font(Font.PLAIN, fitFontSize(g2, new String[] { book.author },
                    textWidth, Math.max(11, Math.round(14 * scale)))));
            g2.drawString(book.author, x + padding, y + height - Math.round(24 * scale));
            g2.dispose();
        }

        /** @param title 书名 @return 按空格折行后的各行 */
        private static String[] titleLines(String title) {
            String[] words = title.split(" ");
            if (words.length == 1) {
                return new String[] { title };
            }
            return new String[] { words[0], title.substring(words[0].length() + 1) };
        }

        /**
         * 把字号收到能放进可用宽度为止。
         *
         * @param graphics  画布
         * @param lines     待画各行
         * @param maxWidth  可用宽度
         * @param startSize 起始字号
         * @return 放得下且尽量大的字号
         */
        private static int fitFontSize(Graphics2D graphics, String[] lines, int maxWidth,
                int startSize) {
            for (int size = startSize; size > 9; size--) {
                graphics.setFont(UiTheme.font(Font.BOLD, size));
                FontMetrics metrics = graphics.getFontMetrics();
                int widest = 0;
                for (String line : lines) {
                    widest = Math.max(widest, metrics.stringWidth(line));
                }
                if (widest <= maxWidth) {
                    return size;
                }
            }
            return 9;
        }

        private void drawTitleLines(Graphics2D graphics, String[] lines, int x, int y,
                int lineHeight) {
            if (lines.length == 1) {
                graphics.drawString(lines[0], x, y);
            } else {
                graphics.drawString(lines[0], x, y - lineHeight);
                graphics.drawString(lines[1], x, y + 4);
            }
        }
    }
}
