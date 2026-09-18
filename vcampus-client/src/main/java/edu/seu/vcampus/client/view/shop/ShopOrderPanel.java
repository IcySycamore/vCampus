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
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableColumn;

/**
 * 订单列表面板。
 */
public class ShopOrderPanel extends JPanel {

    private static final int COLUMN_SELECTED = ShopOrderTableModel.COLUMN_SELECTED;
    private static final int COLUMN_ORDER_ID = ShopOrderTableModel.COLUMN_ORDER_ID;
    private static final int COLUMN_QUANTITY = ShopOrderTableModel.COLUMN_QUANTITY;
    private static final int COLUMN_TOTAL = ShopOrderTableModel.COLUMN_TOTAL;
    private static final int COLUMN_STATUS = ShopOrderTableModel.COLUMN_STATUS;

    private final ShopService api;
    private final Runnable paymentSuccess;
    private final JLabel feedback;
    private final JButton refreshButton;
    private final JButton payButton;
    private final ShopOrderStatusSelector statusSelector;
    private final JButton prevButton;
    private final JButton nextButton;
    private final JLabel pageLabel;
    private final JLabel selectedTotalLabel;
    private final JTable orderTable;
    private final ShopOrderTableModel tableModel;
    private final Map<String, BigDecimal> orderTotals =
            new HashMap<String, BigDecimal>();
    private final Map<String, Integer> orderQuantities =
            new HashMap<String, Integer>();
    private boolean busy;
    private boolean updatingTable;
    private ShopOrderStatus displayedStatus = ShopOrderStatus.UNPAID;
    private int currentPage = 1;
    private long totalPages = 1;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * 创建订单面板。
     *
     * @param api 商店服务
     */
    public ShopOrderPanel(ShopService api) {
        this(api, null);
    }
    ShopOrderPanel(ShopService api, Runnable paymentSuccess) {
        this.api = api;
        this.paymentSuccess = paymentSuccess;
        this.busy = false;
        this.updatingTable = false;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        JLabel title = new JLabel("我的订单");
        title.setFont(UiTheme.font(java.awt.Font.BOLD, 20));
        toolbar.add(title);
        statusSelector = new ShopOrderStatusSelector(
                new ShopOrderStatusSelector.Listener() {
                    @Override
                    public void onStatusSelected(ShopOrderStatus status) {
                        switchOrderStatus(status);
                    }
                });
        toolbar.add(statusSelector);
        refreshButton = UiFactory.primaryButton("刷新", null);
        toolbar.add(refreshButton);
        payButton = UiFactory.primaryButton("支付", null);
        payButton.setEnabled(false);
        toolbar.add(payButton);
        feedback = new JLabel(" ");
        feedback.setForeground(UiTheme.TEXT);
        toolbar.add(feedback);
        add(toolbar, BorderLayout.NORTH);
        tableModel = new ShopOrderTableModel();
        orderTable = new JTable(tableModel);
        orderTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        orderTable.setRowHeight(30);
        orderTable.getColumnModel().getColumn(COLUMN_SELECTED).setMinWidth(48);
        orderTable.getColumnModel().getColumn(COLUMN_SELECTED).setMaxWidth(48);
        ShopQuantitySpinnerCell quantityCell = new ShopQuantitySpinnerCell();
        orderTable.getColumnModel().getColumn(COLUMN_QUANTITY).setCellRenderer(quantityCell);
        orderTable.getColumnModel().getColumn(COLUMN_QUANTITY).setCellEditor(quantityCell);
        orderTable.getColumnModel().getColumn(COLUMN_QUANTITY).setMinWidth(82);
        orderTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        UiFactory.styleTable(orderTable);
        JScrollPane scrollPane = new JScrollPane(orderTable);
        scrollPane.setPreferredSize(new Dimension(800, 400));
        add(scrollPane, BorderLayout.CENTER);
        JPanel bottomPanel = new JPanel(new BorderLayout());
        selectedTotalLabel = new JLabel("待支付总金额：¥0.00");
        selectedTotalLabel.setFont(UiTheme.font(java.awt.Font.BOLD, 16));
        selectedTotalLabel.setForeground(UiTheme.TEXT);
        bottomPanel.add(selectedTotalLabel, BorderLayout.WEST);
        JPanel paginationPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        prevButton = UiFactory.secondaryButton("上一页", null);
        prevButton.setEnabled(false);
        paginationPanel.add(prevButton);
        pageLabel = new JLabel("第 1 页 / 共 1 页");
        paginationPanel.add(pageLabel);
        nextButton = UiFactory.secondaryButton("下一页", null);
        nextButton.setEnabled(false);
        paginationPanel.add(nextButton);
        bottomPanel.add(paginationPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
        updateOrderView();
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadOrders();
            }
        });
        payButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                paySelectedOrder();
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
        tableModel.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent event) {
                if (!updatingTable && event.getType() == TableModelEvent.UPDATE
                        && event.getColumn() == COLUMN_QUANTITY
                        && event.getFirstRow() == event.getLastRow()) {
                    updateOrderQuantity(event.getFirstRow());
                }
                updateSelectedTotal();
                updateButtonStates();
            }
        });
        loadOrders();
    }

    private void loadOrders() {
        if (busy || api == null) {
            return;
        }
        setBusy(true, "正在加载订单...");
        UiTasks.run(new UiTasks.Task<OrderListResponse>() {
            @Override
            public OrderListResponse run() throws ApiException {
                OrderQuery query = new OrderQuery(
                        currentPage, 10, displayedStatus, null);
                return api.listMyOrders(query);
            }
        }, new UiTasks.Success<OrderListResponse>() {
            @Override
            public void accept(OrderListResponse response) {
                try {
                    displayOrders(response);
                    setBusy(false, "订单加载成功");
                } catch (RuntimeException error) {
                    setBusy(false, "订单数据异常，请刷新重试");
                    feedback.setForeground(UiTheme.ACCENT);
                }
            }
        }, new UiTasks.Failure() {
            @Override
            public void accept(ApiException error) {
                setBusy(false, "加载失败: " + error.getMessage());
                feedback.setForeground(UiTheme.ACCENT);
            }
        });
    }
    private void displayOrders(OrderListResponse response) {
        tableModel.setRowCount(0);
        orderTotals.clear();
        orderQuantities.clear();
        if (response.getOrders() != null) {
            for (ShopOrder order : response.getOrders()) {
                if (order.getoStatus() != displayedStatus) {
                    continue;
                }
                Object[] row = new Object[7];
                row[0] = Boolean.FALSE;
                row[1] = order.getoId();
                row[2] = order.getoItemId(); // 显示商品ID，因为ShopOrder没有商品名称字段
                row[3] = order.getoQuantity();
                row[4] = "¥" + order.getoTotal(); // 使用总价
                row[5] = order.getoStatus().getDisplayName();
                row[6] = order.getoTime() != null ? dateFormat.format(order.getoTime()) : "";
                orderTotals.put(order.getoId(), order.getoTotal());
                orderQuantities.put(order.getoId(), order.getoQuantity());
                tableModel.addRow(row);
            }
        }

        currentPage = response.getPageNumber();
        totalPages = Math.max(1L, response.getTotalPages());
        pageLabel.setText("第 " + currentPage + " 页 / 共 " + totalPages + " 页");

        prevButton.setEnabled(currentPage > 1);
        nextButton.setEnabled(currentPage < totalPages);

        updateButtonStates();
        updateSelectedTotal();
    }
    /** 将数量编辑提交到服务端，并使用服务端返回值刷新数量和总价。 */
    private void updateOrderQuantity(final int row) {
        if (row < 0 || row >= tableModel.getRowCount()) {
            return;
        }
        final String orderId = (String) tableModel.getValueAt(row, COLUMN_ORDER_ID);
        final Integer previousQuantity = orderQuantities.get(orderId);
        final BigDecimal previousTotal = orderTotals.get(orderId);
        Object value = tableModel.getValueAt(row, COLUMN_QUANTITY);
        if (!(value instanceof Number) || ((Number) value).intValue() < 0) {
            restoreOrderValues(orderId, previousQuantity, previousTotal);
            return;
        }
        final int quantity = ((Number) value).intValue();
        if (previousQuantity != null && previousQuantity.intValue() == quantity) {
            return;
        }
        if (busy || api == null) {
            restoreOrderValues(orderId, previousQuantity, previousTotal);
            return;
        }
        applyEstimatedTotal(orderId, quantity, previousQuantity, previousTotal);
        setBusy(true, "正在更新商品数量...");

        UiTasks.run(new UiTasks.Task<ShopOrder>() {
            @Override
            public ShopOrder run() throws ApiException {
                return api.updateOrderQuantity(orderId, quantity);
            }
        }, new UiTasks.Success<ShopOrder>() {
            @Override
            public void accept(ShopOrder order) {
                boolean removed = order != null && order.getoQuantity() != null
                        && order.getoQuantity().intValue() == 0;
                applyOrderValues(order);
                setBusy(false, removed ? "订单已移除" : "商品数量已更新");
                feedback.setForeground(UiTheme.SUCCESS);
            }
        }, new UiTasks.Failure() {
            @Override
            public void accept(ApiException error) {
                restoreOrderValues(orderId, previousQuantity, previousTotal);
                setBusy(false, "数量修改失败: " + error.getMessage());
                feedback.setForeground(UiTheme.ACCENT);
            }
        });
    }
    private void applyEstimatedTotal(String orderId, int quantity,
            Integer previousQuantity, BigDecimal previousTotal) {
        BigDecimal estimated = previousQuantity == null ? null
                : ShopOrderAmounts.estimate(previousTotal,
                        previousQuantity.intValue(), quantity);
        if (estimated == null) {
            return;
        }
        orderTotals.put(orderId, estimated);
        int row = findOrderRow(orderId);
        if (row >= 0) {
            updatingTable = true;
            try {
                tableModel.setValueAt("¥" + estimated.toPlainString(), row, COLUMN_TOTAL);
            } finally {
                updatingTable = false;
            }
        }
        updateSelectedTotal();
    }
    private void applyOrderValues(ShopOrder order) {
        if (order == null) {
            return;
        }
        int row = findOrderRow(order.getoId());
        if (order.getoQuantity() != null && order.getoQuantity().intValue() == 0) {
            orderQuantities.remove(order.getoId());
            orderTotals.remove(order.getoId());
            if (row >= 0) {
                updatingTable = true;
                try {
                    tableModel.removeRow(row);
                } finally {
                    updatingTable = false;
                }
            }
            updateSelectedTotal();
            return;
        }
        orderQuantities.put(order.getoId(), order.getoQuantity());
        orderTotals.put(order.getoId(), order.getoTotal());
        if (row >= 0) {
            updatingTable = true;
            try {
                tableModel.setValueAt(order.getoQuantity(), row, COLUMN_QUANTITY);
                tableModel.setValueAt("¥" + order.getoTotal(), row, COLUMN_TOTAL);
            } finally {
                updatingTable = false;
            }
        }
        updateSelectedTotal();
    }
    private void restoreOrderValues(String orderId, Integer quantity, BigDecimal total) {
        int row = findOrderRow(orderId);
        if (row < 0) {
            return;
        }
        updatingTable = true;
        try {
            if (quantity != null) {
                orderQuantities.put(orderId, quantity);
                tableModel.setValueAt(quantity, row, COLUMN_QUANTITY);
            }
            if (total != null) {
                orderTotals.put(orderId, total);
                tableModel.setValueAt("¥" + total, row, COLUMN_TOTAL);
            }
        } finally {
            updatingTable = false;
        }
        updateSelectedTotal();
    }

    private int findOrderRow(String orderId) {
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            if (orderId.equals(tableModel.getValueAt(row, COLUMN_ORDER_ID))) {
                return row;
            }
        }
        return -1;
    }

    private void paySelectedOrder() {
        if (busy) {
            return;
        }
        final List<String> orderIds = selectedOrderIds();
        if (orderIds.isEmpty()) {
            return;
        }
        final char[] bankPassword = ShopPaymentDialog.request(
                this, selectedTotalLabel.getText());
        if (bankPassword == null) {
            return;
        }
        if (bankPassword.length == 0) {
            Arrays.fill(bankPassword, '\0');
            feedback.setText("请输入银行密码");
            feedback.setForeground(UiTheme.ACCENT);
            return;
        }
        setBusy(true, "正在支付订单...");

        UiTasks.run(new UiTasks.Task<Void>() {
            @Override
            public Void run() throws ApiException {
                try {
                    api.payOrders(orderIds, bankPassword);
                    return null;
                } finally {
                    Arrays.fill(bankPassword, '\0');
                }
            }
        }, new UiTasks.Success<Void>() {
            @Override
            public void accept(Void result) {
                removeOrders(orderIds);
                setBusy(false, "支付成功！");
                feedback.setForeground(UiTheme.SUCCESS);
                if (paymentSuccess != null) {
                    paymentSuccess.run();
                }
                loadOrders();
            }
        }, new UiTasks.Failure() {
            @Override
            public void accept(ApiException error) {
                setBusy(false, "支付失败: " + error.getMessage());
                feedback.setForeground(UiTheme.ACCENT);
            }
        });
    }

    private void updateButtonStates() {
        payButton.setEnabled(!busy && displayedStatus == ShopOrderStatus.UNPAID
                && !selectedOrderIds().isEmpty());
    }

    private void removeOrders(List<String> orderIds) {
        updatingTable = true;
        try {
            tableModel.removeOrders(orderIds);
            for (String orderId : orderIds) {
                orderTotals.remove(orderId);
                orderQuantities.remove(orderId);
            }
        } finally {
            updatingTable = false;
        }
        updateSelectedTotal();
    }

    private void switchOrderStatus(ShopOrderStatus status) {
        if (busy || status == displayedStatus) {
            return;
        }
        displayedStatus = status;
        currentPage = 1;
        tableModel.setRowCount(0);
        orderTotals.clear();
        orderQuantities.clear();
        updateSelectedTotal();
        updateOrderView();
        loadOrders();
    }
    private void updateOrderView() {
        boolean unpaid = displayedStatus == ShopOrderStatus.UNPAID;
        TableColumn selected = orderTable.getColumnModel().getColumn(COLUMN_SELECTED);
        selected.setMinWidth(unpaid ? 48 : 0);
        selected.setMaxWidth(unpaid ? 48 : 0);
        selected.setPreferredWidth(unpaid ? 48 : 0);
        payButton.setVisible(unpaid);
        selectedTotalLabel.setVisible(unpaid);
        updateButtonStates();
        revalidate();
        repaint();
    }
    private List<String> selectedOrderIds() {
        return tableModel.selectedUnpaidOrderIds();
    }
    private void updateSelectedTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (String orderId : selectedOrderIds()) {
            BigDecimal amount = orderTotals.get(orderId);
            if (amount != null) {
                total = total.add(amount);
            }
        }
        selectedTotalLabel.setText("待支付总金额：¥"
                + total.setScale(2).toPlainString());
    }
    private void setBusy(boolean value, String message) {
        busy = value;
        refreshButton.setEnabled(!value);
        statusSelector.setControlsEnabled(!value);
        orderTable.setEnabled(!value);
        if (value) {
            payButton.setEnabled(false);
        } else {
            updateButtonStates();
        }
        prevButton.setEnabled(!value && currentPage > 1);
        nextButton.setEnabled(!value && currentPage < totalPages);
        feedback.setText(message);
        feedback.setForeground(UiTheme.TEXT);
    }
}
