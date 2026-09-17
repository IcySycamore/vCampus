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
import java.math.BigDecimal;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

/**
 * 商店管理员面板（商品管理）。
 */
public class ShopAdminPanel extends JPanel {

    private final ShopService api;
    private final JLabel feedback;
    private final JButton refreshButton;
    private final JButton addButton;
    private final JButton editButton;
    private final JTable itemTable;
    private final DefaultTableModel tableModel;
    private boolean busy;

    /**
     * 创建管理员面板。
     *
     * @param api 商店服务
     */
    public ShopAdminPanel(ShopService api) {
        this.api = api;
        this.busy = false;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // 顶部工具栏
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        JLabel title = new JLabel("商品管理");
        title.setFont(UiTheme.font(java.awt.Font.BOLD, 20));
        toolbar.add(title);

        refreshButton = UiFactory.primaryButton("刷新", null);
        toolbar.add(refreshButton);

        addButton = UiFactory.primaryButton("添加商品", null);
        toolbar.add(addButton);

        editButton = UiFactory.secondaryButton("编辑商品", null);
        editButton.setEnabled(false);
        toolbar.add(editButton);

        feedback = new JLabel(" ");
        feedback.setForeground(UiTheme.TEXT);
        toolbar.add(feedback);

        add(toolbar, BorderLayout.NORTH);

        // 商品表格
        String[] columns = {"商品ID", "商品名称", "价格", "库存", "描述"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        itemTable = new JTable(tableModel);
        itemTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        itemTable.setRowHeight(30);
        UiFactory.styleTable(itemTable);

        JScrollPane scrollPane = new JScrollPane(itemTable);
        scrollPane.setPreferredSize(new Dimension(800, 500));
        add(scrollPane, BorderLayout.CENTER);

        // 事件监听
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadItems();
            }
        });

        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showItemDialog(null);
            }
        });

        editButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                editSelectedItem();
            }
        });

        itemTable.getSelectionModel().addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            @Override
            public void valueChanged(javax.swing.event.ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    editButton.setEnabled(itemTable.getSelectedRow() >= 0);
                }
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
        tableModel.setRowCount(0);

        if (items != null) {
            for (ShopItem item : items) {
                Object[] row = new Object[5];
                row[0] = item.getSiId();
                row[1] = item.getSiName();
                row[2] = "¥" + item.getSiPrice();
                row[3] = item.getSiStock();
                row[4] = item.getSiDesc();
                tableModel.addRow(row);
            }
        }
    }

    /**
     * 编辑选中的商品。
     */
    private void editSelectedItem() {
        int selectedRow = itemTable.getSelectedRow();
        if (selectedRow < 0) {
            return;
        }

        final String itemId = (String) tableModel.getValueAt(selectedRow, 0);
        if (busy) {
            return;
        }
        setBusy(true, "正在加载商品详情...");

        UiTasks.run(new UiTasks.Task<ShopItem>() {
            @Override
            public ShopItem run() throws ApiException {
                return api.getItemDetail(itemId);
            }
        }, new UiTasks.Success<ShopItem>() {
            @Override
            public void accept(ShopItem item) {
                setBusy(false, "");
                showItemDialog(item);
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
     * 显示商品编辑对话框。
     *
     * @param item 要编辑的商品（null表示新建）
     */
    private void showItemDialog(final ShopItem item) {
        JPanel panel = new JPanel(new GridLayout(5, 2, 10, 10));

        JTextField nameField = new JTextField(item != null ? item.getSiName() : "");
        JTextField priceField = new JTextField(item != null ? item.getSiPrice().toString() : "0.00");
        JTextField stockField = new JTextField(item != null ? String.valueOf(item.getSiStock()) : "0");
        JTextField descField = new JTextField(item != null ? item.getSiDesc() : "");

        panel.add(new JLabel("商品ID:"));
        panel.add(new JLabel(item != null ? item.getSiId() : "自动生成"));
        panel.add(new JLabel("商品名称:"));
        panel.add(nameField);
        panel.add(new JLabel("价格:"));
        panel.add(priceField);
        panel.add(new JLabel("库存:"));
        panel.add(stockField);
        panel.add(new JLabel("描述:"));
        panel.add(descField);

        int result = JOptionPane.showConfirmDialog(this, panel,
                item != null ? "编辑商品" : "添加商品",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            try {
                String name = nameField.getText().trim();
                BigDecimal price = new BigDecimal(priceField.getText().trim());
                int stock = Integer.parseInt(stockField.getText().trim());
                String desc = descField.getText().trim();

                if (name.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "商品名称不能为空", "错误", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                ShopItem newItem = new ShopItem();
                if (item != null) {
                    newItem.setSiId(item.getSiId());
                }
                newItem.setSiName(name);
                newItem.setSiPrice(price);
                newItem.setSiStock(stock);
                newItem.setSiDesc(desc);

                saveItem(newItem);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "价格或库存格式错误", "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * 保存商品。
     *
     * @param item 商品
     */
    private void saveItem(final ShopItem item) {
        if (busy) {
            return;
        }
        setBusy(true, "正在保存商品...");

        UiTasks.run(new UiTasks.Task<ShopItem>() {
            @Override
            public ShopItem run() throws ApiException {
                return api.upsertItem(item);
            }
        }, new UiTasks.Success<ShopItem>() {
            @Override
            public void accept(ShopItem result) {
                setBusy(false, "商品保存成功！");
                feedback.setForeground(UiTheme.SUCCESS);
                loadItems();
            }
        }, new UiTasks.Failure() {
            @Override
            public void accept(ApiException error) {
                setBusy(false, "保存失败: " + error.getMessage());
                feedback.setForeground(UiTheme.ACCENT);
            }
        });
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
        addButton.setEnabled(!value);
        editButton.setEnabled(!value && itemTable.getSelectedRow() >= 0);
        feedback.setText(message);
        feedback.setForeground(UiTheme.TEXT);
    }
}
