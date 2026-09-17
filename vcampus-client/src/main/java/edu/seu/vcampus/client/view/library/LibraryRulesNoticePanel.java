package edu.seu.vcampus.client.view.library;

import edu.seu.vcampus.client.view.component.RoundedPanel;
import edu.seu.vcampus.client.view.theme.UiTheme;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** 首页图书馆规则公告。 */
final class LibraryRulesNoticePanel extends JPanel {
    private static final long serialVersionUID = 1L;

    LibraryRulesNoticePanel() {
        setName("libraryHomeRules");
        setLayout(new BorderLayout(0, 9));
        setOpaque(false);
        JLabel title = new JLabel("规则公告");
        title.setFont(UiTheme.font(Font.BOLD, 17F));
        title.setForeground(UiTheme.TEXT);
        add(title, BorderLayout.NORTH);
        RoundedPanel card = new RoundedPanel(new GridLayout(4, 1, 0, 5),
                16, new Color(242, 247, 249));
        card.setBorder(BorderFactory.createEmptyBorder(11, 14, 11, 14));
        card.add(rule("借期 30 天，最多续借 2 次"));
        card.add(rule("逾期图书需先归还并结清滞纳金"));
        card.add(rule("无可借馆藏时可提交预约"));
        card.add(rule("预约到馆后保留 15 天"));
        add(card, BorderLayout.CENTER);
    }

    private JLabel rule(String text) {
        JLabel label = new JLabel("•  " + text);
        label.setForeground(UiTheme.TEXT);
        label.setFont(UiTheme.font(Font.PLAIN, 13F));
        return label;
    }
}
