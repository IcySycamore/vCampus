package edu.seu.vcampus.client.view.shop;

import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.client.shop.ShopService;
import edu.seu.vcampus.client.view.UiTasks;
import edu.seu.vcampus.client.view.theme.UiFactory;
import edu.seu.vcampus.client.view.theme.UiTheme;
import edu.seu.vcampus.common.shop.dto.OrderListResponse;
import edu.seu.vcampus.common.shop.dto.OrderQuery;
import edu.seu.vcampus.common.shop.entity.ShopOrder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

/**
 * 管理员订单管理面板。
 */
public class ShopAdminOrderPanel extends JPanel {

    private final ShopService api;
    private final JLabel feedback;
    private final JButton refreshButton;
    private final JButton advanceButton;
    private final JButton prevButton;
    private final JButton nextButton;
    private final JLabel pageLabel;
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
        this.api = api;
        this.busy = false;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // 顶部工具栏
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        JLabel title = new JLabel("订单管理");
        title.setFont(UiTheme.font(java.awt.Font.BOLD, 20));
        toolbar.add(title);

        refreshButton = UiFactory.primaryButton("刷新", null);
        toolbar.add(refreshButton);

        advanceButton = UiFactory.primaryButton("推进订单", null);
        advanceButton.setEnabled(false);
        toolbar.add(advanceButton);

        feedback = new JLabel(" ");
        feedback.setForeground(UiTheme.TEXT);
        toolbar.add(feedback);

        add(toolbar, BorderLayout.NORTH);

        // 订单表格
        String[] columns = {"订单ID", "用户", "商品ID", "数量", "总价", "状态", "下单时间"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        orderTable = new JTable(tableModel);
        orderTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        orderTable.setRowHeight(30);
        UiFactory.styleTable(orderTable);

        JScrollPane scrollPane = new JScrollPane(orderTable);
        scrollPane.setPreferredSize(new Dimension(900, 400));
        add(scrollPane, BorderLayout.CENTER);

        // 底部分页栏
        JPanel paginationPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));

        prevButton = UiFactory.secondaryButton("上一页", null);
        prevButton.setEnabled(false);
        paginationPanel.add(prevButton);

        pageLabel = new JLabel("第 1 页 / 共 1 页");
        paginationPanel.add(pageLabel);

        nextButton = UiFactory.secondaryButton("下一页", null);
        nextButton.setEnabled(false);
        paginationPanel.add(nextButton);

        add(paginationPanel, BorderLayout.SOUTH);

        // 事件监听
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
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
                OrderQuery query = new OrderQuery(currentPage, 10);
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
                row[5] = order.getoStatus().getDisplayName();
                row[6] = order.getoTime() != null ? dateFormat.format(order.getoTime()) : "";
                tableModel.addRow(row);
            }
        }

        currentPage = response.getPageNumber();
        totalPages = response.getTotalPages();
        pageLabel.setText("第 " + currentPage + " 页 / 共 " + totalPages + " 页");

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
        advanceButton.setEnabled(!value && orderTable.getSelectedRow() >= 0);
        prevButton.setEnabled(!value && currentPage > 1);
        nextButton.setEnabled(!value && currentPage < totalPages);
        feedback.setText(message);
        feedback.setForeground(UiTheme.TEXT);
    }
}
