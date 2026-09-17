package edu.seu.vcampus.client.shop;

import edu.seu.vcampus.client.network.ClientMessageDispatcher;
import edu.seu.vcampus.client.user.UserService;
import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.message.MessageSender;
import edu.seu.vcampus.common.shop.ShopCommands;
import edu.seu.vcampus.common.shop.dto.OrderLineRequest;
import edu.seu.vcampus.common.shop.dto.OrderQuantityUpdateRequest;
import edu.seu.vcampus.common.shop.dto.ShopPaymentRequest;
import edu.seu.vcampus.common.shop.entity.ShopOrder;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 验证 Shop 客户端使用与服务端一致的下单请求协议。 */
class ShopServiceTest {

    /** 客户端消息分发器。 */
    private ClientMessageDispatcher dispatcher;

    /** 待测 Shop API。 */
    private ShopService service;

    /** 最近一次发送的请求。 */
    private Message sent;

    /** 服务端返回的订单。 */
    private ShopOrder responseOrder;

    /** 发送器在请求对象清零前捕获的支付订单号。 */
    private String paymentOrderId;

    /** 发送器捕获的本次结算订单列表。 */
    private List<String> paymentOrderIds;

    /** 发送器在请求对象清零前捕获的银行密码。 */
    private char[] paymentPassword;

    /** 搭建一个可同步回包的客户端消息链路。 */
    @BeforeEach
    void setUp() {
        dispatcher = new ClientMessageDispatcher();
        UserService users = mock(UserService.class);
        when(users.currentToken()).thenReturn("shop-token");
        service = new ShopService(dispatcher, users);
        responseOrder = new ShopOrder();

        dispatcher.bindSender(new MessageSender() {
            @Override
            public void send(Message request) {
                sent = request;
                if (request.getData() instanceof ShopPaymentRequest) {
                    ShopPaymentRequest payment = (ShopPaymentRequest) request.getData();
                    paymentOrderId = payment.getOrderId();
                    paymentOrderIds = payment.getOrderIds();
                    paymentPassword = payment.getBankPassword();
                }
                Message response = new Message(request.getCommand(), responseOrder);
                response.setUid(request.getUid());
                response.setStatusCode(StatusCode.SUCCESS);
                dispatcher.dispatch(response);
            }
        });
    }

    /** 创建订单时应发送强类型 DTO，而不是 Map 或数组。 */
    @Test
    void createOrderSendsOrderLineRequest() {
        ShopOrder result = service.createOrder("S001", 2);

        assertSame(responseOrder, result);
        assertEquals(Command.SHOP_ORDER_CREATE, sent.getCommand());
        assertEquals("shop-token", sent.getToken());
        assertTrue(sent.getData() instanceof OrderLineRequest);
        OrderLineRequest request = (OrderLineRequest) sent.getData();
        assertEquals("S001", request.getItemId());
        assertEquals(2, request.getQuantity());
    }

    /** 非法数量应在网络请求发出前被 DTO 拒绝。 */
    @Test
    void createOrderRejectsInvalidQuantityBeforeSending() {
        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() {
                service.createOrder("S001", 0);
            }
        });
    }

    /** 修改数量时应发送订单ID和新数量，并接收服务端重算后的订单。 */
    @Test
    void updateOrderQuantityUsesShopUpdateProtocol() {
        ShopOrder result = service.updateOrderQuantity("order-1", 4);

        assertSame(responseOrder, result);
        assertEquals(ShopCommands.ORDER_QUANTITY_UPDATE, sent.getCommand());
        assertTrue(sent.getData() instanceof OrderQuantityUpdateRequest);
        OrderQuantityUpdateRequest request =
                (OrderQuantityUpdateRequest) sent.getData();
        assertEquals("order-1", request.getOrderId());
        assertEquals(4, request.getQuantity());
    }

    /** 数量为零应作为删除待支付订单的合法请求发送。 */
    @Test
    void zeroQuantityIsSentAsRemoveRequest() {
        service.updateOrderQuantity("order-remove", 0);

        OrderQuantityUpdateRequest request =
                (OrderQuantityUpdateRequest) sent.getData();
        assertEquals(ShopCommands.ORDER_QUANTITY_UPDATE, sent.getCommand());
        assertEquals("order-remove", request.getOrderId());
        assertEquals(0, request.getQuantity());
    }

    /** 支付订单时应发送订单号和银行密码，且结束后清零请求中的密码。 */
    @Test
    void payOrderSendsPasswordRequestAndClearsItAfterCall() {
        char[] password = "bank12345".toCharArray();

        service.payOrder("order-1", password);

        assertEquals(Command.SHOP_ORDER_PAY, sent.getCommand());
        assertTrue(sent.getData() instanceof ShopPaymentRequest);
        assertEquals("order-1", paymentOrderId);
        assertEquals("bank12345", new String(paymentPassword));
        char[] cleared = ((ShopPaymentRequest) sent.getData()).getBankPassword();
        for (char value : cleared) {
            assertEquals('\0', value);
        }
    }

    /** 多订单结算应在一个请求中发送全部勾选订单。 */
    @Test
    void payOrdersSendsAllSelectedOrderIds() {
        service.payOrders(Arrays.asList("order-1", "order-2"),
                "bank12345".toCharArray());

        assertEquals(Arrays.asList("order-1", "order-2"), paymentOrderIds);
        assertEquals(Command.SHOP_ORDER_PAY, sent.getCommand());
    }
}
