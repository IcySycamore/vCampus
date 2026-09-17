package edu.seu.vcampus.client.shop;

import edu.seu.vcampus.client.api.ApiErrors;
import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.client.handler.ConnectionListener;
import edu.seu.vcampus.client.network.ClientMessageDispatcher;
import edu.seu.vcampus.client.user.UserService;
import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.NetworkConstant;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.shop.ShopCommands;
import edu.seu.vcampus.common.shop.dto.OrderListResponse;
import edu.seu.vcampus.common.shop.dto.OrderLineRequest;
import edu.seu.vcampus.common.shop.dto.OrderQuantityUpdateRequest;
import edu.seu.vcampus.common.shop.dto.OrderQuery;
import edu.seu.vcampus.common.shop.dto.ShopPaymentRequest;
import edu.seu.vcampus.common.shop.entity.ShopItem;
import edu.seu.vcampus.common.shop.entity.ShopOrder;
import java.util.List;
import java.util.Collections;

/**
 * 商店同步客户端 API；请求复用分发器，身份取自共享用户会话。
 */
public class ShopService implements ConnectionListener {

    private final ClientMessageDispatcher dispatcher;
    private final UserService users;
    private boolean disconnected;

    /**
     * 创建商店服务。
     *
     * @param dispatcher 共享分发器
     * @param users 共享用户服务
     */
    public ShopService(ClientMessageDispatcher dispatcher, UserService users) {
        this.dispatcher = dispatcher;
        this.users = users;
        this.disconnected = false;
    }

    /**
     * 获取商品列表。
     *
     * @return 商品列表
     * @throws ApiException 网络或业务错误
     */
    @SuppressWarnings("unchecked")
    public List<ShopItem> listItems() throws ApiException {
        return call(Command.SHOP_ITEM_LIST, null, List.class);
    }

    /**
     * 获取商品详情。
     *
     * @param itemId 商品ID
     * @return 商品详情
     * @throws ApiException 网络或业务错误
     */
    public ShopItem getItemDetail(String itemId) throws ApiException {
        return call(Command.SHOP_ITEM_DETAIL, itemId, ShopItem.class);
    }

    /**
     * 创建订单。
     *
     * @param itemId 商品ID
     * @param quantity 购买数量
     * @return 创建的订单
     * @throws ApiException 网络或业务错误
     */
    public ShopOrder createOrder(String itemId, int quantity) throws ApiException {
        OrderLineRequest request = new OrderLineRequest(itemId, quantity);
        return call(Command.SHOP_ORDER_CREATE, request, ShopOrder.class);
    }

    /**
     * 查询我的订单列表（分页）。
     *
     * @param query 查询条件
     * @return 订单列表响应
     * @throws ApiException 网络或业务错误
     */
    public OrderListResponse listMyOrders(OrderQuery query) throws ApiException {
        return call(Command.SHOP_ORDER_LIST, query, OrderListResponse.class);
    }

    /**
     * 获取订单详情。
     *
     * @param orderId 订单ID
     * @return 订单详情
     * @throws ApiException 网络或业务错误
     */
    public ShopOrder getOrderDetail(String orderId) throws ApiException {
        return call(Command.SHOP_ORDER_DETAIL, orderId, ShopOrder.class);
    }

    /**
     * 修改待支付订单中的商品数量。
     *
     * @param orderId 订单ID
     * @param quantity 新数量
     * @return 服务端重算金额后的订单
     * @throws ApiException 网络或业务错误
     */
    public ShopOrder updateOrderQuantity(String orderId, int quantity) throws ApiException {
        OrderQuantityUpdateRequest request =
                new OrderQuantityUpdateRequest(orderId, quantity);
        return call(ShopCommands.ORDER_QUANTITY_UPDATE, request, ShopOrder.class);
    }

    /**
     * 取消订单（退款）。
     *
     * @param orderId 订单ID
     * @throws ApiException 网络或业务错误
     */
    public void cancelOrder(String orderId) throws ApiException {
        call(Command.SHOP_ORDER_CANCEL, orderId, Void.class);
    }

