package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.client.view.component.RoundedPanel;
import edu.seu.vcampus.client.view.theme.UiIcons;
import edu.seu.vcampus.client.view.theme.UiTheme;
import edu.seu.vcampus.common.user.entity.Capability;
import edu.seu.vcampus.common.user.entity.Permissions;
import edu.seu.vcampus.common.user.entity.Role;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * 工作台「快捷入口」磁贴网格：按角色能力过滤。
 *
 * <p>
 * 从 {@code OaDashboardPanel} 里抽出来（原文件逼近 200 行上限），同时修掉「用户中心」入口： 用户中心页已下线，管理员这里改为直达「用户管理」，非管理员看不到该磁贴。
 */
public class DashboardServicesPanel extends JPanel {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 入口定义：{图标, 标题, 页面标识, 所需能力（可为 null 表示人人可见）}。 */
    private static final Object[][] SERVICES = {
            { "student", "个人信息", PageNames.STUDENT, null },
            { "course", "选课成绩", PageNames.COURSE, null },
            { "library", "图书馆", PageNames.LIBRARY, null },
            { "shop", "校园商店", PageNames.SHOP, null },
            { "bank", "校园银行", PageNames.BANK, null },
            { "user", "用户管理", PageNames.USER_ADMIN, Capability.USER_MANAGE }
    };

    /** 页面跳转回调。 */
    private final StringHandler navigator;

    /** 当前角色；null 表示不过滤。 */
    private final Role role;

    /**
     * 创建快捷入口网格。
     *
     * @param role      当前角色；null 表示不过滤
     * @param navigator 页面跳转回调
     */
    public DashboardServicesPanel(Role role, StringHandler navigator) {
        this.role = role;
        this.navigator = navigator;
        setOpaque(false);
        setLayout(new GridLayout(0, 3, 14, 14));
        for (Object[] item : SERVICES) {
            if (visibleTo(item, role)) {
                add(createCard(item));
            }
        }
    }

    /**
     * 统计某角色可见的入口数量。
     *
     * @param role 角色；null 表示不过滤
     * @return 可见入口数
     */
    public static int visibleCount(Role role) {
        int count = 0;
        for (Object[] item : SERVICES) {
            if (visibleTo(item, role)) {
                count++;
            }
        }
        return count;
    }

    /** 能力过滤：无能力要求或角色具备该能力时可见。 */
    private static boolean visibleTo(Object[] item, Role role) {
        Capability required = (Capability) item[3];
        return required == null || Permissions.can(role, required);
    }

    /** 构造单个磁贴。 */
    private JPanel createCard(Object[] item) {
        JPanel card = new RoundedPanel(new BorderLayout(0, 2), 12, new Color(235, 235, 231));
        card.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        JLabel icon = UiIcons.responsiveLabel((String) item[0], 20, SwingConstants.CENTER);
        JLabel title = new JLabel((String) item[1], SwingConstants.CENTER);
        title.setForeground(UiTheme.TEXT);
        title.setFont(UiTheme.font(Font.PLAIN, 14F));
        card.add(icon, BorderLayout.CENTER);
        card.add(title, BorderLayout.SOUTH);
        final String page = (String) item[2];
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (navigator != null) {
                    navigator.handle(page);
                }
            }
        });
        return card;
    }
}
