package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.client.view.theme.UiFactory;
import edu.seu.vcampus.common.user.entity.SessionEntry;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;

/**
 * 主窗口面包屑、全局搜索、设置和账户顶栏。
 *
 * <p>
 * 右上角账户区是<b>可点击入口</b>（{@link AccountTriggerButton}）：点击后在按钮下方右对齐弹出
 * {@link AccountPopupPanel}，资料、修改密码、退出登录都在里面。原来的「用户中心」页因此不再需要。
 */
public class MainHeaderPanel extends JPanel {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 全局搜索输入框。 */
    private final JTextField searchField = new JTextField(20);

    /** 账户触发按钮。 */
    private final AccountTriggerButton accountButton;

    /** 账户弹窗容器。 */
    private final JPopupMenu accountMenu = new JPopupMenu();

    /** 账户弹窗内容。 */
    private final AccountPopupPanel accountPopup;

    /**
     * 创建顶栏（不接入账户动作，兼容旧调用）。
     *
     * @param userId   登录名
     * @param role     登录身份
     * @param search   搜索回调
     * @param settings 设置回调
     */
    public MainHeaderPanel(String userId, String role, StringHandler search, Runnable settings) {
        this(new SessionEntry(null, userId, role, 0L), search, settings, null, null);
    }

    /**
     * 创建顶栏并接入账户动作。
     *
     * @param session        当前会话；null 表示未登录
     * @param search         搜索回调
     * @param settings       设置回调
     * @param changePassword 「修改密码」回调；null 表示不响应
     * @param logout         「退出登录」回调；null 表示不响应
     */
    public MainHeaderPanel(SessionEntry session, StringHandler search, Runnable settings,
            Runnable changePassword, Runnable logout) {
        this.accountPopup = new AccountPopupPanel(session, changePassword, logout);
        this.accountButton = new AccountTriggerButton(session);
        setLayout(new BorderLayout(22, 0));
        setBackground(edu.seu.vcampus.client.view.theme.UiTheme.BACKGROUND);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(225, 218, 204)),
                BorderFactory.createEmptyBorder(14, 24, 14, 24)));
        add(createBreadcrumb(), BorderLayout.WEST);
        add(createSearch(search), BorderLayout.CENTER);
        add(createActions(settings), BorderLayout.EAST);
    }

    private JPanel createBreadcrumb() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        panel.setOpaque(false);
        JLabel home = new JLabel("首页");
        home.setForeground(edu.seu.vcampus.client.view.theme.UiTheme.TEXT);
        home.setFont(edu.seu.vcampus.client.view.theme.UiTheme.font(Font.BOLD, 14F));
        JLabel separator = new JLabel("/");
        separator.setForeground(edu.seu.vcampus.client.view.theme.UiTheme.MUTED);
        JLabel page = new JLabel("工作台");
        page.setForeground(edu.seu.vcampus.client.view.theme.UiTheme.MUTED);
        panel.add(home);
        panel.add(separator);
        panel.add(page);
        return panel;
    }

    private JPanel createSearch(final StringHandler search) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 2));
        panel.setOpaque(false);
        searchField.setPreferredSize(new Dimension(270, 36));
        searchField.setToolTipText("搜索个人信息、课程、图书馆等校园功能");
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 216, 208)),
                BorderFactory.createEmptyBorder(6, 11, 6, 11)));
        ActionListener searchAction = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                search.handle(searchField.getText());
            }
        };
        searchField.addActionListener(searchAction);
        panel.add(searchField);
        JButton button = UiFactory.primaryButton("搜索", "search");
        button.addActionListener(searchAction);
        panel.add(button);
        return panel;
    }

    private JPanel createActions(final Runnable settings) {
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        JButton settingButton = UiFactory.secondaryButton("设置", "settings");
        settingButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                settings.run();
            }
        });
        actions.add(settingButton);
        accountMenu.setBorder(BorderFactory.createEmptyBorder());
        accountMenu.add(accountPopup);
        accountButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                showAccountMenu();
            }
        });
        actions.add(accountButton);
        return actions;
    }

    /** 在账户按钮下方、右对齐弹出账户面板。 */
    public void showAccountMenu() {
        accountMenu.show(accountButton, accountButton.getWidth()
                - accountPopup.getPreferredSize().width, accountButton.getHeight() + 6);
    }

    /** @return 账户触发按钮 */
    public JButton getAccountButton() {
        return accountButton;
    }

    /** @return 账户弹窗内容面板 */
    public AccountPopupPanel getAccountPopup() {
        return accountPopup;
    }

    /** @return 账户弹窗当前是否可见 */
    public boolean isAccountMenuVisible() {
        return accountMenu.isVisible();
    }
}
