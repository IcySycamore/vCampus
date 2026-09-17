package edu.seu.vcampus.client.view.library;

import edu.seu.vcampus.client.library.LibraryService;
import edu.seu.vcampus.client.view.component.RoundedPanel;
import edu.seu.vcampus.client.view.theme.UiIcons;
import edu.seu.vcampus.client.view.theme.UiTheme;
import edu.seu.vcampus.common.library.entity.BorrowRecord;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/** 图书馆首页，汇总读者状态并展示阅读活动与推荐。 */
final class LibraryHomePanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final Color CARD = new Color(255, 252, 246);
    private final LibraryService api;
    private final JLabel borrowed = valueLabel("—", "libraryHomeBorrowed");
    private final JLabel overdue = valueLabel("—", "libraryHomeOverdue");
    private final JLabel remaining = valueLabel("—", "libraryHomeRemaining");
    private final JLabel dueHint = new JLabel("正在读取你的借阅状态…");
    private final LibraryNewsCarousel news = new LibraryNewsCarousel();
    private final LibraryReadingCarousel reading = new LibraryReadingCarousel();
    private final LibraryPopularBorrowPanel popular;
    private final LibraryRulesNoticePanel rules = new LibraryRulesNoticePanel();

    LibraryHomePanel(LibraryService api) {
        this.api = api;
        popular = new LibraryPopularBorrowPanel(api);
        setName("libraryHome");
        setLayout(new BorderLayout(0, 16));
        setOpaque(false);
        add(statusArea(), BorderLayout.NORTH);
        add(showcase(), BorderLayout.CENTER);
        add(serviceHint(), BorderLayout.SOUTH);
        if (api == null || !api.isLoggedIn() || api.borrowLimit() <= 0) {
            showUnavailable();
        }
    }

    void refreshPopular() {
        popular.refresh();
    }

    void borrowLoading() {
        borrowed.setText("—");
        overdue.setText("—");
        remaining.setText("—");
        dueHint.setText("正在读取你的借阅状态…");
    }

    void borrowFailed() {
        borrowed.setText("—");
        overdue.setText("—");
        remaining.setText("—");
        dueHint.setText("借阅状态加载失败，请稍后刷新");
    }

    void showBorrows(List<BorrowRecord> records) {
        int active = 0;
        int late = 0;
        BorrowRecord nearest = null;
        Date now = new Date();
        for (BorrowRecord record : records) {
            if (!record.isReturned()) {
                active++;
                Date due = record.getDueAt();
                if (due != null && now.after(due)) {
                    late++;
                }
                if (due != null && (nearest == null
                        || due.before(nearest.getDueAt()))) {
                    nearest = record;
                }
            }
        }
        borrowed.setText(String.valueOf(active));
        overdue.setText(String.valueOf(late));
        remaining.setText(String.valueOf(Math.max(0, api.borrowLimit() - active)));
        showNearest(nearest);
    }

    private JPanel statusArea() {
        JPanel area = new JPanel(new BorderLayout(0, 10));
        area.setOpaque(false);
        JPanel metrics = new JPanel(new GridLayout(1, 3, 14, 0));
        metrics.setOpaque(false);
        metrics.add(metric("正在借阅", borrowed, "borrow", UiTheme.NAVY));
        metrics.add(metric("已经逾期", overdue, "return", UiTheme.ACCENT));
        metrics.add(metric("剩余可借", remaining, "library", UiTheme.SUCCESS));
        area.add(metrics, BorderLayout.CENTER);
        dueHint.setName("libraryHomeDueHint");
        dueHint.setOpaque(true);
        dueHint.setBackground(new Color(234, 241, 245));
        dueHint.setForeground(UiTheme.MUTED);
        dueHint.setBorder(BorderFactory.createEmptyBorder(9, 12, 9, 12));
        area.add(dueHint, BorderLayout.SOUTH);
        return area;
    }

    private JPanel showcase() {
        JPanel area = new JPanel(new GridBagLayout());
        area.setName("libraryHomeShowcase");
        area.setOpaque(false);

        // 保留首页两栏 65% / 35% 的宽度设置。
        news.setPreferredSize(new java.awt.Dimension(0, 0));
        reading.setPreferredSize(new java.awt.Dimension(0, 0));
        area.add(news, constraints(0, 0, 0.65D, 0.90D));
        area.add(reading, constraints(1, 0, 0.35D, 0.90D));
        area.add(popular, constraints(0, 1, 0.65D, 0.10D));
        area.add(rules, constraints(1, 1, 0.35D, 0.10D));
        return area;
    }

    private GridBagConstraints constraints(int column, int row,
            double horizontal, double vertical) {
        GridBagConstraints value = new GridBagConstraints();
        value.gridx = column;
        value.gridy = row;
        value.weightx = horizontal;
        value.weighty = vertical;
        value.fill = GridBagConstraints.BOTH;
        value.insets = new Insets(row == 0 ? 0 : 12,
                column == 0 ? 0 : 10, 0, 0);
        return value;
    }

    private JPanel metric(String caption, JLabel value, String icon, Color color) {
        RoundedPanel card = new RoundedPanel(new BorderLayout(10, 0), 18, CARD);
        card.setBorder(BorderFactory.createEmptyBorder(13, 16, 13, 14));
        JPanel text = new JPanel(new BorderLayout(0, 3));
        text.setOpaque(false);
        JLabel title = new JLabel(caption);
        title.setForeground(UiTheme.MUTED);
        text.add(title, BorderLayout.NORTH);
        text.add(value, BorderLayout.CENTER);
        card.add(text, BorderLayout.CENTER);
        JLabel badge = new JLabel(UiIcons.load(icon, 28), SwingConstants.CENTER);
        badge.setForeground(color);
        card.add(badge, BorderLayout.EAST);
        return card;
    }

    private JLabel serviceHint() {
        JLabel label = new JLabel("开放时间 08:00—22:00  ·  借期 30 天  ·  可续借 2 次");
        label.setForeground(UiTheme.MUTED);
        label.setFont(UiTheme.font(Font.PLAIN, 13F));
        return label;
    }

    private void showNearest(BorrowRecord nearest) {
        if (nearest == null) {
            dueHint.setText("当前没有未归还图书，去图书检索发现一本好书吧");
            return;
        }
        String date = new SimpleDateFormat("yyyy-MM-dd").format(nearest.getDueAt());
        dueHint.setText("最近应还：《" + nearest.getBookTitle() + "》 · " + date);
    }

    private void showUnavailable() {
        borrowed.setText("—");
        overdue.setText("—");
        remaining.setText("—");
        dueHint.setText(api == null ? "登录并连接服务器后查看借阅状态"
                : "当前身份不使用读者借阅额度");
    }

    private static JLabel valueLabel(String text, String name) {
        JLabel label = new JLabel(text);
        label.setName(name);
        label.setForeground(UiTheme.NAVY);
        label.setFont(UiTheme.font(Font.BOLD, 27F));
        return label;
    }
}
