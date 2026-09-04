package edu.seu.vcampus.client.view.shell;

import java.awt.CardLayout;
import java.awt.Component;
import javax.swing.JPanel;

/**
 * 管理一级页面注册、切换和当前页面状态。
 */
public class AppRouter {

    private final CardLayout layout = new CardLayout();
    private final JPanel container;
    private String currentPage;

    /**
     * 创建页面路由。
     *
     * @param container 页面容器
     * @param initialPage 初始页面标识
     */
    public AppRouter(JPanel container, String initialPage) {
        this.container = container;
        currentPage = initialPage;
        container.setLayout(layout);
    }

    /**
     * 注册一个页面。
     *
     * @param page 页面标识
     * @param component 页面组件
     */
    public void register(String page, Component component) {
        container.add(component, page);
    }

    /**
     * 切换页面。
     *
     * @param page 页面标识
     */
    public void navigate(String page) {
        currentPage = page;
        layout.show(container, page);
    }

    /**
     * 返回当前页面。
     *
     * @return 页面标识
     */
    public String getCurrentPage() {
        return currentPage;
    }
}
