package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.client.view.component.RoundedPanel;
import edu.seu.vcampus.client.view.theme.UiIcons;
import edu.seu.vcampus.client.view.theme.UiTheme;

import java.awt.BorderLayout;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * 页面占位卡：模块未装配或尚未实现时显示的统一形态。
 *
 * <p>
 * 从 {@code MainContentPanel} 抽出（原文件逼近 200 行上限），同时让「占位」这件事只有一个实现，
 * 不会每个页面各写一份。
 */
public final class PlaceholderPage {

    /**
     * 私有构造器，禁止实例化工具类。
     */
    private PlaceholderPage() {
    }

    /**
     * 创建占位页。
     *
     * @param title       页面标题
     * @param description 页面描述
     * @param icon        图标名
     * @return 占位页
     */
    public static JPanel create(String title, String description, String icon) {
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(UiTheme.BACKGROUND);
        page.setBorder(BorderFactory.createEmptyBorder(34, 36, 34, 36));
        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 14), 24, UiTheme.SURFACE);
        card.setBorder(BorderFactory.createEmptyBorder(80, 40, 80, 40));
        JLabel iconLabel = new JLabel(UiIcons.load(icon, 72), SwingConstants.CENTER);
        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setForeground(UiTheme.TEXT);
        titleLabel.setFont(UiTheme.font(Font.BOLD, 28F));
        JLabel descriptionLabel = new JLabel(description, SwingConstants.CENTER);
        descriptionLabel.setForeground(UiTheme.MUTED);
        card.add(iconLabel, BorderLayout.NORTH);
        card.add(titleLabel, BorderLayout.CENTER);
        card.add(descriptionLabel, BorderLayout.SOUTH);
        page.add(card, BorderLayout.CENTER);
        return page;
    }
}
