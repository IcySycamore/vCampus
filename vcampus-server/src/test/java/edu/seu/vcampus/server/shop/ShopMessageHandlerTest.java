package edu.seu.vcampus.server.shop;

import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.message.MessageSender;
import edu.seu.vcampus.common.shop.ShopCommands;
import edu.seu.vcampus.common.shop.dto.OrderListResponse;
import edu.seu.vcampus.common.shop.dto.OrderLineRequest;
import edu.seu.vcampus.common.shop.dto.OrderQuery;
import edu.seu.vcampus.common.shop.dto.OrderQuantityUpdateRequest;
import edu.seu.vcampus.common.shop.dto.ShopPaymentRequest;
import edu.seu.vcampus.common.shop.entity.ShopOrder;
import edu.seu.vcampus.common.shop.entity.ShopOrderStatus;
import edu.seu.vcampus.server.network.ServerMessageDispatcher;
import edu.seu.vcampus.server.user.SessionManager;
import java.math.BigDecimal;
import java.util.HashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 验证 Shop 下单消息与内存 DAO 的完整处理链路。 */
class ShopMessageHandlerTest {

    /** 被测处理器。 */
    private ShopMessageHandler handler;

    /** 用于验证订单与库存状态的内存 DAO。 */
    private ShopDaoMemory dao;

    /** 有效的测试会话令牌。 */
    private String token;

    /** 每个用例使用新的内存 Shop 数据。 */
    @BeforeEach
    void setUp() {
        SessionManager sessions = new SessionManager();
        token = sessions.create("uuid-shop-user", "shop-user", "学生");
        dao = new ShopDaoMemory();
        handler = new ShopMessageHandler(new ShopService(dao), sessions);
    }

    /** 合法 DTO 应生成订单并返回成功。 */
    @Test
    void createOrderWithDtoReturnsCreatedOrder() {
        Message response = send(Command.SHOP_ORDER_CREATE,
                new OrderLineRequest("S001", 2));

        assertEquals(StatusCode.SUCCESS, response.getStatusCode());
        assertTrue(response.getData() instanceof ShopOrder);
        ShopOrder order = (ShopOrder) response.getData();
        assertNotNull(order.getoId());
        assertEquals("uuid-shop-user", order.getoUserUuid());
        assertEquals("S001", order.getoItemId());
        assertEquals(Integer.valueOf(2), order.getoQuantity());
        assertEquals(new BigDecimal("119.80"), order.getoTotal());
    }

    /** 旧的 Map 载荷应返回 400，不应再因强制转换返回 500。 */
    @Test
    void createOrderWithWrongPayloadReturnsBadRequest() {
        Message response = send(Command.SHOP_ORDER_CREATE,
                new HashMap<String, Object>());

        assertEquals(StatusCode.BAD_REQUEST, response.getStatusCode());
        assertEquals("请求数据格式错误", response.getData());
    }

    /** 同一商品重复下单应累加到原待支付订单。 */
    @Test
    void repeatedItemIncreasesQuantityInsteadOfAddingRow() {
        ShopOrder first = (ShopOrder) send(Command.SHOP_ORDER_CREATE,
                new OrderLineRequest("S001", 2)).getData();
        ShopOrder merged = (ShopOrder) send(Command.SHOP_ORDER_CREATE,
                new OrderLineRequest("S001", 3)).getData();
        Message listResponse = send(Command.SHOP_ORDER_LIST, new OrderQuery());

        assertEquals(first.getoId(), merged.getoId());
        assertEquals(Integer.valueOf(5), merged.getoQuantity());
        assertEquals(new BigDecimal("299.50"), merged.getoTotal());
        OrderListResponse orders = (OrderListResponse) listResponse.getData();
        assertEquals(1L, orders.getTotalCount());
        assertEquals(1, orders.getOrders().size());
        assertEquals(Integer.valueOf(5), orders.getOrders().get(0).getoQuantity());
        assertEquals(Integer.valueOf(100), dao.findItemById("S001").getSiStock());
    }

    /** 不同商品仍应生成独立订单行。 */
    @Test
    void differentItemsRemainSeparateRows() {
        send(Command.SHOP_ORDER_CREATE, new OrderLineRequest("S001", 1));
        send(Command.SHOP_ORDER_CREATE, new OrderLineRequest("S002", 1));

        OrderListResponse orders = (OrderListResponse) send(
                Command.SHOP_ORDER_LIST, new OrderQuery()).getData();

        assertEquals(2L, orders.getTotalCount());
        assertEquals(2, orders.getOrders().size());
    }

    /** 多次加入的累计数量不得超过当前库存。 */
    @Test
    void repeatedItemCannotExceedAvailableStock() {
        send(Command.SHOP_ORDER_CREATE, new OrderLineRequest("S001", 60));

        Message rejected = send(Command.SHOP_ORDER_CREATE,
                new OrderLineRequest("S001", 50));
        OrderListResponse orders = (OrderListResponse) send(
                Command.SHOP_ORDER_LIST, new OrderQuery()).getData();

        assertEquals(StatusCode.BAD_REQUEST, rejected.getStatusCode());
        assertEquals(1L, orders.getTotalCount());
        assertEquals(Integer.valueOf(60), orders.getOrders().get(0).getoQuantity());
        assertEquals(Integer.valueOf(100), dao.findItemById("S001").getSiStock());
    }

