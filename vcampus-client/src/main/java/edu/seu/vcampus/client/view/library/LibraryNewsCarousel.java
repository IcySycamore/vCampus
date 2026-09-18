package edu.seu.vcampus.client.view.library;

import edu.seu.vcampus.client.view.theme.UiTheme;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.net.URL;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;

/** 图书馆活动资讯图片轮播，支持自动播放和手动翻页。 */
final class LibraryNewsCarousel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final String[] IMAGES = { "/library/home/reading-festival.png",
            "/library/home/reading-talk.png" };
    private static final String[] CAPTIONS = { "秋季阅读市集：与好书不期而遇",
            "读书会现场：分享一本改变你的书" };
    private final NewsPhoto photo = new NewsPhoto();
    private final JLabel caption = new JLabel();
    private final JPanel media = new JPanel(new BorderLayout());
    private final JLabel page = new JLabel();
    private final Timer timer;
    private int index;

    LibraryNewsCarousel() {
        setName("libraryHomeNews");
        setLayout(new BorderLayout(0, 8));
        setOpaque(false);
        add(header(), BorderLayout.NORTH);
        configureMedia();
        add(media, BorderLayout.CENTER);
        showSlide();
        timer = new Timer(6000, new ActionListener() {
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

    private void configureMedia() {
        media.setName("libraryHomeNewsMedia");
        media.setOpaque(true);
        media.setBackground(new Color(238, 243, 246));
        media.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER));
        media.add(photo, BorderLayout.CENTER);
        caption.setOpaque(true);
        caption.setBackground(UiTheme.NAVY);
        caption.setForeground(Color.WHITE);
        caption.setFont(UiTheme.font(Font.BOLD, UiTheme.SIZE_SMALL));
        caption.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        media.add(caption, BorderLayout.SOUTH);
    }

    private JPanel header() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("新闻资讯");
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
        button.setName(direction < 0 ? "libraryHomeNewsPrevious" : "libraryHomeNewsNext");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                change(direction);
            }
        });
        return button;
    }

    private void change(int direction) {
        index = (index + direction + IMAGES.length) % IMAGES.length;
        showSlide();
    }

    private void showSlide() {
        URL resource = getClass().getResource(IMAGES[index]);
        if (resource == null) {
            photo.show(null);
            caption.setText("活动图片不可用");
        } else {
            photo.show(new ImageIcon(resource).getImage());
            caption.setText(CAPTIONS[index]);
        }
        caption.setToolTipText(caption.getText());
        page.setText((index + 1) + "/" + IMAGES.length);
    }

    private void updateTimer() {
        if (isShowing()) {
            timer.start();
        } else {
            timer.stop();
        }
    }

    /**
     * 计算原图完整内接视口后的尺寸；只缩放，不改变比例，也不裁切。
     *
     * @param sourceWidth 原图宽度
     * @param sourceHeight 原图高度
     * @param frameWidth 视口宽度
     * @param frameHeight 视口高度
     * @return 绘制尺寸；参数无效时为 0x0
     */
    static Dimension fitInside(int sourceWidth, int sourceHeight,
            int frameWidth, int frameHeight) {
        if (sourceWidth <= 0 || sourceHeight <= 0 || frameWidth <= 0 || frameHeight <= 0) {
            return new Dimension(0, 0);
        }
        double scale = Math.min((double) frameWidth / sourceWidth,
                (double) frameHeight / sourceHeight);
        return new Dimension(Math.max(1, (int) Math.round(sourceWidth * scale)),
                Math.max(1, (int) Math.round(sourceHeight * scale)));
    }

    /** 横版活动图视口：完整等比内接，不拉伸、不裁切。 */
    private static final class NewsPhoto extends JPanel {
        private static final long serialVersionUID = 1L;
        private Image image;

        NewsPhoto() {
            setName("libraryHomeNewsImage");
            setOpaque(true);
            setBackground(new Color(238, 243, 246));
        }

        void show(Image image) {
            this.image = image;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            Insets insets = getInsets();
            int x = insets.left;
            int y = insets.top;
            int width = getWidth() - insets.left - insets.right;
            int height = getHeight() - insets.top - insets.bottom;
            if (width <= 0 || height <= 0) {
                g2.dispose();
                return;
            }
            g2.clipRect(x, y, width, height);
            drawImage(g2, x, y, width, height);
            g2.dispose();
        }

        private void drawImage(Graphics2D graphics, int x, int y, int width, int height) {
            if (image == null || image.getWidth(null) <= 0 || image.getHeight(null) <= 0) {
                graphics.setColor(UiTheme.MUTED);
                graphics.setFont(UiTheme.font(Font.PLAIN, UiTheme.SIZE_SMALL));
                FontMetrics metrics = graphics.getFontMetrics();
                String message = "活动图片加载中";
                graphics.drawString(message, x + Math.max(0,
                        (width - metrics.stringWidth(message)) / 2),
                        y + Math.max(metrics.getAscent(), (height + metrics.getAscent()) / 2));
                return;
            }
            Dimension fitted = fitInside(image.getWidth(null), image.getHeight(null),
                    width, height);
            int drawWidth = fitted.width;
            int drawHeight = fitted.height;
            int drawX = x + (width - drawWidth) / 2;
            int drawY = y + (height - drawHeight) / 2;
            graphics.drawImage(image, drawX, drawY, drawWidth, drawHeight, null);
        }
    }
}
