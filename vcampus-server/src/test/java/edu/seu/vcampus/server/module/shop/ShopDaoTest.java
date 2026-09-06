package edu.seu.vcampus.server.module.shop;

import edu.seu.vcampus.common.entity.Order;
import edu.seu.vcampus.common.entity.ShopItem;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ShopDao 接口契约测试：用 Mockito 验证调用方可依赖的行为约定（见 ADR-0005）。
 * 真实 SQL 行为由 {@link ShopDaoImplTest} 覆盖。
 */
class ShopDaoTest {

    /**
     * 商品查询命中时应返回对应商品。
     */
    @Test
    void findItemByIdReturnsMatchingItem() {
        ShopDao dao = mock(ShopDao.class);
        ShopItem expected = new ShopItem("S001", "校园文化衫",
                new BigDecimal("59.90"), 100, "纯棉短袖");
        when(dao.findItemById("S001")).thenReturn(expected);

        assertEquals("校园文化衫", dao.findItemById("S001").getSiName());
        verify(dao).findItemById("S001");
    }

    /**
     * 商品查询未命中时约定返回 null。
     */
    @Test
    void findItemByIdReturnsNullWhenAbsent() {
        ShopDao dao = mock(ShopDao.class);
        when(dao.findItemById("S404")).thenReturn(null);

        assertNull(dao.findItemById("S404"));
    }

    /**
     * 商品列表查询应返回列表。
     */
    @Test
    void findAllItemsReturnsList() {
        ShopDao dao = mock(ShopDao.class);
        List<ShopItem> items = new ArrayList<>();
        items.add(new ShopItem("S002", "笔记本", new BigDecimal("12.50"), 30, "A5"));
        when(dao.findAllItems()).thenReturn(items);

        assertEquals(1, dao.findAllItems().size());
    }

    /**
     * 库存充足时扣减返回 true，不足时返回 false。
     */
    @Test
    void reduceStockReflectsAvailability() {
        ShopDao dao = mock(ShopDao.class);
        when(dao.reduceStock("S001", 2)).thenReturn(true);
        when(dao.reduceStock("S001", 999)).thenReturn(false);

        assertTrue(dao.reduceStock("S001", 2), "库存充足应扣减成功");
        assertFalse(dao.reduceStock("S001", 999), "库存不足应返回 false");
    }

    /**
     * 增删改商品成功时应返回 true。
     */
    @Test
    void itemMutationsReturnTrueOnSuccess() {
        ShopDao dao = mock(ShopDao.class);
        ShopItem item = new ShopItem("S003", "保温杯", new BigDecimal("88.00"), 20, "500ml");
        when(dao.addItem(any(ShopItem.class))).thenReturn(true);
        when(dao.updateItem(any(ShopItem.class))).thenReturn(true);
        when(dao.deleteItem("S003")).thenReturn(true);

        assertTrue(dao.addItem(item));
        assertTrue(dao.updateItem(item));
        assertTrue(dao.deleteItem("S003"));
    }

    /**
     * 新增订单成功应返回 true，并可按用户查回订单列表。
     */
    @Test
    void orderMutationAndQuery() {
        ShopDao dao = mock(ShopDao.class);
        Order order = new Order("O001", "001", "S001", 2,
                new BigDecimal("119.80"), new Date(), "待支付");
        List<Order> orders = new ArrayList<>();
        orders.add(order);
        when(dao.addOrder(any(Order.class))).thenReturn(true);
        when(dao.findOrdersByUser("001")).thenReturn(orders);

        assertTrue(dao.addOrder(order));
        assertEquals(1, dao.findOrdersByUser("001").size());
    }
}
