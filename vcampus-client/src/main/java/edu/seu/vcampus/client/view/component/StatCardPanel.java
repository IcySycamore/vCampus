package edu.seu.vcampus.client.view.component;

import edu.seu.vcampus.client.view.theme.UiIcons;
import edu.seu.vcampus.client.view.theme.UiTheme;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * 工作台首页的统一统计卡片。
 */
public class StatCardPanel extends RoundedPanel {

    private static final long serialVersionUID = 1L;

    /**
     * 创建统计卡片。
     *
     * @param label 指标名称
     * @param value 指标值
     * @param icon 图标名称
     * @param accent 强调色
     */
    public StatCardPanel(String label, String value, String icon, Color accent) {
        super(new BorderLayout(10, 0), 16, UiTheme.SURFACE);
        setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 14));
        JPanel text = new JPanel(new GridLayout(2, 1, 0, 5));
        text.setOpaque(false);
        JLabel caption = new JLabel(label);
        caption.setForeground(UiTheme.MUTED);
        JLabel number = new JLabel(value);
        number.setForeground(accent);
        number.setFont(UiTheme.font(Font.BOLD, 31F));
        text.add(caption);
        text.add(number);
        add(text, BorderLayout.CENTER);
        RoundedPanel badge = new RoundedPanel(new BorderLayout(), 44, accent);
        badge.setPreferredSize(new Dimension(44, 44));
        badge.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        badge.add(new JLabel(UiIcons.load(icon + "-light", 22)));
        JPanel badgeArea = new JPanel(new GridBagLayout());
        badgeArea.setOpaque(false);
        badgeArea.add(badge);
        add(badgeArea, BorderLayout.EAST);
    }
}
