package edu.seu.vcampus.client.view.library;

import edu.seu.vcampus.client.view.component.RoundedOutlineBorder;
import edu.seu.vcampus.client.view.theme.UiTheme;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.net.URL;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;

/** 图书馆活动资讯图片轮播，支持自动播放和手动翻页。 */
final class LibraryNewsCarousel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final String[] IMAGES = {"/library/home/reading-festival.png",
        "/library/home/reading-talk.png"};
    private static final String[] CAPTIONS = {"秋季阅读市集：与好书不期而遇",
        "读书会现场：分享一本改变你的书"};
    private final JLabel photo = new JLabel("活动图片加载中", SwingConstants.CENTER);
    private final JLabel caption = new JLabel();
    private final JLabel page = new JLabel();
    private final Timer timer;
    private int index;

    LibraryNewsCarousel() {
        setName("libraryHomeNews");
        setLayout(new BorderLayout(0, 8));
        setOpaque(false);
        add(header(), BorderLayout.NORTH);
        photo.setOpaque(true);
        photo.setBackground(new Color(232, 239, 242));
        photo.setBorder(BorderFactory.createLineBorder(new Color(214, 224, 228)));
        add(photo, BorderLayout.CENTER);
        caption.setOpaque(true);
        caption.setBackground(UiTheme.NAVY);
        caption.setForeground(Color.WHITE);
        caption.setBorder(BorderFactory.createEmptyBorder(9, 11, 9, 11));
        add(caption, BorderLayout.SOUTH);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                showSlide();
            }
        });
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
        button.setForeground(UiTheme.NAVY);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                new RoundedOutlineBorder(UiTheme.NAVY_LIGHT, 14),
                BorderFactory.createEmptyBorder(3, 10, 3, 10)));
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
            photo.setIcon(null);
            photo.setText("活动图片不可用");
        } else {
            Image image = new ImageIcon(resource).getImage();
            int width = Math.max(1, photo.getWidth());
            int height = Math.max(1, photo.getHeight());
            photo.setIcon(new ImageIcon(image.getScaledInstance(width, height,
                    Image.SCALE_SMOOTH)));
            photo.setText("");
        }
        caption.setText(CAPTIONS[index]);
        page.setText((index + 1) + "/" + IMAGES.length);
    }

    private void updateTimer() {
        if (isShowing()) {
            timer.start();
        } else {
            timer.stop();
        }
    }
}
