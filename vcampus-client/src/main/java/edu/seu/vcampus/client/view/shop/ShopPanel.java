package edu.seu.vcampus.client.view.shop;

import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.client.shop.ShopService;
import edu.seu.vcampus.client.view.UiTasks;
import edu.seu.vcampus.client.view.theme.UiFactory;
import edu.seu.vcampus.client.view.theme.UiTheme;
import edu.seu.vcampus.common.shop.entity.ShopItem;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

/**
 * 商店主面板。
 */
public class ShopPanel extends JPanel {

    private final ShopService api;
    private final JLabel feedback;
    private final JButton refreshButton;
    private final JButton myOrdersButton;
    private final JPanel itemsContainer;
    private final JScrollPane scrollPane;
    private boolean busy;

    /**
     * 创建商店面板。
     *
     * @param api 商店服务
     */
    public ShopPanel(ShopService api) {
        this.api = api;
        this.busy = false;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // 顶部工具栏
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        JLabel title = new JLabel("虚拟校园商店");
        title.setFont(UiTheme.font(java.awt.Font.BOLD, 20));
        toolbar.add(title);

        refreshButton = UiFactory.primaryButton("刷新", null);
        toolbar.add(refreshButton);

        myOrdersButton = UiFactory.primaryButton("我的订单", null);
        toolbar.add(myOrdersButton);

        feedback = new JLabel(" ");
        feedback.setForeground(UiTheme.TEXT);
        toolbar.add(feedback);

        add(toolbar, BorderLayout.NORTH);

        // 商品列表容器
        itemsContainer = new JPanel();
        itemsContainer.setLayout(new GridLayout(0, 3, 15, 15)); // 3列网格
        itemsContainer.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        scrollPane = new JScrollPane(itemsContainer);
        scrollPane.setPreferredSize(new Dimension(800, 500));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        // 事件监听
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadItems();
            }
        });

        myOrdersButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showMyOrders();
            }
        });

        // 初始加载
        loadItems();
    }

    /**
     * 加载商品列表。
     */
    private void loadItems() {
        if (busy || api == null) {
            return;
        }
        setBusy(true, "正在加载商品...");

        UiTasks.run(new UiTasks.Task<List<ShopItem>>() {
            @Override
            public List<ShopItem> run() throws ApiException {
                return api.listItems();
            }
        }, new UiTasks.Success<List<ShopItem>>() {
            @Override
            public void accept(List<ShopItem> items) {
                displayItems(items);
                setBusy(false, "商品加载成功");
            }
        }, new UiTasks.Failure() {
            @Override
            public void accept(ApiException error) {
                setBusy(false, "加载失败: " + error.getMessage());
                feedback.setForeground(UiTheme.ACCENT);
            }
        });
    }

    /**
     * 显示商品列表。
     *
     * @param items 商品列表
     */
    private void displayItems(List<ShopItem> items) {
        itemsContainer.removeAll();

        if (items == null || items.isEmpty()) {
            JLabel emptyLabel = new JLabel("暂无商品", SwingConstants.CENTER);
            emptyLabel.setForeground(UiTheme.MUTED);
            itemsContainer.add(emptyLabel);
        } else {
            for (ShopItem item : items) {
                ShopItemCard card = new ShopItemCard(item, new ShopItemCard.PurchaseListener() {
                    @Override
                    public void onPurchase(ShopItem item, int quantity) {
                        createOrder(item, quantity);
                    }
                });
                itemsContainer.add(card);
            }
        }

        itemsContainer.revalidate();
        itemsContainer.repaint();
    }

    /**
     * 创建订单。
     *
     * @param item 商品
     * @param quantity 数量
     */
    private void createOrder(final ShopItem item, final int quantity) {
        if (busy) {
            return;
        }
        setBusy(true, "正在创建订单...");

        UiTasks.run(new UiTasks.Task<Void>() {
            @Override
            public Void run() throws ApiException {
                api.createOrder(item.getSiId(), quantity);
                return null;
            }
        }, new UiTasks.Success<Void>() {
            @Override
            public void accept(Void result) {
                setBusy(false, "订单创建成功！请在「我的订单」中查看");
                feedback.setForeground(UiTheme.SUCCESS);
            }
        }, new UiTasks.Failure() {
            @Override
            public void accept(ApiException error) {
                setBusy(false, "订单创建失败: " + error.getMessage());
                feedback.setForeground(UiTheme.ACCENT);
            }
        });
    }

    /**
     * 显示我的订单。
     */
    private void showMyOrders() {
        // 创建订单面板窗口
        javax.swing.JFrame orderFrame = new javax.swing.JFrame("我的订单");
        orderFrame.setDefaultCloseOperation(javax.swing.JFrame.DISPOSE_ON_CLOSE);
        orderFrame.setContentPane(new ShopOrderPanel(api));
        orderFrame.setSize(900, 600);
        orderFrame.setLocationRelativeTo(this);
        orderFrame.setVisible(true);
    }

    /**
     * 设置忙碌状态。
     *
     * @param value 是否忙碌
     * @param message 反馈消息
     */
    private void setBusy(boolean value, String message) {
        busy = value;
        refreshButton.setEnabled(!value);
        myOrdersButton.setEnabled(!value);
        feedback.setText(message);
        feedback.setForeground(UiTheme.TEXT);
    }
}
