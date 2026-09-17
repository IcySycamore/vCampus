package edu.seu.vcampus.client.view.shop;

import edu.seu.vcampus.common.shop.dto.OrderListResponse;
import edu.seu.vcampus.common.shop.entity.ShopOrder;
import edu.seu.vcampus.common.shop.entity.ShopOrderStatus;
import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验证订单购物车的勾选、数量编辑和支付状态规则。 */
class ShopOrderPanelTest {

    /** 订单表首列为复选框，且只有待支付订单能进入合并结算。 */
    @Test
    void checkedUnpaidOrdersUpdateCheckoutTotalAndEnablePayment() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    ShopOrderPanel panel = new ShopOrderPanel(null);
                    display(panel, new OrderListResponse(Arrays.asList(
                            order("order-1", "12.30", ShopOrderStatus.UNPAID),
                            order("order-2", "7.70", ShopOrderStatus.UNPAID),
                            order("order-3", "99.00", ShopOrderStatus.PAID)),
                            1, 10, 3));

                    JTable table = field(panel, "orderTable", JTable.class);
                    JLabel total = field(panel, "selectedTotalLabel", JLabel.class);
                    JButton pay = field(panel, "payButton", JButton.class);
                    ShopOrderStatusSelector selector = field(panel,
                            "statusSelector", ShopOrderStatusSelector.class);

                    assertFalse(hasButton(panel, "取消订单"));
                    assertEquals(2, table.getRowCount());
                    assertEquals("选择", table.getColumnName(0));
                    assertEquals(Boolean.class, table.getColumnClass(0));
                    assertTrue(table.isCellEditable(0, 0));
                    assertTrue(table.isCellEditable(0, 3));
                    assertFalse(pay.isEnabled());

                    Component quantityEditor = table.getCellEditor(0, 3)
                            .getTableCellEditorComponent(table, Integer.valueOf(1),
                                    true, 0, 3);
                    assertTrue(quantityEditor instanceof JSpinner);
                    JSpinner spinner = (JSpinner) quantityEditor;
                    spinner.setValue(Integer.valueOf(2));
                    assertEquals(Integer.valueOf(2), spinner.getValue());
                    JFormattedTextField textField =
                            ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField();
                    textField.setText("3");
                    spinner.commitEdit();
                    assertEquals(Integer.valueOf(3), spinner.getValue());
                    spinner.setValue(Integer.valueOf(0));
                    assertEquals(Integer.valueOf(0), spinner.getValue());

                    table.setValueAt(Boolean.TRUE, 0, 0);
                    table.setValueAt(Boolean.TRUE, 1, 0);
                    assertEquals("待支付总金额：¥20.00", total.getText());
                    assertTrue(pay.isEnabled());

                    estimate(panel, "order-1", 3, 1, new BigDecimal("12.30"));
                    assertEquals("¥36.90", table.getValueAt(0, 4));
                    assertEquals("待支付总金额：¥44.60", total.getText());

                    table.setValueAt(Boolean.FALSE, 0, 0);
                    table.setValueAt(Boolean.FALSE, 1, 0);
                    assertEquals("待支付总金额：¥0.00", total.getText());
                    assertFalse(pay.isEnabled());

                    ShopOrder removed = order("order-1", "0", ShopOrderStatus.UNPAID);
                    removed.setoQuantity(Integer.valueOf(0));
                    apply(panel, removed);
                    assertEquals(1, table.getRowCount());

