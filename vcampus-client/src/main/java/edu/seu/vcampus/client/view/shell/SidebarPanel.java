package edu.seu.vcampus.client.view.shell;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * 主窗口侧栏，负责展示一级导航并维护选中状态。
 */
public class SidebarPanel extends JPanel implements StringHandler {

    private static final long serialVersionUID = 1L;
    private static final Color NORMAL_TEXT = new Color(190, 204, 219);
    private static final String[][] NAVIGATION = {
        {"工作台", PageNames.HOME},
        {"用户中心", PageNames.USER},
        {"学籍管理", PageNames.STUDENT},
        {"选课与成绩", PageNames.COURSE},
        {"图书馆", PageNames.LIBRARY},
        {"校园商店", PageNames.SHOP},
        {"校园银行", PageNames.BANK}
    };
    private final Map<String, JButton> buttons = new LinkedHashMap<String, JButton>();
    private final StringHandler navigator;
    private String selectedPage = PageNames.HOME;

    /**
     * 创建侧栏。
     *
     * @param navigator 点击菜单后的页面跳转回调
     */
    public SidebarPanel(StringHandler navigator) {
        this.navigator = navigator;
        setLayout(new BorderLayout());
        setBackground(UiTheme.NAVY);
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, UiTheme.NAVY_LIGHT));
        add(createTop(), BorderLayout.NORTH);
        add(createItems(), BorderLayout.CENTER);
        add(createFooter(), BorderLayout.SOUTH);
        selectPage(PageNames.HOME);
    }

    private JPanel createTop() {
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JPanel brand = new JPanel(new BorderLayout(12, 0));
        brand.setOpaque(false);
        brand.setBorder(BorderFactory.createEmptyBorder(22, 22, 20, 18));
        brand.add(new JLabel(UiIcons.load("student-light", 34)), BorderLayout.WEST);
        JPanel text = new JPanel(new GridLayout(2, 1, 0, 1));
        text.setOpaque(false);
        JLabel title = new JLabel("vCampus");
        title.setForeground(Color.WHITE);
        title.setFont(UiTheme.font(Font.BOLD, 20F));
        JLabel subtitle = new JLabel("智慧校园");
        subtitle.setForeground(new Color(157, 176, 198));
        text.add(title);
        text.add(subtitle);
        brand.add(text, BorderLayout.CENTER);
        top.add(brand, BorderLayout.NORTH);
        top.add(createHeading(), BorderLayout.SOUTH);
        return top;
    }

    private JPanel createHeading() {
        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        heading.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UiTheme.NAVY_LIGHT),
                BorderFactory.createEmptyBorder(18, 22, 8, 18)));
        JLabel label = new JLabel("校园服务导航");
        label.setForeground(new Color(126, 150, 177));
        label.setFont(UiTheme.font(Font.BOLD, 12F));
        heading.add(label);
        return heading;
    }

    private JPanel createItems() {
        JPanel items = new JPanel(new GridLayout(NAVIGATION.length, 1, 0, 8));
        items.setOpaque(false);
        items.setBorder(BorderFactory.createEmptyBorder(8, 0, 20, 0));
        for (String[] item : NAVIGATION) {
            JButton button = createButton(item[0], item[1]);
            buttons.put(item[1], button);
            items.add(button);
        }
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(items, BorderLayout.NORTH);
        return wrapper;
    }

    private JButton createButton(String label, final String page) {
        final JButton button = new JButton(label, UiIcons.load(page + "-light", 22));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setIconTextGap(13);
        button.setFont(UiTheme.font(Font.BOLD, 16F));
        button.setForeground(NORMAL_TEXT);
        button.setBorder(normalBorder());
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setFocusPainted(false);
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (navigator != null) {
                    navigator.handle(page);
                }
            }
        });
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                if (!page.equals(selectedPage)) {
                    button.setForeground(Color.WHITE);
                }
            }

            @Override
            public void mouseExited(MouseEvent event) {
                if (!page.equals(selectedPage)) {
                    button.setForeground(NORMAL_TEXT);
                }
            }
        });
        return button;
    }

    private JLabel createFooter() {
        JLabel footer = new JLabel("<html><div style='text-align:center;color:#8fa3b9'>"
                + "<b>● 系统在线</b><br>vCampus · 2026</div></html>", SwingConstants.CENTER);
        footer.setForeground(new Color(148, 162, 173));
        footer.setBorder(BorderFactory.createEmptyBorder(18, 8, 22, 8));
        return footer;
    }

    /**
     * 更新侧栏当前选中的页面。
     *
     * @param page 页面标识
     */
    public void selectPage(String page) {
        selectedPage = page;
        for (Map.Entry<String, JButton> entry : buttons.entrySet()) {
            boolean selected = entry.getKey().equals(page);
            entry.getValue().setForeground(selected ? Color.WHITE : NORMAL_TEXT);
            entry.getValue().setBorder(selected ? selectedBorder() : normalBorder());
        }
    }

    /**
     * 返回当前选中的页面。
     *
     * @return 页面标识
     */
    public String getSelectedPage() {
        return selectedPage;
    }

    /** {@inheritDoc} */
    @Override
    public void handle(String page) {
        selectPage(page);
    }

    private javax.swing.border.Border normalBorder() {
        return BorderFactory.createEmptyBorder(16, 27, 16, 18);
    }

    private javax.swing.border.Border selectedBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, UiTheme.ACCENT),
                BorderFactory.createEmptyBorder(16, 23, 16, 18));
    }
}
