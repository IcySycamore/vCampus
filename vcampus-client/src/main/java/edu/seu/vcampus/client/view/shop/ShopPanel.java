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
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * 商店主面板。
 */
public class ShopPanel extends JPanel {

    private static final int CATALOG_GAP = 14;
    private static final int MAX_COLUMNS = 4;

    private final ShopService api;
    private final Runnable paymentSuccess;
    private final JLabel feedback;
    private final JLabel countLabel;
    private final JButton refreshButton;
    private final JButton myOrdersButton;
    private final JTextField searchField;
    private final JComboBox<String> categoryFilter;
    private final JPanel itemsContainer;
    private final GridLayout itemGrid;
    private final JScrollPane scrollPane;
    private final List<ShopItem> catalogItems = new ArrayList<ShopItem>();
    private JFrame orderFrame;
    private boolean busy;
    private boolean itemRefreshPending;

    /** 首屏是否已成功拿到过商品；没有就在首次显示时补一次。 */
    private boolean catalogLoaded;

    /** 首屏补加载是否已经排过，避免反复排定时器。 */
    private boolean initialLoadScheduled;
    private int visibleItemCount;

    /**
     * 创建商店面板。
     *
     * @param api 商店服务
     */
    public ShopPanel(ShopService api) {
        this(api, null);
    }