                    JToggleButton paidButton = selector.getPaidButton();
                    paidButton.doClick();
                    assertEquals(0, table.getRowCount());
                    display(panel, new OrderListResponse(Arrays.asList(
                            order("order-1", "12.30", ShopOrderStatus.UNPAID),
                            order("order-3", "99.00", ShopOrderStatus.PAID)),
                            1, 10, 2));
                    assertTrue(paidButton.isSelected());
                    assertEquals(1, table.getRowCount());
                    assertEquals("order-3", table.getValueAt(0, 1));
                    assertEquals(0, table.getColumnModel().getColumn(0).getMaxWidth());
                    assertFalse(pay.isVisible());
                    assertFalse(total.isVisible());
                } catch (ParseException e) {
                    throw new AssertionError(e);
                } catch (Exception e) {
                    throw new AssertionError(e);
                }
            }
        });
    }

    /** 数量改为零并得到服务端确认后，待支付行应立即从表格移除。 */
    @Test
    void zeroQuantityUpdateRemovesRowFromVisibleOrders() throws Exception {
        final QuantityZeroService service = new QuantityZeroService();
        final ShopOrderPanel[] panels = new ShopOrderPanel[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                panels[0] = new ShopOrderPanel(service);
            }
        });
        assertTrue(service.loaded.await(2, TimeUnit.SECONDS));
        flushEdt();

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    display(panels[0], new OrderListResponse(Arrays.asList(
                            order("order-zero", "12.30", ShopOrderStatus.UNPAID)),
                            1, 10, 1));
                    JTable table = field(panels[0], "orderTable", JTable.class);
                    assertTrue(table.editCellAt(0, 3));
                    JSpinner spinner = (JSpinner) table.getEditorComponent();
                    JFormattedTextField input =
                            ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField();
                    input.setText("0");
                } catch (Exception e) {
                    throw new AssertionError(e);
                }
            }
        });

        assertTrue(service.updated.await(2, TimeUnit.SECONDS));
        waitForRowCount(panels[0], 0);
    }

    /** 即使收到畸形响应，订单窗口也必须解除加载状态并允许重试。 */
    @Test
    void malformedOrderResponseDoesNotLeavePanelBusy() throws Exception {
        final ShopOrderPanel[] panels = new ShopOrderPanel[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                panels[0] = new ShopOrderPanel(new MalformedOrderService());
            }
        });

        waitForFeedback(panels[0], "订单数据异常，请刷新重试");
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    assertTrue(field(panels[0], "refreshButton", JButton.class).isEnabled());
                    assertTrue(field(panels[0], "orderTable", JTable.class).isEnabled());
                } catch (Exception error) {
                    throw new AssertionError(error);
                }
            }
        });
    }

    private static final class QuantityZeroService
            extends edu.seu.vcampus.client.shop.ShopService {

        private final CountDownLatch loaded = new CountDownLatch(1);
        private final CountDownLatch updated = new CountDownLatch(1);

        QuantityZeroService() {
            super(null, null);
        }

        @Override
        public OrderListResponse listMyOrders(
                edu.seu.vcampus.common.shop.dto.OrderQuery query) {
            loaded.countDown();
            return new OrderListResponse(java.util.Collections.<ShopOrder>emptyList(),
                    1, 10, 0);
        }

        @Override
        public ShopOrder updateOrderQuantity(String orderId, int quantity) {
            ShopOrder removed = order(orderId, "0", ShopOrderStatus.UNPAID);
            removed.setoQuantity(Integer.valueOf(0));
            updated.countDown();
            return removed;
        }
    }

    private static final class MalformedOrderService
            extends edu.seu.vcampus.client.shop.ShopService {

        MalformedOrderService() {
            super(null, null);
        }

        @Override
        public OrderListResponse listMyOrders(
                edu.seu.vcampus.common.shop.dto.OrderQuery query) {
            return null;
        }
    }

    private static void waitForRowCount(final ShopOrderPanel panel,
            final int expected) throws Exception {
        long deadline = System.currentTimeMillis() + 2000L;
        while (System.currentTimeMillis() < deadline) {
            final int[] count = new int[1];
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    try {
                        count[0] = field(panel, "orderTable", JTable.class).getRowCount();
                    } catch (Exception e) {
                        throw new AssertionError(e);
                    }
                }
            });
            if (count[0] == expected) {
                return;
            }
            Thread.sleep(10L);
        }
        assertEquals(expected, field(panel, "orderTable", JTable.class).getRowCount());
    }

    private static void waitForFeedback(final ShopOrderPanel panel,
            String expected) throws Exception {
        long deadline = System.currentTimeMillis() + 2000L;
        while (System.currentTimeMillis() < deadline) {
            flushEdt();
            if (expected.equals(field(panel, "feedback", JLabel.class).getText())) {
                return;
            }
            Thread.sleep(10L);
        }
        assertEquals(expected, field(panel, "feedback", JLabel.class).getText());
    }

    private static void flushEdt() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                // Flush pending UI callbacks.
            }
        });
    }

    private static ShopOrder order(String id, String total, ShopOrderStatus status) {
        ShopOrder order = new ShopOrder();
        order.setoId(id);
        order.setoItemId("item-" + id);
        order.setoQuantity(Integer.valueOf(1));
        order.setoTotal(new BigDecimal(total));
        order.setoTime(new Date());
        order.setoStatus(status);
        return order;
    }

    private static void display(ShopOrderPanel panel, OrderListResponse response)
            throws Exception {
        Method method = ShopOrderPanel.class.getDeclaredMethod(
                "displayOrders", OrderListResponse.class);
        method.setAccessible(true);
        method.invoke(panel, response);
    }

    private static void apply(ShopOrderPanel panel, ShopOrder order) throws Exception {
        Method method = ShopOrderPanel.class.getDeclaredMethod(
                "applyOrderValues", ShopOrder.class);
        method.setAccessible(true);
        method.invoke(panel, order);
    }

    private static void estimate(ShopOrderPanel panel, String orderId, int quantity,
            int previousQuantity, BigDecimal previousTotal) throws Exception {
        Method method = ShopOrderPanel.class.getDeclaredMethod("applyEstimatedTotal",
                String.class, Integer.TYPE, Integer.class, BigDecimal.class);
        method.setAccessible(true);
        method.invoke(panel, orderId, Integer.valueOf(quantity),
                Integer.valueOf(previousQuantity), previousTotal);
    }

    private static <T> T field(ShopOrderPanel panel, String name, Class<T> type)
            throws Exception {
        Field field = ShopOrderPanel.class.getDeclaredField(name);
        field.setAccessible(true);
        return type.cast(field.get(panel));
    }

    private static boolean hasButton(Container parent, String text) {
        for (Component component : parent.getComponents()) {
            if (component instanceof JButton
                    && text.equals(((JButton) component).getText())) {
                return true;
            }
            if (component instanceof Container
                    && hasButton((Container) component, text)) {
                return true;
            }
        }
        return false;
    }
}
