package edu.seu.vcampus.server.shop;

import edu.seu.vcampus.common.shop.dto.OrderQuery;
import edu.seu.vcampus.common.shop.entity.ShopItem;
import edu.seu.vcampus.common.shop.entity.ShopOrder;
import edu.seu.vcampus.common.shop.entity.ShopOrderStatus;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 不依赖数据库的 ShopService 业务规则测试。 */
class ShopServiceTest {

    private static final String USER_UUID = "uuid-shop-user";
    private ShopDao dao;
    private BankAdapter bank;
    private ShopService service;

    @BeforeEach
    void setUp() {
        dao = mock(ShopDao.class);
        bank = mock(BankAdapter.class);
        when(dao.findAllItems()).thenReturn(Collections.<ShopItem>emptyList());
        when(dao.addItem(any(ShopItem.class))).thenReturn(true);
        service = new ShopService(dao, bank);
    }

    @Test
    void repeatedItemMergesIntoExistingUnpaidOrder() {
        ShopItem item = item(10);
        ShopOrder existing = order("O-1", 2, "19.80");
        when(dao.findItemById("S001")).thenReturn(item);
        when(dao.findUnpaidOrder(USER_UUID, "S001")).thenReturn(existing);
        when(dao.updateUnpaidOrder("O-1", USER_UUID, 5,
                new BigDecimal("49.50"))).thenReturn(true);

        ShopOrder merged = service.purchase(USER_UUID, "S001", 3);

        assertSame(existing, merged);
        assertEquals(Integer.valueOf(5), merged.getoQuantity());
        assertEquals(new BigDecimal("49.50"), merged.getoTotal());
        verify(dao, never()).addOrder(any(ShopOrder.class));
        verify(dao, never()).reduceStock(anyString(), anyInt());
    }

    @Test
    void zeroQuantityDeletesUnpaidOrder() {
        ShopOrder existing = order("O-2", 2, "19.80");
        when(dao.findOrderById("O-2")).thenReturn(existing);
        when(dao.deleteUnpaidOrder("O-2", USER_UUID)).thenReturn(true);

        ShopOrder removed = service.updateOrderQuantity("O-2", USER_UUID, 0);

        assertSame(existing, removed);
        assertEquals(Integer.valueOf(0), removed.getoQuantity());
        assertEquals(BigDecimal.ZERO, removed.getoTotal());
        verify(dao, never()).reduceStock(anyString(), anyInt());
    }

    @Test
    void paymentFailureRestoresStockAndLeavesStatusUnchanged() {
        ShopOrder existing = order("O-3", 2, "19.80");
        when(dao.findOrderById("O-3")).thenReturn(existing);
        when(dao.findItemById("S001")).thenReturn(item(10));
        when(dao.reduceStock("S001", 2)).thenReturn(true);
        when(bank.deduct(eq(USER_UUID), any(char[].class),
                eq(new BigDecimal("19.80")), eq("O-3"), anyString())).thenReturn(null);

        assertTrue(!service.payOrder("O-3", USER_UUID, "bank12345".toCharArray()));

        verify(dao).reduceStock("S001", 2);
        verify(dao).reduceStock("S001", -2);
        verify(dao, never()).updateOrderStatus(anyString(), any(ShopOrderStatus.class));
    }

    @Test
    void statusFilterIsPassedToDatabasePaging() {
        OrderQuery query = new OrderQuery(2, 10, ShopOrderStatus.PAID, null);
        when(dao.findOrdersByUserPaged(USER_UUID, ShopOrderStatus.PAID, 2, 10))
                .thenReturn(Collections.<ShopOrder>emptyList());
        when(dao.countOrdersByUser(USER_UUID, ShopOrderStatus.PAID)).thenReturn(11L);

        assertEquals(11L, service.listOrdersOfUserPaged(USER_UUID, query).getTotalCount());
        verify(dao).findOrdersByUserPaged(USER_UUID, ShopOrderStatus.PAID, 2, 10);
    }

    @Test
    void catalogBootstrapAddsThirtyDatabaseItemsOnlyOnce() {
        assertTrue(ShopCatalogBootstrap.ensure(dao));
        verify(dao, org.mockito.Mockito.times(30)).addItem(any(ShopItem.class));
    }

    private static ShopItem item(int stock) {
        return new ShopItem("S001", "校园文化衫", new BigDecimal("9.90"),
                Integer.valueOf(stock), "test", null);
    }

    private static ShopOrder order(String id, int quantity, String total) {
        ShopOrder order = new ShopOrder();
        order.setoId(id);
        order.setoUserUuid(USER_UUID);
        order.setoItemId("S001");
        order.setoQuantity(Integer.valueOf(quantity));
        order.setoTotal(new BigDecimal(total));
        order.setoTime(new Date());
        order.setoStatus(ShopOrderStatus.UNPAID);
        return order;
    }
}
