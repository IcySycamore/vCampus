package edu.seu.vcampus.client.view.shop;

import edu.seu.vcampus.common.shop.entity.ShopOrderStatus;
import java.util.ArrayList;
import java.util.List;
import javax.swing.table.DefaultTableModel;

/** “我的订单”表格模型。 */
final class ShopOrderTableModel extends DefaultTableModel {

    private static final long serialVersionUID = 1L;

    static final int COLUMN_SELECTED = 0;
    static final int COLUMN_ORDER_ID = 1;
    static final int COLUMN_QUANTITY = 3;
    static final int COLUMN_TOTAL = 4;
    static final int COLUMN_STATUS = 5;

    private static final String[] COLUMNS = {
        "选择", "订单ID", "商品名称", "数量", "总价", "状态", "下单时间"
    };

    ShopOrderTableModel() {
        super(COLUMNS, 0);
    }

    @Override
    public Class<?> getColumnClass(int column) {
        return column == COLUMN_SELECTED ? Boolean.class : Object.class;
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        if (column != COLUMN_SELECTED && column != COLUMN_QUANTITY) {
            return false;
        }
        Object value = getValueAt(row, COLUMN_STATUS);
        return value instanceof String && ShopOrderStatus.UNPAID
                == ShopOrderStatus.fromDisplayName((String) value);
    }

    List<String> selectedUnpaidOrderIds() {
        List<String> result = new ArrayList<String>();
        for (int row = 0; row < getRowCount(); row++) {
            if (Boolean.TRUE.equals(getValueAt(row, COLUMN_SELECTED))
                    && statusAt(row) == ShopOrderStatus.UNPAID) {
                result.add((String) getValueAt(row, COLUMN_ORDER_ID));
            }
        }
        return result;
    }

    void removeOrders(List<String> orderIds) {
        for (int row = getRowCount() - 1; row >= 0; row--) {
            if (orderIds.contains(getValueAt(row, COLUMN_ORDER_ID))) {
                removeRow(row);
            }
        }
    }

    private ShopOrderStatus statusAt(int row) {
        Object value = getValueAt(row, COLUMN_STATUS);
        return value instanceof String
                ? ShopOrderStatus.fromDisplayName((String) value) : null;
    }
}
