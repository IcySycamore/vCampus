package edu.seu.vcampus.client.view.shop;

import edu.seu.vcampus.client.shop.ShopService;
import edu.seu.vcampus.common.shop.entity.ShopItem;
import java.awt.Frame;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

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
}
