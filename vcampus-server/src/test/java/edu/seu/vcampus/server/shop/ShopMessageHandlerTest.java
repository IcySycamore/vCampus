package edu.seu.vcampus.server.shop;

import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.message.MessageSender;
import edu.seu.vcampus.common.shop.ShopCommands;
import edu.seu.vcampus.common.shop.dto.OrderLineRequest;
import edu.seu.vcampus.common.shop.dto.OrderQuantityUpdateRequest;
import edu.seu.vcampus.common.shop.dto.ShopPaymentRequest;
import edu.seu.vcampus.common.shop.entity.ShopOrder;
import edu.seu.vcampus.server.network.ServerMessageDispatcher;
import edu.seu.vcampus.server.user.SessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Shop 消息格式、会话身份和命令注册测试。 */
class ShopMessageHandlerTest {

    private ShopService service;
    private ShopMessageHandler handler;
    private String token;

    @BeforeEach
    void setUp() {
        SessionManager sessions = new SessionManager();
        token = sessions.create("uuid-shop-user", "shop-user", "学生");
        service = mock(ShopService.class);
        handler = new ShopMessageHandler(service, sessions);
    }

    @Test
    void createOrderUsesAuthenticatedUuidAndTypedRequest() {
        ShopOrder created = new ShopOrder();
        when(service.purchase("uuid-shop-user", "S001", 2)).thenReturn(created);

        Message response = send(Command.SHOP_ORDER_CREATE,
                new OrderLineRequest("S001", 2));

        assertEquals(StatusCode.SUCCESS, response.getStatusCode());
        assertSame(created, response.getData());
        verify(service).purchase("uuid-shop-user", "S001", 2);
    }

    @Test
    void createOrderRejectsLegacyPayload() {
        Message response = send(Command.SHOP_ORDER_CREATE, new Object[] { "S001", 2 });

        assertEquals(StatusCode.BAD_REQUEST, response.getStatusCode());
        assertEquals("请求数据格式错误", response.getData());
    }

    @Test
    void moduleRegistersPayCommandAndClearsPassword() {
        ServerMessageDispatcher dispatcher = new ServerMessageDispatcher();
        SessionManager sessions = new SessionManager();
        String payToken = sessions.create("uuid-pay-user", "pay-user", "学生");
        when(service.payOrders(ArgumentMatchers.<String>anyList(),
                eq("uuid-pay-user"), any(char[].class)))
                .thenReturn(true);
        ShopModule.register(dispatcher, sessions, service);

        ShopPaymentRequest payment = new ShopPaymentRequest(
                "order-pay", "bank12345".toCharArray());
        Message request = new Message(Command.SHOP_ORDER_PAY, payment);
        request.setToken(payToken);
        Message response = dispatch(dispatcher, request);

        verify(service).payOrders(ArgumentMatchers.<String>anyList(),
                eq("uuid-pay-user"), any(char[].class));
        assertEquals(StatusCode.SUCCESS, response.getStatusCode());
        for (char value : payment.getBankPassword()) {
            assertEquals('\0', value);
        }
    }

    @Test
    void moduleRegistersQuantityUpdateCommand() {
        ServerMessageDispatcher dispatcher = new ServerMessageDispatcher();
        SessionManager sessions = new SessionManager();
        String updateToken = sessions.create("uuid-update-user", "update-user", "学生");
        ShopOrder updated = new ShopOrder();
        when(service.updateOrderQuantity("order-update", "uuid-update-user", 3))
                .thenReturn(updated);
        ShopModule.register(dispatcher, sessions, service);

        Message request = new Message(ShopCommands.ORDER_QUANTITY_UPDATE,
                new OrderQuantityUpdateRequest("order-update", 3));
        request.setToken(updateToken);
        Message response = dispatch(dispatcher, request);

        verify(service).updateOrderQuantity("order-update", "uuid-update-user", 3);
        assertEquals(StatusCode.SUCCESS, response.getStatusCode());
        assertSame(updated, response.getData());
    }

    @Test
    void nonAdminCannotUseCatalogMaintenanceCommand() {
        Message response = send(Command.SHOP_ITEM_UPSERT, mock(
                edu.seu.vcampus.common.shop.entity.ShopItem.class));

        assertEquals(StatusCode.FORBIDDEN, response.getStatusCode());
        verify(service, never()).upsertItem(any(
                edu.seu.vcampus.common.shop.entity.ShopItem.class));
    }

    private Message send(int command, Object data) {
        Message request = new Message(command, data);
        request.setToken(token);
        return handle(handler, request);
    }

    private static Message dispatch(ServerMessageDispatcher dispatcher, Message request) {
        final Message[] sent = new Message[1];
        dispatcher.dispatch(request, new MessageSender() {
            @Override
            public void send(Message response) {
                sent[0] = response;
            }
        });
        return sent[0];
    }

    private static Message handle(ShopMessageHandler target, Message request) {
        final Message[] sent = new Message[1];
        target.handle(request, new MessageSender() {
            @Override
            public void send(Message response) {
                sent[0] = response;
            }
        });
        return sent[0];
    }
}
