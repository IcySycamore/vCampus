package edu.seu.vcampus.client.view.shop;

import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.client.shop.ShopService;
import edu.seu.vcampus.client.view.UiTasks;
import edu.seu.vcampus.client.view.theme.UiFactory;
import edu.seu.vcampus.client.view.theme.UiTheme;
import edu.seu.vcampus.common.shop.dto.OrderListResponse;
import edu.seu.vcampus.common.shop.dto.OrderQuery;
import edu.seu.vcampus.common.shop.entity.ShopOrder;
import edu.seu.vcampus.common.shop.entity.ShopOrderStatus;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

/**
 * 管理员订单管理面板。
 */
public class ShopAdminOrderPanel extends JPanel {

    private static final int PAGE_SIZE = 20;
    private static final String ALL_STATUS = "全部状态";

    private final ShopService api;
    private final boolean readOnly;
    private final JLabel feedback;
    private final JButton refreshButton;
    private final JButton advanceButton;
    private final JButton searchButton;
    private final JButton resetButton;
    private final JButton prevButton;
    private final JButton nextButton;
    private final JLabel pageLabel;
    private final JLabel totalLabel;
    private final JComboBox<String> statusFilter;
    private final JTextField userFilter;
    private final JTable orderTable;
    private final DefaultTableModel tableModel;
    private boolean busy;
    private int currentPage = 1;
    private long totalPages = 1;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * 创建管理员订单面板。
     *
     * @param api 商店服务
     */
    public ShopAdminOrderPanel(ShopService api) {
        this(api, false);
    }

