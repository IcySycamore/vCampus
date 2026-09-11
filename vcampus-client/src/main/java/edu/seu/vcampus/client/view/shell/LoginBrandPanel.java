package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.client.view.component.GradientPanel;
import edu.seu.vcampus.client.view.component.RoundedPanel;
import edu.seu.vcampus.client.view.theme.UiIcons;
import edu.seu.vcampus.client.view.theme.UiTheme;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * 登录页左侧深色品牌展示区。
 */
public class LoginBrandPanel extends GradientPanel {

    private static final long serialVersionUID = 1L;

    /** 创建品牌展示区。 */
    public LoginBrandPanel() {
        super(new Color(16, 36, 64), new Color(22, 58, 94));
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(38, 34, 38, 34));
        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);
        GridBagConstraints grid = new GridBagConstraints();
        grid.gridx = 0;
        grid.insets = new Insets(8, 10, 8, 10);
        RoundedPanel badge = new RoundedPanel(new BorderLayout(), 24, UiTheme.ACCENT);
        badge.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        badge.add(new JLabel(UiIcons.load("student-light", 40)));
        content.add(badge, grid);
        grid.gridy = 1;
        grid.insets = new Insets(28, 10, 8, 10);
        JLabel title = new JLabel("vCampus 虚拟校园");
        title.setForeground(Color.WHITE);
        title.setFont(UiTheme.font(Font.BOLD, 27F));
        content.add(title, grid);
        grid.gridy = 2;
        grid.insets = new Insets(8, 10, 8, 10);
        JLabel line = new JLabel("—");
        line.setForeground(UiTheme.ACCENT);
        line.setFont(UiTheme.font(Font.BOLD, 20F));
        content.add(line, grid);
        grid.gridy = 3;
        JLabel english = new JLabel("VIRTUAL CAMPUS MANAGEMENT SYSTEM");
        english.setForeground(new Color(158, 179, 204));
        english.setFont(UiTheme.font(Font.BOLD, 11F));
        content.add(english, grid);
        grid.gridy = 4;
        grid.insets = new Insets(20, 10, 8, 10);
        JLabel slogan = new JLabel("高效   ·   便捷   ·   智慧");
        slogan.setForeground(new Color(122, 151, 182));
        content.add(slogan, grid);
        add(content);
    }
}
