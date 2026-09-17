package edu.seu.vcampus.server.shop;

import edu.seu.vcampus.common.shop.entity.ShopOrder;
import edu.seu.vcampus.common.shop.entity.ShopItem;
import edu.seu.vcampus.common.shop.entity.ShopOrderStatus;
import edu.seu.vcampus.common.shop.dto.OrderListResponse;
import edu.seu.vcampus.common.shop.dto.OrderQuery;
import edu.seu.vcampus.common.bank.entity.BankTransaction;
import edu.seu.vcampus.common.bank.entity.BankTransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
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
        return new ShopItem("S001", "校园文化衫", new BigDecimal(price), 100, "纯棉短袖", "SHOP001");
    }

    /** 构造一条待支付测试订单。 */
    private ShopOrder order(String orderId, int quantity, String total) {
        ShopOrder order = new ShopOrder();
        order.setoId(orderId);
        order.setoUserUuid(userUuid);
        order.setoItemId("S001");
        order.setoQuantity(Integer.valueOf(quantity));
        order.setoTotal(new BigDecimal(total));
        order.setoTime(new Date());
        order.setoStatus(ShopOrderStatus.UNPAID);
        return order;
    }

    /** 构造一条与订单金额一致的成功消费流水。 */
    private BankTransaction transaction(ShopOrder order) {
        BigDecimal before = new BigDecimal("500.00");
        return new BankTransaction("tx-1", "account-1",
                BankTransactionType.CONSUMPTION, order.getoTotal(), before,
                before.subtract(order.getoTotal()), order.getoId(), "购买商品", new Date());
    }

    /**
     * 总价应为"单价 × 数量"，且订单初始状态为待支付。
     */
    @Test
    void purchaseComputesTotalOnServer() {
        when(dao.findItemById("S001")).thenReturn(item("59.90"));
        when(dao.reduceStock("S001", 2)).thenReturn(true);
        when(dao.addOrder(any(ShopOrder.class))).thenReturn(true);

        ShopOrder order = service.purchase(userUuid, "S001", 2);

        assertNotNull(order, "下单应成功");
        assertEquals(new BigDecimal("119.80"), order.getoTotal(), "总价应为单价×数量");
        assertEquals(ShopOrderStatus.UNPAID, order.getoStatus());
        assertEquals(userUuid, order.getoUserUuid());
        assertEquals(Integer.valueOf(2), order.getoQuantity());
        assertNotNull(order.getoId(), "应生成订单ID");
        assertNotNull(order.getoTime(), "应记录下单时间");
        verify(dao, never()).reduceStock(anyString(), anyInt());
    }

    /**
     * 库存不足时不应落单。
     */
    @Test
    void purchaseRejectedWhenStockInsufficient() {
        when(dao.findItemById("S001")).thenReturn(item("59.90"));

        assertNull(service.purchase(userUuid, "S001", 999), "库存不足应返回 null");
        verify(dao, never()).reduceStock(anyString(), anyInt());
        verify(dao, never()).addOrder(any(ShopOrder.class));
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
        verify(dao, never()).addOrder(any(ShopOrder.class));
    }

    /**
     * 订单写入失败时不应修改库存。
     */
    @Test
    void purchaseDoesNotTouchStockWhenOrderInsertFails() {
        when(dao.findItemById("S001")).thenReturn(item("59.90"));
        when(dao.addOrder(any(ShopOrder.class))).thenReturn(false);

        assertNull(service.purchase(userUuid, "S001", 3), "落单失败应返回 null");
        verify(dao, never()).reduceStock(anyString(), anyInt());
    }

    /** 待支付订单修改数量后应重算金额，但不能提前扣减库存。 */
    @Test
    void updateOrderQuantityRecalculatesTotalWithoutReducingStock() {
        ShopDaoMemory memory = new ShopDaoMemory();
        ShopService memoryService = new ShopService(memory);
        ShopOrder created = memoryService.purchase(userUuid, "S001", 2);

        ShopOrder updated = memoryService.updateOrderQuantity(
                created.getoId(), userUuid, 4);

        assertNotNull(updated);
        assertEquals(Integer.valueOf(4), updated.getoQuantity());
        assertEquals(new BigDecimal("239.60"), updated.getoTotal());
        assertEquals(Integer.valueOf(100), memory.findItemById("S001").getSiStock());
    }

    /** 数量清零应移除待支付订单行，且不改变库存。 */
    @Test
    void zeroQuantityRemovesUnpaidOrderWithoutChangingStock() {
        ShopDaoMemory memory = new ShopDaoMemory();
        ShopService memoryService = new ShopService(memory);
        ShopOrder created = memoryService.purchase(userUuid, "S001", 2);

        ShopOrder removed = memoryService.updateOrderQuantity(
                created.getoId(), userUuid, 0);

        assertNotNull(removed);
        assertEquals(Integer.valueOf(0), removed.getoQuantity());
        assertEquals(BigDecimal.ZERO, removed.getoTotal());
        assertNull(memory.findOrderById(created.getoId()));
        assertEquals(Integer.valueOf(100), memory.findItemById("S001").getSiStock());
    }

    /** 数量修改必须校验订单所有权、状态和当前库存。 */
    @Test
    void updateOrderQuantityRejectsInvalidOwnerStateAndStock() {
        ShopDaoMemory memory = new ShopDaoMemory();
        ShopService memoryService = new ShopService(memory);
        ShopOrder created = memoryService.purchase(userUuid, "S001", 2);

        assertNull(memoryService.updateOrderQuantity(created.getoId(), "other-user", 3));
        assertNull(memoryService.updateOrderQuantity(created.getoId(), userUuid, -1));
        assertNull(memoryService.updateOrderQuantity(created.getoId(), userUuid, 101));
        assertTrue(memory.updateOrderStatus(created.getoId(), ShopOrderStatus.PAID));
        assertNull(memoryService.updateOrderQuantity(created.getoId(), userUuid, 3));
        assertEquals(Integer.valueOf(2), created.getoQuantity());
        assertEquals(Integer.valueOf(100), memory.findItemById("S001").getSiStock());
    }

    /** 支付成功时才应扣减库存并将订单标记为已支付。 */
    @Test
    void payOrderReducesStockAfterSuccessfulCharge() {
        BankAdapter bank = mock(BankAdapter.class);
        service = new ShopService(dao, bank);
        ShopOrder order = order("order-1", 2, "119.80");
        char[] password = "bank12345".toCharArray();
        when(dao.findOrderById("order-1")).thenReturn(order);
        when(dao.findItemById("S001")).thenReturn(item("59.90"));
        when(dao.reduceStock("S001", 2)).thenReturn(true);
        when(bank.deduct(userUuid, password, order.getoTotal(), "order-1",
                "购买商品 - 订单:order-1")).thenReturn(transaction(order));
        when(dao.updateOrderStatus("order-1", ShopOrderStatus.PAID)).thenReturn(true);

        assertTrue(service.payOrder("order-1", userUuid, password));

        verify(dao).reduceStock("S001", 2);
        verify(dao).updateOrderStatus("order-1", ShopOrderStatus.PAID);
    }

    /** 银行扣款失败时应回补支付阶段临时扣减的库存。 */
    @Test
    void payOrderRestoresStockWhenChargeFails() {
        BankAdapter bank = mock(BankAdapter.class);
        service = new ShopService(dao, bank);
        ShopOrder order = order("order-2", 3, "179.70");
        char[] password = "bank12345".toCharArray();
        when(dao.findOrderById("order-2")).thenReturn(order);
        when(dao.findItemById("S001")).thenReturn(item("59.90"));
        when(dao.reduceStock("S001", 3)).thenReturn(true);
        when(bank.deduct(userUuid, password, order.getoTotal(), "order-2",
                "购买商品 - 订单:order-2")).thenReturn(null);

        assertTrue(!service.payOrder("order-2", userUuid, password));

        verify(dao).reduceStock("S001", 3);
        verify(dao).reduceStock("S001", -3);
        verify(dao, never()).updateOrderStatus(anyString(), any(ShopOrderStatus.class));
    }

    /** 待支付订单应直接取消，不退款也不修改库存。 */
    @Test
    void cancelUnpaidOrderOnlyChangesStatus() {
        BankAdapter bank = mock(BankAdapter.class);
        service = new ShopService(dao, bank);
        ShopOrder order = order("order-unpaid", 2, "119.80");
        when(dao.findOrderById("order-unpaid")).thenReturn(order);
        when(dao.updateOrderStatus("order-unpaid", ShopOrderStatus.CANCELLED))
                .thenReturn(true);

        assertTrue(service.cancelOrder("order-unpaid", userUuid));

        verify(dao).updateOrderStatus("order-unpaid", ShopOrderStatus.CANCELLED);
        verify(dao, never()).reduceStock(anyString(), anyInt());
        verify(bank, never()).refund(anyString(), any(BigDecimal.class),
                anyString(), anyString());
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

    /** 内存目录应提供 30 件商品，且全部归属于唯一的校园 Shop。 */
    @Test
    void memoryCatalogContainsThirtyItemsInOneShop() {
        ShopDaoMemory memory = new ShopDaoMemory();

        assertEquals(1, memory.findAllShops().size());
        assertEquals(30, memory.findAllItems().size());
        for (ShopItem catalogItem : memory.findAllItems()) {
            assertEquals("SHOP001", catalogItem.getSiShopId());
        }
    }

    /** 我的订单应按待支付、已支付状态分别查询并独立计算总数。 */
    @Test
    void listOrdersOfUserPagedFiltersByRequestedStatus() {
        ShopDaoMemory memory = new ShopDaoMemory();
        ShopService memoryService = new ShopService(memory);
        ShopOrder unpaid = memoryService.purchase(userUuid, "S001", 1);
        ShopOrder paid = memoryService.purchase(userUuid, "S002", 2);
        assertTrue(memory.updateOrderStatus(paid.getoId(), ShopOrderStatus.PAID));

        OrderListResponse unpaidPage = memoryService.listOrdersOfUserPaged(userUuid,
                new OrderQuery(1, 10, ShopOrderStatus.UNPAID, null));
        OrderListResponse paidPage = memoryService.listOrdersOfUserPaged(userUuid,
                new OrderQuery(1, 10, ShopOrderStatus.PAID, null));

        assertEquals(1L, unpaidPage.getTotalCount());
        assertEquals(unpaid.getoId(), unpaidPage.getOrders().get(0).getoId());
        assertEquals(1L, paidPage.getTotalCount());
        assertEquals(paid.getoId(), paidPage.getOrders().get(0).getoId());
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