    /**
     * 创建管理员订单面板。
     *
     * @param api 商店服务
     * @param readOnly 是否作为只读的全用户流水页
     */
    public ShopAdminOrderPanel(ShopService api, boolean readOnly) {
        this.api = api;
        this.readOnly = readOnly;
        this.busy = false;

        setLayout(new BorderLayout(0, 16));
        setBorder(BorderFactory.createEmptyBorder(22, 24, 20, 24));
        setBackground(UiTheme.BACKGROUND);

        JPanel header = new JPanel(new BorderLayout(0, 12));
        header.setOpaque(false);
        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        JPanel headingText = new JPanel();
        headingText.setOpaque(false);
        headingText.setLayout(new BoxLayout(headingText, BoxLayout.Y_AXIS));

        JLabel title = new JLabel(readOnly ? "全用户订单流水" : "订单管理");
        title.setFont(UiTheme.font(java.awt.Font.BOLD, 25));
        title.setForeground(UiTheme.TEXT);
        headingText.add(title);
        JLabel subtitle = new JLabel(readOnly
                ? "集中查看学生和教师在校园 Shop 中产生的全部订单记录"
                : "查询订单并推进已支付订单的履约状态");
        subtitle.setFont(UiTheme.font(java.awt.Font.PLAIN, 13));
        subtitle.setForeground(UiTheme.MUTED);
        headingText.add(subtitle);
        heading.add(headingText, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        refreshButton = UiFactory.secondaryButton("刷新", "refresh");
        actions.add(refreshButton);

        advanceButton = UiFactory.primaryButton("推进订单", "arrow-right");
        advanceButton.setEnabled(false);
        if (!readOnly) {
            actions.add(advanceButton);
        }
        heading.add(actions, BorderLayout.EAST);
        header.add(heading, BorderLayout.NORTH);

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filters.setOpaque(false);
        userFilter = new JTextField();
        userFilter.setPreferredSize(new Dimension(250, 36));
        userFilter.setToolTipText("输入完整用户 UUID");
        filters.add(new JLabel("用户 UUID"));
        filters.add(userFilter);
        statusFilter = new JComboBox<String>(statusOptions());
        statusFilter.setPreferredSize(new Dimension(130, 36));
        filters.add(statusFilter);
        searchButton = UiFactory.primaryButton("查询", "search");
        resetButton = UiFactory.secondaryButton("重置", "refresh");
        filters.add(searchButton);
        filters.add(resetButton);
        header.add(filters, BorderLayout.CENTER);

        feedback = new JLabel(" ");
        feedback.setForeground(UiTheme.TEXT);
        header.add(feedback, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        // 订单表格
        String[] columns = {"订单号", "用户 UUID", "商品编号", "数量", "金额", "状态", "交易时间"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        orderTable = new JTable(tableModel);
        orderTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        orderTable.setRowHeight(30);
        orderTable.setFillsViewportHeight(true);
        orderTable.getTableHeader().setReorderingAllowed(false);
        UiFactory.styleTable(orderTable);
        setColumnWidths();

        JScrollPane scrollPane = new JScrollPane(orderTable);
        scrollPane.setPreferredSize(new Dimension(900, 400));
        add(scrollPane, BorderLayout.CENTER);

        // 底部分页栏
        JPanel paginationPanel = new JPanel(new BorderLayout());
        paginationPanel.setOpaque(false);
        totalLabel = new JLabel("共 0 条流水");
        totalLabel.setForeground(UiTheme.MUTED);
        paginationPanel.add(totalLabel, BorderLayout.WEST);
        JPanel pageActions = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        pageActions.setOpaque(false);

        prevButton = UiFactory.secondaryButton("上一页", null);
        prevButton.setEnabled(false);
        pageActions.add(prevButton);

        pageLabel = new JLabel("第 1 页 / 共 1 页");
        pageActions.add(pageLabel);

        nextButton = UiFactory.secondaryButton("下一页", null);
        nextButton.setEnabled(false);
        pageActions.add(nextButton);
        paginationPanel.add(pageActions, BorderLayout.CENTER);

        add(paginationPanel, BorderLayout.SOUTH);

        // 事件监听
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadOrders();
            }
        });

        searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                currentPage = 1;
                loadOrders();
            }
        });
        userFilter.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                currentPage = 1;
                loadOrders();
            }
        });
        resetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                userFilter.setText("");
                statusFilter.setSelectedIndex(0);
                currentPage = 1;
                loadOrders();
            }
        });

        advanceButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                advanceSelectedOrder();
            }
        });

        prevButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentPage > 1) {
                    currentPage--;
                    loadOrders();
                }
            }
        });

        nextButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentPage < totalPages) {
                    currentPage++;
                    loadOrders();
                }
            }
        });

        orderTable.getSelectionModel().addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            @Override
            public void valueChanged(javax.swing.event.ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    updateButtonStates();
                }
            }
        });

        // 初始加载
        loadOrders();
    }

    /**
     * 加载所有订单。
     */
    private void loadOrders() {
        if (busy || api == null) {
            return;
        }
        setBusy(true, "正在加载订单...");

        UiTasks.run(new UiTasks.Task<OrderListResponse>() {
            @Override
            public OrderListResponse run() throws ApiException {
                OrderQuery query = new OrderQuery(currentPage, PAGE_SIZE,
                        selectedStatus(), normalizedUserFilter());
                return api.queryAllOrders(query);
            }
        }, new UiTasks.Success<OrderListResponse>() {
            @Override
            public void accept(OrderListResponse response) {
                displayOrders(response);
                setBusy(false, "订单加载成功");
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
     * 显示订单列表。
     *
     * @param response 订单列表响应
     */
    private void displayOrders(OrderListResponse response) {
        tableModel.setRowCount(0);

        if (response.getOrders() != null) {
            for (ShopOrder order : response.getOrders()) {
                Object[] row = new Object[7];
                row[0] = order.getoId();
                row[1] = order.getoUserUuid();
                row[2] = order.getoItemId();
                row[3] = order.getoQuantity();
                row[4] = "¥" + order.getoTotal();
                row[5] = order.getoStatus() == null ? "" : order.getoStatus().getDisplayName();
                row[6] = order.getoTime() != null ? dateFormat.format(order.getoTime()) : "";
                tableModel.addRow(row);
            }
        }

        currentPage = response.getPageNumber();
        totalPages = response.getTotalPages();
        pageLabel.setText(totalPages == 0 ? "第 0 页 / 共 0 页"
                : "第 " + currentPage + " 页 / 共 " + totalPages + " 页");
        totalLabel.setText("共 " + response.getTotalCount() + " 条流水");

        prevButton.setEnabled(currentPage > 1);
        nextButton.setEnabled(currentPage < totalPages);

        updateButtonStates();
    }

    /**
     * 推进选中的订单。
     */
    private void advanceSelectedOrder() {
        int selectedRow = orderTable.getSelectedRow();
        if (selectedRow < 0) {
            return;
        }

        final String orderId = (String) tableModel.getValueAt(selectedRow, 0);
        if (busy) {
            return;
        }
        setBusy(true, "正在推进订单...");

        UiTasks.run(new UiTasks.Task<Void>() {
            @Override
            public Void run() throws ApiException {
                api.advanceOrder(orderId);
                return null;
            }
        }, new UiTasks.Success<Void>() {
            @Override
            public void accept(Void result) {
                setBusy(false, "订单状态已推进！");
                feedback.setForeground(UiTheme.SUCCESS);
                loadOrders();
            }
        }, new UiTasks.Failure() {
            @Override
            public void accept(ApiException error) {
                setBusy(false, "推进失败: " + error.getMessage());
                feedback.setForeground(UiTheme.ACCENT);
            }
        });
    }

    /**
     * 更新按钮状态。
     */
    private void updateButtonStates() {
        if (readOnly) {
            advanceButton.setEnabled(false);
            return;
        }
        int selectedRow = orderTable.getSelectedRow();
        if (selectedRow < 0) {
            advanceButton.setEnabled(false);
            return;
        }

        String status = (String) tableModel.getValueAt(selectedRow, 5);
        // 只有未完成的订单可以推进
        advanceButton.setEnabled(!"已取消".equals(status) && !"已完成".equals(status));
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
        searchButton.setEnabled(!value);
        resetButton.setEnabled(!value);
        userFilter.setEnabled(!value);
        statusFilter.setEnabled(!value);
        advanceButton.setEnabled(!readOnly && !value && orderTable.getSelectedRow() >= 0);
        prevButton.setEnabled(!value && currentPage > 1);
        nextButton.setEnabled(!value && currentPage < totalPages);
        feedback.setText(message);
        feedback.setForeground(UiTheme.TEXT);
    }

    private String normalizedUserFilter() {
        String value = userFilter.getText();
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private ShopOrderStatus selectedStatus() {
        Object value = statusFilter.getSelectedItem();
        return value == null || ALL_STATUS.equals(value)
                ? null : ShopOrderStatus.fromDisplayName(String.valueOf(value));
    }

    private static String[] statusOptions() {
        ShopOrderStatus[] statuses = ShopOrderStatus.values();
        String[] options = new String[statuses.length + 1];
        options[0] = ALL_STATUS;
        for (int index = 0; index < statuses.length; index++) {
            options[index + 1] = statuses[index].getDisplayName();
        }
        return options;
    }

    private void setColumnWidths() {
        int[] widths = {170, 285, 90, 60, 100, 90, 165};
        for (int index = 0; index < widths.length; index++) {
            orderTable.getColumnModel().getColumn(index).setPreferredWidth(widths[index]);
        }
    }
}
