package edu.seu.vcampus.client.view.shell;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
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
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * 登录后的客户端主窗口。
 */
public class MainFrame extends JFrame {
    private static final long serialVersionUID = 1L;
    private static final String[][] NAVIGATION = {
        {"工作台", MainContentPanel.HOME},
        {"用户中心", MainContentPanel.USER},
        {"学籍管理", MainContentPanel.STUDENT},
        {"选课与成绩", MainContentPanel.COURSE},
        {"图书馆", MainContentPanel.LIBRARY},
        {"校园商店", MainContentPanel.SHOP},
        {"校园银行", MainContentPanel.BANK}
    };
    private final MainContentPanel contentPanel;
    private final Map<String, JButton> navigation = new LinkedHashMap<String, JButton>();
    private String currentPage = MainContentPanel.HOME;

    /**
     * 创建主窗口。
     *
     * @param userId 当前用户 ID
     */
    public MainFrame(String userId) {
        this(userId, "学生");
    }

    /**
     * 创建带身份信息的主窗口。
     *
     * @param userId 当前用户 ID
     * @param role 当前登录身份
     */
    public MainFrame(String userId, String role) {
        super("vCampus 虚拟校园");
        contentPanel = new MainContentPanel(userId, role);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1000, 650));
        setSize(1180, 760);
        setLocationRelativeTo(null);
        getContentPane().setBackground(UiTheme.BACKGROUND);
        JPanel sidebar = createNavigation();
        add(sidebar, BorderLayout.WEST);
        ResponsiveTypography.installProportionalWidth(this, sidebar, 0.15F);
        JPanel workspace = new JPanel(new BorderLayout());
        workspace.setBackground(UiTheme.BACKGROUND);
        workspace.add(new MainHeaderPanel(userId, role, new StringAction() {
            @Override
            public void accept(String query) {
                openSearch(query);
            }
        }, new Runnable() {
            @Override
            public void run() {
                new SettingsDialog(MainFrame.this).setVisible(true);
            }
        }), BorderLayout.NORTH);
        workspace.add(contentPanel, BorderLayout.CENTER);
        add(workspace, BorderLayout.CENTER);
        contentPanel.setPageChangeListener(new StringAction() {
            @Override
            public void accept(String page) {
                updateNavigation(page);
            }
        });
        updateNavigation(MainContentPanel.HOME);
        ResponsiveTypography.install(this, 1180);
    }

    private JPanel createNavigation() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(UiTheme.NAVY);
        wrapper.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, UiTheme.NAVY_LIGHT));
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JPanel brand = new JPanel(new BorderLayout(12, 0));
        brand.setOpaque(false);
        brand.setBorder(BorderFactory.createEmptyBorder(22, 22, 20, 18));
        brand.add(new JLabel(UiIcons.load("student-light", 34)), BorderLayout.WEST);
        JPanel brandText = new JPanel(new GridLayout(2, 1, 0, 1));
        brandText.setOpaque(false);
        JLabel brandTitle = new JLabel("vCampus");
        brandTitle.setForeground(Color.WHITE);
        brandTitle.setFont(UiTheme.font(Font.BOLD, 20F));
        JLabel brandSub = new JLabel("智慧校园");
        brandSub.setForeground(new Color(157, 176, 198));
        brandText.add(brandTitle);
        brandText.add(brandSub);
        brand.add(brandText, BorderLayout.CENTER);
        top.add(brand, BorderLayout.NORTH);
        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        heading.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UiTheme.NAVY_LIGHT),
                BorderFactory.createEmptyBorder(18, 22, 8, 18)));
        JLabel label = new JLabel("校园服务导航");
        label.setForeground(new Color(126, 150, 177));
        label.setFont(UiTheme.font(Font.BOLD, 12F));
        heading.add(label);
        top.add(heading, BorderLayout.SOUTH);
        wrapper.add(top, BorderLayout.NORTH);
        JPanel items = new JPanel(new GridLayout(NAVIGATION.length, 1, 0, 8));
        items.setOpaque(false);
        items.setBorder(BorderFactory.createEmptyBorder(8, 0, 20, 0));
        for (String[] item : NAVIGATION) {
            JButton button = createNavigationButton(item[0], item[1]);
            navigation.put(item[1], button);
            items.add(button);
        }
        JPanel itemsWrapper = new JPanel(new BorderLayout());
        itemsWrapper.setOpaque(false);
        itemsWrapper.add(items, BorderLayout.NORTH);
        wrapper.add(itemsWrapper, BorderLayout.CENTER);
        JLabel footer = new JLabel("<html><div style='text-align:center;color:#8fa3b9'>"
                + "<b>● 系统在线</b><br>vCampus · 2026</div></html>",
                SwingConstants.CENTER);
        footer.setForeground(new Color(148, 162, 173));
        footer.setBorder(BorderFactory.createEmptyBorder(18, 8, 22, 8));
        wrapper.add(footer, BorderLayout.SOUTH);
        return wrapper;
    }

    private JButton createNavigationButton(String label, final String page) {
        final JButton button = new JButton(label, UiIcons.load(page + "-light", 22));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setIconTextGap(13);
        button.setFont(UiTheme.font(Font.BOLD, 16F));
        button.setForeground(new Color(190, 204, 219));
        button.setBorder(BorderFactory.createEmptyBorder(16, 27, 16, 18));
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setFocusPainted(false);
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                contentPanel.showPage(page);
            }
        });
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                if (!page.equals(currentPage)) {
                    button.setForeground(Color.WHITE);
                }
            }

            @Override
            public void mouseExited(MouseEvent event) {
                if (!page.equals(currentPage)) {
                    button.setForeground(new Color(190, 204, 219));
                }
            }
        });
        return button;
    }

    private void updateNavigation(String page) {
        currentPage = page;
        for (Map.Entry<String, JButton> entry : navigation.entrySet()) {
            boolean selected = entry.getKey().equals(page);
            entry.getValue().setForeground(selected ? Color.WHITE : new Color(190, 204, 219));
            entry.getValue().setBorder(selected
                    ? BorderFactory.createCompoundBorder(
                            BorderFactory.createMatteBorder(0, 4, 0, 0, UiTheme.ACCENT),
                            BorderFactory.createEmptyBorder(16, 23, 16, 18))
                    : BorderFactory.createEmptyBorder(16, 27, 16, 18));
        }
    }

    private void openSearch(String query) {
        new GlobalSearchDialog(this, query, new StringAction() {
            @Override
            public void accept(String page) {
                contentPanel.showPage(page);
            }
        }).setVisible(true);
    }
}