    /** 数量更新命令应保存新数量并返回服务端重算后的金额。 */
    @Test
    void quantityUpdateReturnsRecalculatedOrder() {
        ShopOrder created = (ShopOrder) send(Command.SHOP_ORDER_CREATE,
                new OrderLineRequest("S001", 2)).getData();

        Message response = send(ShopCommands.ORDER_QUANTITY_UPDATE,
                new OrderQuantityUpdateRequest(created.getoId(), 5));

        assertEquals(StatusCode.SUCCESS, response.getStatusCode());
        ShopOrder updated = (ShopOrder) response.getData();
        assertEquals(Integer.valueOf(5), updated.getoQuantity());
        assertEquals(new BigDecimal("299.50"), updated.getoTotal());
        assertEquals(Integer.valueOf(100), dao.findItemById("S001").getSiStock());
    }

    /** 数量清零命令应让待支付订单从列表中消失。 */
    @Test
    void zeroQuantityRemovesOrderFromList() {
        ShopOrder created = (ShopOrder) send(Command.SHOP_ORDER_CREATE,
                new OrderLineRequest("S001", 2)).getData();

        Message response = send(ShopCommands.ORDER_QUANTITY_UPDATE,
                new OrderQuantityUpdateRequest(created.getoId(), 0));
        OrderListResponse orders = (OrderListResponse) send(
                Command.SHOP_ORDER_LIST, new OrderQuery()).getData();

        assertEquals(StatusCode.SUCCESS, response.getStatusCode());
        assertEquals(Integer.valueOf(0),
                ((ShopOrder) response.getData()).getoQuantity());
        assertEquals(0L, orders.getTotalCount());
        assertTrue(orders.getOrders().isEmpty());
        assertEquals(Integer.valueOf(100), dao.findItemById("S001").getSiStock());
    }

    /** 已支付订单不得接收后续购买数量。 */
    @Test
    void paidOrderIsNotMergedWithLaterPurchase() {
        ShopOrder paid = (ShopOrder) send(Command.SHOP_ORDER_CREATE,
                new OrderLineRequest("S001", 1)).getData();
        assertTrue(dao.updateOrderStatus(paid.getoId(), ShopOrderStatus.PAID));

        ShopOrder later = (ShopOrder) send(Command.SHOP_ORDER_CREATE,
                new OrderLineRequest("S001", 2)).getData();
        OrderListResponse orders = (OrderListResponse) send(
                Command.SHOP_ORDER_LIST, new OrderQuery()).getData();

        assertTrue(!paid.getoId().equals(later.getoId()));
        assertEquals(2L, orders.getTotalCount());
        assertEquals(Integer.valueOf(1), paid.getoQuantity());
        assertEquals(Integer.valueOf(2), later.getoQuantity());
    }

    /** Shop 模块必须注册支付命令，否则支付流程无法进入服务层。 */
    @Test
    void moduleRegistersPayCommand() {
        ServerMessageDispatcher dispatcher = new ServerMessageDispatcher();
        SessionManager sessions = new SessionManager();
        String payToken = sessions.create("uuid-pay-user", "pay-user", "学生");
        ShopService service = mock(ShopService.class);
        when(service.payOrders(ArgumentMatchers.<String>anyList(), eq("uuid-pay-user"),
                any(char[].class))).thenReturn(true);
        ShopModule.register(dispatcher, sessions, service);

        ShopPaymentRequest payment = new ShopPaymentRequest(
                "order-pay", "bank12345".toCharArray());
        Message request = new Message(Command.SHOP_ORDER_PAY, payment);
        request.setToken(payToken);
        final Message[] sent = new Message[1];
        dispatcher.dispatch(request, new MessageSender() {
            @Override
            public void send(Message response) {
                sent[0] = response;
            }
        });

        verify(service).payOrders(ArgumentMatchers.<String>anyList(), eq("uuid-pay-user"),
                any(char[].class));
        assertEquals(StatusCode.SUCCESS, sent[0].getStatusCode());
        for (char value : payment.getBankPassword()) {
            assertEquals('\0', value);
        }
    }

    /** Shop 模块必须注册数量更新命令。 */
    @Test
    void moduleRegistersQuantityUpdateCommand() {
        ServerMessageDispatcher dispatcher = new ServerMessageDispatcher();
        SessionManager sessions = new SessionManager();
        String updateToken = sessions.create("uuid-update-user", "update-user", "学生");
        ShopService service = mock(ShopService.class);
        ShopOrder updated = new ShopOrder();
        when(service.updateOrderQuantity("order-update", "uuid-update-user", 3))
                .thenReturn(updated);
        ShopModule.register(dispatcher, sessions, service);

        Message request = new Message(ShopCommands.ORDER_QUANTITY_UPDATE,
                new OrderQuantityUpdateRequest("order-update", 3));
        request.setToken(updateToken);
        final Message[] sent = new Message[1];
        dispatcher.dispatch(request, new MessageSender() {
            @Override
            public void send(Message response) {
                sent[0] = response;
            }
        });

        verify(service).updateOrderQuantity("order-update", "uuid-update-user", 3);
        assertEquals(StatusCode.SUCCESS, sent[0].getStatusCode());
        assertEquals(updated, sent[0].getData());
    }

    /** 发送下单请求并捕获处理器的响应。 */
    private Message send(int command, Object data) {
        Message request = new Message(command, data);
        request.setToken(token);
        request.setUid(503L);
        final Message[] sent = new Message[1];
        handler.handle(request, new MessageSender() {
            @Override
            public void send(Message response) {
                sent[0] = response;
            }
        });
        return sent[0];
    }
}