    /**
     * 创建商店面板，并在支付成功后通知外部页面刷新。
     *
     * @param api            商店服务
     * @param paymentSuccess 支付成功回调；可为空
     */
    public ShopPanel(ShopService api, Runnable paymentSuccess) {
        this.api = api;
        this.paymentSuccess = paymentSuccess;
        this.busy = false;

        setLayout(new BorderLayout(0, 16));
        setBorder(BorderFactory.createEmptyBorder(22, 24, 20, 24));

        refreshButton = UiFactory.secondaryButton("刷新", "refresh");
        myOrdersButton = UiFactory.primaryButton("我的订单", "shop");
        searchField = new JTextField();
        searchField.setPreferredSize(new Dimension(220, 36));
        searchField.setToolTipText("搜索商品名称或描述");
        categoryFilter = new JComboBox<String>(ShopCategoryCatalog.filterNames());
        categoryFilter.setPreferredSize(new Dimension(130, 36));
        countLabel = new JLabel("0 件商品");
        countLabel.setForeground(UiTheme.MUTED);
        feedback = new JLabel(" ");
        feedback.setForeground(UiTheme.TEXT);
        add(createCatalogHeader(), BorderLayout.NORTH);

        itemGrid = new GridLayout(0, 3, CATALOG_GAP, CATALOG_GAP);
        itemsContainer = new JPanel();
        itemsContainer.setLayout(itemGrid);
        itemsContainer.setOpaque(false);
        itemsContainer.setBorder(BorderFactory.createEmptyBorder(2, 2, 10, 2));

        scrollPane = new JScrollPane(itemsContainer);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setPreferredSize(new Dimension(820, 520));
        scrollPane.getVerticalScrollBar().setUnitIncrement(22);
        scrollPane.getViewport().setBackground(UiTheme.BACKGROUND);
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
        searchField.getDocument().addDocumentListener(catalogFilterListener());
        categoryFilter.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                applyCatalogFilter();
            }
        });
        scrollPane.getViewport().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                updateCatalogLayout();
            }
        });

        // 首屏加载：面板契约是「构造完就发出一次商品请求」（ShopPanelTest 按此断言）。
        // 但这一次请求可能被丢掉——构造发生在登录收尾阶段——所以 addNotify 里再兜一道重试。
        loadItems();
    }

    /**
     * 首次真正进入显示树时拉一次商品列表，并补一次延迟重试。
     *
     * <p>
     * 只靠一次请求不够稳：登录刚成功时连接池/回复追踪可能还没就绪，响应会被丢掉。 延迟再试一次就能盖住这种竞态，用户不必自己点「刷新」。
     */
    @Override
    public void addNotify() {
        super.addNotify();
        if (catalogLoaded || initialLoadScheduled) {
            return;
        }
        initialLoadScheduled = true;
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                loadItems();
            }
        });
        javax.swing.Timer retry = new javax.swing.Timer(1200, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (!catalogLoaded) {
                    loadItems();
                }
            }
        });
        retry.setRepeats(false);
        retry.start();
    }

    private JPanel createCatalogHeader() {
        JPanel header = new JPanel(new BorderLayout(0, 14));
        header.setOpaque(false);

        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("校园 Shop");
        title.setFont(UiTheme.font(java.awt.Font.BOLD, UiTheme.SIZE_TITLE));
        title.setForeground(UiTheme.TEXT);
        JLabel subtitle = new JLabel("课程之外的学习、生活与校园纪念好物");
        subtitle.setFont(UiTheme.font(java.awt.Font.PLAIN, UiTheme.SIZE_SUBTITLE));
        subtitle.setForeground(UiTheme.MUTED);
        text.add(title);
        text.add(subtitle);
        heading.add(text, BorderLayout.WEST);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(refreshButton);
        actions.add(myOrdersButton);
        heading.add(actions, BorderLayout.EAST);
        header.add(heading, BorderLayout.NORTH);

        JPanel filters = new JPanel(new BorderLayout(12, 0));
        filters.setOpaque(false);
        JPanel inputs = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        inputs.setOpaque(false);
        inputs.add(new JLabel("搜索商品"));
        inputs.add(searchField);
        inputs.add(categoryFilter);
        filters.add(inputs, BorderLayout.WEST);
        filters.add(countLabel, BorderLayout.EAST);
        header.add(filters, BorderLayout.CENTER);
        header.add(feedback, BorderLayout.SOUTH);
        return header;
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
                catalogLoaded = true;
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
        catalogItems.clear();
        if (items != null) {
            catalogItems.addAll(items);
        }
        applyCatalogFilter();
    }

    private void applyCatalogFilter() {
        itemsContainer.removeAll();
        String query = searchField.getText() == null ? ""
                : searchField.getText().trim().toLowerCase(Locale.ROOT);
        String category = ShopCategoryCatalog.idAt(categoryFilter.getSelectedIndex());
        visibleItemCount = 0;
        for (ShopItem item : catalogItems) {
            if (matchesQuery(item, query) && ShopCategoryCatalog.matches(item, category)) {
                ShopItemCard card = new ShopItemCard(item, new ShopItemCard.PurchaseListener() {
                    @Override
                    public void onPurchase(ShopItem item, int quantity) {
                        createOrder(item, quantity);
                    }
                });
                itemsContainer.add(card);
                visibleItemCount++;
            }
        }
        if (visibleItemCount == 0) {
            JLabel empty = new JLabel(catalogItems.isEmpty()
                    ? "暂无商品"
                    : "没有符合条件的商品", SwingConstants.CENTER);
            empty.setFont(UiTheme.font(java.awt.Font.PLAIN, 14));
            empty.setForeground(UiTheme.MUTED);
            itemsContainer.add(empty);
        }
        countLabel.setText(visibleItemCount + " 件商品");
        updateCatalogLayout();
        itemsContainer.revalidate();
        itemsContainer.repaint();
    }

    private boolean matchesQuery(ShopItem item, String query) {
        if (query.length() == 0) {
            return true;
        }
        String name = item.getSiName() == null ? "" : item.getSiName();
        String description = item.getSiDesc() == null ? "" : item.getSiDesc();
        return name.toLowerCase(Locale.ROOT).contains(query)
                || description.toLowerCase(Locale.ROOT).contains(query);
    }

    private void updateCatalogLayout() {
        int width = scrollPane == null ? 0 : scrollPane.getViewport().getExtentSize().width;
        if (width <= 0) {
            width = 820;
        }
        int usable = Math.max(ShopItemCard.MINIMUM_WIDTH, width - 4);
        int columns = Math.max(1, (usable + CATALOG_GAP)
                / (ShopItemCard.MINIMUM_WIDTH + CATALOG_GAP));
        columns = Math.min(MAX_COLUMNS, columns);
        itemGrid.setColumns(columns);
        int cells = Math.max(1, visibleItemCount);
        int rows = (cells + columns - 1) / columns;
        int height = rows * ShopItemCard.CARD_HEIGHT
                + Math.max(0, rows - 1) * CATALOG_GAP + 12;
        itemsContainer.setPreferredSize(new Dimension(usable, height));
        itemsContainer.revalidate();
    }

    private DocumentListener catalogFilterListener() {
        return new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                applyCatalogFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                applyCatalogFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                applyCatalogFilter();
            }
        };
    }

    /**
     * 创建订单。
     *
     * @param item     商品
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
        if (orderFrame != null && orderFrame.isDisplayable()) {
            orderFrame.setState(Frame.NORMAL);
            orderFrame.setVisible(true);
            orderFrame.toFront();
            orderFrame.requestFocus();
            return;
        }

        final JFrame frame = new JFrame("我的订单");
        orderFrame = frame;
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setContentPane(new ShopOrderPanel(api, new Runnable() {
            @Override
            public void run() {
                paymentCompleted();
            }
        }));
        frame.setSize(900, 600);
        frame.setLocationRelativeTo(this);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent event) {
                if (orderFrame == frame) {
                    orderFrame = null;
                }
            }
        });
        frame.setVisible(true);
    }

    void paymentCompleted() {
        if (busy) {
            itemRefreshPending = true;
        } else {
            loadItems();
        }
        if (paymentSuccess != null) {
            paymentSuccess.run();
        }
    }

    /**
     * 设置忙碌状态。
     *
     * @param value   是否忙碌
     * @param message 反馈消息
     */
    private void setBusy(boolean value, String message) {
        busy = value;
        refreshButton.setEnabled(!value);
        myOrdersButton.setEnabled(!value);
        feedback.setText(message);
        feedback.setForeground(UiTheme.TEXT);
        if (!value && itemRefreshPending) {
            itemRefreshPending = false;
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    loadItems();
                }
            });
        }
    }
}
