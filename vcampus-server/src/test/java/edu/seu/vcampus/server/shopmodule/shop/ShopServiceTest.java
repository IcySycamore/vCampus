package edu.seu.vcampus.server.shopmodule.shop;

import edu.seu.vcampus.common.shop.Order;
import edu.seu.vcampus.common.shop.ShopItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ShopService 业务逻辑测试：用 Mockito 隔离数据库，覆盖参数校验、金额计算、
 * 库存不足与回补分支（见 ADR-0005）。
 */
class ShopServiceTest {

    /** 数据访问替身。 */
    private ShopDao dao;

    /** 被测服务。 */
    private ShopService service;

    /** 测试用户的全局身份 UUID。 */
    private final String userUuid = "550e8400-e29b-41d4-a716-446655440000";

    /**
     * 每个用例前重建替身与被测对象。
     */
    @BeforeEach
    void setUp() {
        dao = mock(ShopDao.class);
        service = new ShopService(dao);
    }

    /**
     * 构造一件测试商品。
     *
     * @param price 单价
     * @return 商品对象
     */
    private ShopItem item(String price) {
        return new ShopItem("S001", "校园文化衫", new BigDecimal(price), 100, "纯棉短袖");
    }

    /**
     * 总价应为"单价 × 数量"，且订单初始状态为待支付。
     */
    @Test
    void purchaseComputesTotalOnServer() {
        when(dao.findItemById("S001")).thenReturn(item("59.90"));
        when(dao.reduceStock("S001", 2)).thenReturn(true);
        when(dao.addOrder(any(Order.class))).thenReturn(true);

        Order order = service.purchase(userUuid, "S001", 2);

        assertNotNull(order, "下单应成功");
        assertEquals(new BigDecimal("119.80"), order.getoTotal(), "总价应为单价×数量");
        assertEquals("待支付", order.getoStatus());
        assertEquals(userUuid, order.getoUserUuid());
        assertEquals(Integer.valueOf(2), order.getoQuantity());
        assertNotNull(order.getoId(), "应生成订单ID");
        assertNotNull(order.getoTime(), "应记录下单时间");
    }

    /**
     * 库存不足时不应落单。
     */
    @Test
    void purchaseRejectedWhenStockInsufficient() {
        when(dao.findItemById("S001")).thenReturn(item("59.90"));
        when(dao.reduceStock("S001", 999)).thenReturn(false);

        assertNull(service.purchase(userUuid, "S001", 999), "库存不足应返回 null");
        verify(dao, never()).addOrder(any(Order.class));
    }

    /**
     * 请求数量超过商品当前库存时，应在扣库存前拒绝下单。
     */
    @Test
    void purchaseChecksCurrentStockBeforeReducing() {
        ShopItem item = item("59.90");
        item.setSiStock(2);
        when(dao.findItemById("S001")).thenReturn(item);

        assertNull(service.purchase(userUuid, "S001", 3), "当前库存不足应提前返回 null");
        verify(dao, never()).reduceStock(anyString(), anyInt());
        verify(dao, never()).addOrder(any(Order.class));
    }

    /**
     * 订单写入失败时应回补已扣减的库存，避免扣了库存却没有订单。
     */
    @Test
    void purchaseRestoresStockWhenOrderInsertFails() {
        when(dao.findItemById("S001")).thenReturn(item("59.90"));
        when(dao.reduceStock("S001", 3)).thenReturn(true);
        when(dao.addOrder(any(Order.class))).thenReturn(false);

        assertNull(service.purchase(userUuid, "S001", 3), "落单失败应返回 null");
        verify(dao).reduceStock("S001", -3);
    }

    /**
     * 商品不存在时应拒绝下单，且不触碰库存。
     */
    @Test
    void purchaseRejectedWhenItemAbsent() {
        when(dao.findItemById("S404")).thenReturn(null);

        assertNull(service.purchase(userUuid, "S404", 1));
        verify(dao, never()).reduceStock(anyString(), anyInt());
    }

    /**
     * 非法参数（空用户、空商品、非正数量）应直接拒绝，不访问数据库。
     */
    @Test
    void purchaseRejectsInvalidArguments() {
        assertNull(service.purchase(null, "S001", 1), "用户为空应拒绝");
        assertNull(service.purchase(userUuid, null, 1), "商品为空应拒绝");
        assertNull(service.purchase(userUuid, "S001", 0), "数量为 0 应拒绝");
        assertNull(service.purchase(userUuid, "S001", -1), "数量为负应拒绝");
        verify(dao, never()).findItemById(anyString());
    }

    /**
     * 商品查询应委托给 DAO；ID 为空时直接返回 null。
     */
    @Test
    void getItemDelegatesAndGuardsBlankId() {
        when(dao.findItemById("S001")).thenReturn(item("59.90"));

        assertEquals("校园文化衫", service.getItem("S001").getSiName());
        assertNull(service.getItem(""), "空ID应返回 null");
    }

    /**
     * 商品列表应原样返回 DAO 结果。
     */
    @Test
    void listItemsDelegatesToDao() {
        List<ShopItem> items = new ArrayList<>();
        items.add(item("12.50"));
        when(dao.findAllItems()).thenReturn(items);

        assertEquals(1, service.listItems().size());
    }

    /**
     * 用户ID为空时订单查询应返回空列表而非 null，且不访问数据库。
     */
    @Test
    void listOrdersReturnsEmptyListForBlankUser() {
        assertTrue(service.listOrdersOfUser(null).isEmpty());
        verify(dao, never()).findOrdersByUser(anyString());
    }
}
