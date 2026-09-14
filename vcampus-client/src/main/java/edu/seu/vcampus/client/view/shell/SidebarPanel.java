package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.client.view.theme.UiIcons;
import edu.seu.vcampus.client.view.theme.UiTheme;
import edu.seu.vcampus.client.view.component.NavigationButton;
import edu.seu.vcampus.common.user.entity.Capability;
import edu.seu.vcampus.common.user.entity.Permissions;
import edu.seu.vcampus.common.user.entity.Role;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * 主窗口侧栏，负责展示一级导航并维护选中状态。
 *
 * <p>
 * 导航项带<b>能力要求</b>：只有能力为 null（人人可见）或当前角色具备该能力时才出现。
 * 客户端过滤只负责「不显示」，真正的准入仍是服务端 403（见 ADR-0009 D6）。
 */
public class SidebarPanel extends JPanel implements StringHandler {

    private static final long serialVersionUID = 1L;

    /** 导航项：{显示名, 页面标识, 所需能力（可为 null）}。 */
    private static final Object[][] NAVIGATION = {
        {"工作台", PageNames.HOME, null},
        {"个人信息", PageNames.STUDENT, null},
        {"选课与成绩", PageNames.COURSE, null},
        {"图书馆", PageNames.LIBRARY, null},
        {"校园商店", PageNames.SHOP, null},
        {"校园银行", PageNames.BANK, null},
        {"用户管理", PageNames.USER_ADMIN, Capability.USER_MANAGE}
    };
    private final Map<String, NavigationButton> buttons =
            new LinkedHashMap<String, NavigationButton>();
    private final StringHandler navigator;
    private String selectedPage = PageNames.HOME;

    /**
     * 创建侧栏（不做角色过滤，全部导航项可见）。
     *
     * @param navigator 点击菜单后的页面跳转回调
     */
    public SidebarPanel(StringHandler navigator) {
        this(navigator, null);
    }

    /**
     * 创建侧栏并按角色过滤导航项。
     *
     * @param navigator 点击菜单后的页面跳转回调
     * @param role      当前角色；null 表示不过滤
     */
    public SidebarPanel(StringHandler navigator, Role role) {
        this.navigator = navigator;
        setLayout(new BorderLayout());
        setBackground(UiTheme.NAVY);
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, UiTheme.NAVY_LIGHT));
        add(createTop(), BorderLayout.NORTH);
        add(createItems(role), BorderLayout.CENTER);
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

    private JPanel createItems(Role role) {
        int visible = 0;
        for (Object[] item : NAVIGATION) {
            if (visibleTo(item, role)) {
                visible++;
            }
        }
        JPanel items = new JPanel(new GridLayout(visible, 1, 0, 8));
        items.setOpaque(false);
        items.setBorder(BorderFactory.createEmptyBorder(8, 0, 20, 0));
        for (Object[] item : NAVIGATION) {
            if (!visibleTo(item, role)) {
                continue;
            }
            String page = (String) item[1];
            NavigationButton button = createButton((String) item[0], page);
            buttons.put(page, button);
            items.add(button);
        }
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(items, BorderLayout.NORTH);
        return wrapper;
    }

    /** 判断导航项对当前角色是否可见（能力为 null 表示人人可见）。 */
    private boolean visibleTo(Object[] item, Role role) {
        Capability required = (Capability) item[2];
        return required == null || Permissions.can(role, required);
    }

    private NavigationButton createButton(String label, final String page) {
        NavigationButton button = new NavigationButton(label, page);
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (navigator != null) {
                    navigator.handle(page);
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
        for (Map.Entry<String, NavigationButton> entry : buttons.entrySet()) {
            boolean selected = entry.getKey().equals(page);
            entry.getValue().setSelectedState(selected);
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

    /**
     * 判断某个页面当前是否出现在侧栏（即当前角色是否有权访问）。
     *
     * @param page 页面标识
     * @return 可见返回 true
     */
    public boolean isVisible(String page) {
        return buttons.containsKey(page);
    }

    /** {@inheritDoc} */
    @Override
    public void handle(String page) {
        selectPage(page);
    }

}