    /**
     * 支付订单。
     *
     * @param orderId 订单ID
     * @param bankPassword 当前用户的银行密码
     * @throws ApiException 网络或业务错误
     */
    public void payOrder(String orderId, char[] bankPassword) throws ApiException {
        payOrders(Collections.singletonList(orderId), bankPassword);
    }

    /**
     * 一次结算多个待支付订单。
     *
     * @param orderIds 待支付订单ID列表
     * @param bankPassword 当前用户的银行密码
     * @throws ApiException 网络或业务错误
     */
    public void payOrders(List<String> orderIds, char[] bankPassword) throws ApiException {
        ShopPaymentRequest request = new ShopPaymentRequest(orderIds, bankPassword);
        try {
            call(Command.SHOP_ORDER_PAY, request, Void.class);
        } finally {
            request.clearBankPassword();
        }
    }

    /**
     * 创建或更新商品（管理员）。
     *
     * @param item 商品信息
     * @return 创建/更新后的商品
     * @throws ApiException 网络或业务错误
     */
    public ShopItem upsertItem(ShopItem item) throws ApiException {
        return call(Command.SHOP_ITEM_UPSERT, item, ShopItem.class);
    }

    /**
     * 推进订单状态（管理员）。
     *
     * @param orderId 订单ID
     * @throws ApiException 网络或业务错误
     */
    public void advanceOrder(String orderId) throws ApiException {
        call(Command.SHOP_ORDER_ADVANCE, orderId, Void.class);
    }

    /**
     * 查询所有订单（管理员，分页）。
     *
     * @param query 查询条件
     * @return 订单列表响应
     * @throws ApiException 网络或业务错误
     */
    public OrderListResponse queryAllOrders(OrderQuery query) throws ApiException {
        return call(Command.SHOP_ORDER_QUERY, query, OrderListResponse.class);
    }

    /**
     * 连接关闭时清理状态。
     *
     * @param cause 关闭原因（可为 null）
     */
    @Override
    public void connectionClosed(Exception cause) {
        disconnected = true;
    }

    /**
     * 发送请求并等待响应（同步阻塞）。
     *
     * @param command 命令
     * @param payload 请求数据
     * @param resultType 期望的响应类型
     * @return 响应数据
     * @throws ApiException 网络或业务错误
     */
    private <T> T call(int command, Object payload, Class<T> resultType) throws ApiException {
        if (disconnected) {
            throw new ApiException(ApiErrors.LOCAL_NETWORK);
        }
        String token = users.currentToken();
        if (token == null) {
            throw new ApiException(StatusCode.UNAUTHORIZED);
        }
        Message request = new Message(command, payload);
        request.setToken(token);
        Message response;
        try {
            response = dispatcher.request(request, NetworkConstant.DEFAULT_REQUEST_TIMEOUT_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(ApiErrors.LOCAL_INTERRUPTED);
        } catch (RuntimeException e) {
            throw new ApiException(ApiErrors.LOCAL_NETWORK);
        }
        if (response == null) {
            throw new ApiException(ApiErrors.LOCAL_TIMEOUT);
        }
        if (response.getCommand() != command || request.getUid() == null
                || !request.getUid().equals(response.getUid())
                || response.getStatusCode() == null) {
            throw new ApiException(ApiErrors.LOCAL_MALFORMED);
        }
        if (StatusCode.UNAUTHORIZED.equals(response.getStatusCode())) {
            users.connectionClosed(null);
            throw new ApiException(StatusCode.UNAUTHORIZED);
        }
        if (!StatusCode.SUCCESS.equals(response.getStatusCode())) {
            String message = response.getData() instanceof String
                    ? (String) response.getData() : ApiErrors.messageFor(response.getStatusCode());
            throw new ApiException(response.getStatusCode(), message);
        }
        if (resultType == Void.class) {
            return null;
        }
        if (!resultType.isInstance(response.getData())) {
            throw new ApiException(ApiErrors.LOCAL_MALFORMED);
        }
        return resultType.cast(response.getData());
    }
}
