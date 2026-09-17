package edu.seu.vcampus.client.view.shop;

import edu.seu.vcampus.client.shop.ShopService;
import edu.seu.vcampus.common.shop.dto.OrderListResponse;
import edu.seu.vcampus.common.shop.dto.OrderQuery;
import edu.seu.vcampus.common.shop.entity.ShopItem;
import edu.seu.vcampus.common.shop.entity.ShopOrder;
import java.awt.Frame;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 验证商店主界面只复用一个“我的订单”窗口。 */
class ShopPanelTest {

    /** 已有订单窗口时应将其置前，不得再创建新窗口。 */
    @Test
    void existingOrderWindowIsReused() throws Exception {
        ShopPanel panel = new ShopPanel(null);
        JFrame frame = mock(JFrame.class);
        when(frame.isDisplayable()).thenReturn(true);
        Field field = ShopPanel.class.getDeclaredField("orderFrame");
        field.setAccessible(true);
        field.set(panel, frame);

        Method method = ShopPanel.class.getDeclaredMethod("showMyOrders");
        method.setAccessible(true);
        method.invoke(panel);

        verify(frame).setState(Frame.NORMAL);
        verify(frame).setVisible(true);
        verify(frame).toFront();
        verify(frame).requestFocus();
    }

    /** 支付成功应重新加载库存，并继续通知主界面刷新银行数据。 */
    @Test
    void paymentSuccessRefreshesItemsAndNotifiesExternalPage() throws Exception {
        final ShopService api = mock(ShopService.class);
        when(api.listItems()).thenReturn(Collections.<ShopItem>emptyList());
        final AtomicInteger notifications = new AtomicInteger();
        final ShopPanel[] panel = new ShopPanel[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                panel[0] = new ShopPanel(api, new Runnable() {
                    @Override
                    public void run() {
                        notifications.incrementAndGet();
                    }
                });
                panel[0].paymentCompleted();
            }
        });

        verify(api, org.mockito.Mockito.timeout(2000).atLeast(2)).listItems();
        org.junit.jupiter.api.Assertions.assertEquals(1, notifications.get());
    }

    /** 管理员进入 Shop 时应看到全用户订单流水，而不是商品购买页。 */
    @Test
    void administratorSeesAllUserOrderLedger() throws Exception {
        final ShopService api = mock(ShopService.class);
        when(api.isAdministrator()).thenReturn(true);
        when(api.queryAllOrders(any(OrderQuery.class))).thenReturn(
                new OrderListResponse(Collections.<ShopOrder>emptyList(), 1, 20, 0));
        final ShopPanel[] panel = new ShopPanel[1];

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                panel[0] = new ShopPanel(api);
                panel[0].addNotify();
            }
        });

        org.junit.jupiter.api.Assertions.assertTrue(
                panel[0].getComponent(0) instanceof ShopAdminOrderPanel);
        Field initialLoad = ShopPanel.class.getDeclaredField("initialLoadScheduled");
        initialLoad.setAccessible(true);
        org.junit.jupiter.api.Assertions.assertFalse(initialLoad.getBoolean(panel[0]),
                "管理员视图显示时不应安排商品目录重试");
        verify(api, org.mockito.Mockito.timeout(2000)).queryAllOrders(any(OrderQuery.class));
        verify(api, org.mockito.Mockito.never()).listItems();
    }
}
