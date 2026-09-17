package edu.seu.vcampus.server.shop;

import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.message.MessageHandler;
import edu.seu.vcampus.common.message.MessageSender;
import edu.seu.vcampus.common.shop.ShopCommands;
import edu.seu.vcampus.common.shop.entity.ShopItem;
import edu.seu.vcampus.common.shop.entity.ShopOrder;
import edu.seu.vcampus.common.shop.dto.OrderQuery;
import edu.seu.vcampus.common.shop.dto.OrderListResponse;
import edu.seu.vcampus.common.shop.dto.OrderLineRequest;
import edu.seu.vcampus.common.shop.dto.OrderQuantityUpdateRequest;
import edu.seu.vcampus.common.shop.dto.ShopPaymentRequest;
import edu.seu.vcampus.server.user.SessionManager;

import java.util.Arrays;
import java.util.List;

/**
 * 商店命令处理器：商品浏览、订单管理与购物车数量编辑。
 *
 * <p>
 * token 的合法性由服务器会话层统一检查，本类通过 SessionManager 获取当前用户身份；
 * 未登录时返回 401。
 * </p>
 */
public class ShopMessageHandler implements MessageHandler {

    private final ShopService shopService;
    private final SessionManager sessionManager;

    /**
     * 创建商店处理器。
     *
     * @param shopService 商店业务服务
     * @param sessionManager 会话管理器
     */
    public ShopMessageHandler(ShopService shopService, SessionManager sessionManager) {
        if (shopService == null) {
            throw new IllegalArgumentException("shopService must not be null");
        }
        if (sessionManager == null) {
            throw new IllegalArgumentException("sessionManager must not be null");
        }
        this.shopService = shopService;
        this.sessionManager = sessionManager;
    }

    /**
     * 按商店命令处理请求并通过 sender 发送一条响应。
     *
     * @param request 商店请求
     * @param sender 响应发送器
     */
    @Override
    public void handle(Message request, MessageSender sender) {
        if (request == null || sender == null) {
            throw new IllegalArgumentException("request and sender are required");
        }

        try {
            // 验证登录状态
            if (request.getToken() == null || request.getToken().trim().isEmpty()) {
                send(sender, request, StatusCode.UNAUTHORIZED, null);
                return;
            }

            String userUuid = getUserUuid(request);
            if (userUuid == null || userUuid.isEmpty()) {
                send(sender, request, StatusCode.UNAUTHORIZED, null);
                return;
            }

            switch (request.getCommand()) {
            case Command.SHOP_ITEM_LIST:
                listItems(request, sender);
                return;
            case Command.SHOP_ITEM_DETAIL:
                getItemDetail(request, sender, userUuid);
                return;
            case Command.SHOP_ORDER_CREATE:
                createOrder(request, sender, userUuid);
                return;
            case Command.SHOP_ORDER_LIST:
                listOrders(request, sender, userUuid);
                return;
            case Command.SHOP_ORDER_DETAIL:
                getOrderDetail(request, sender, userUuid);
                return;
            case Command.SHOP_ORDER_CANCEL:
                cancelOrder(request, sender, userUuid);
                return;
            case Command.SHOP_ORDER_PAY:
                payOrder(request, sender, userUuid);
                return;
            case ShopCommands.ORDER_QUANTITY_UPDATE:
                updateOrderQuantity(request, sender, userUuid);
                return;
            case Command.SHOP_ORDER_ADVANCE:
                advanceOrderStatus(request, sender, userUuid);
                return;
            case Command.SHOP_ITEM_UPSERT:
                upsertItem(request, sender, userUuid);
                return;
            case Command.SHOP_ORDER_QUERY:
                queryAllOrders(request, sender, userUuid);
                return;
            default:
                send(sender, request, StatusCode.BAD_REQUEST, null);
            }
        } catch (ShopPaymentException e) {
            send(sender, request, e.getStatusCode(), e.getMessage());
        } catch (IllegalArgumentException e) {
            send(sender, request, StatusCode.BAD_REQUEST, e.getMessage());
        } catch (RuntimeException e) {
            send(sender, request, StatusCode.INTERNAL_ERROR, null);
        }
    }

    private void listItems(Message request, MessageSender sender) {
        System.out.println("[ShopMessageHandler] 开始处理商品列表请求");
        try {
            List<ShopItem> items = shopService.listItems();
            System.out.println("[ShopMessageHandler] 获取到商品数量: " + (items != null ? items.size() : "null"));
            send(sender, request, StatusCode.SUCCESS, items);
            System.out.println("[ShopMessageHandler] 商品列表响应已发送");
        } catch (Exception e) {
            System.err.println("[ShopMessageHandler] 获取商品列表失败: " + e.getMessage());
            e.printStackTrace();
            send(sender, request, StatusCode.INTERNAL_ERROR, null);
        }
    }

    private void getItemDetail(Message request, MessageSender sender, String userUuid) {
        if (!(request.getData() instanceof String)) {
            send(sender, request, StatusCode.BAD_REQUEST, null);
            return;
        }
        String itemId = (String) request.getData();
        ShopItem item = shopService.getItem(itemId);
        if (item == null) {
            send(sender, request, StatusCode.NOT_FOUND, null);
            return;
        }
        send(sender, request, StatusCode.SUCCESS, item);
    }

    private void createOrder(Message request, MessageSender sender, String userUuid) {
        if (!(request.getData() instanceof OrderLineRequest)) {
            send(sender, request, StatusCode.BAD_REQUEST, "请求数据格式错误");
            return;
        }

        OrderLineRequest orderRequest = (OrderLineRequest) request.getData();

        ShopOrder order = shopService.purchase(userUuid, orderRequest.getItemId(),
                orderRequest.getQuantity());
        if (order == null) {
            send(sender, request, StatusCode.BAD_REQUEST, "库存不足或商品不存在");
            return;
        }

        send(sender, request, StatusCode.SUCCESS, order);
    }

    private void listOrders(Message request, MessageSender sender, String userUuid) {
        // 支持分页查询：客户端可传入 OrderQuery 对象，不传则使用默认分页参数
        OrderQuery query;
        if (request.getData() instanceof OrderQuery) {
            query = (OrderQuery) request.getData();
        } else {
            query = new OrderQuery(); // 使用默认分页参数
        }

        OrderListResponse response = shopService.listOrdersOfUserPaged(userUuid, query);
        send(sender, request, StatusCode.SUCCESS, response);
    }

    private void getOrderDetail(Message request, MessageSender sender, String userUuid) {
        if (!(request.getData() instanceof String)) {
            send(sender, request, StatusCode.BAD_REQUEST, null);
            return;
        }
        String orderId = (String) request.getData();
        ShopOrder order = shopService.findOrderById(orderId);
        if (order == null) {
            send(sender, request, StatusCode.NOT_FOUND, null);
            return;
        }
        // 验证订单所有权
        if (!userUuid.equals(order.getoUserUuid())) {
            send(sender, request, StatusCode.FORBIDDEN, null);
            return;
        }
        send(sender, request, StatusCode.SUCCESS, order);
    }

    private void updateOrderQuantity(Message request, MessageSender sender, String userUuid) {
        if (!(request.getData() instanceof OrderQuantityUpdateRequest)) {
            send(sender, request, StatusCode.BAD_REQUEST, "请求数据格式错误");
            return;
        }
        OrderQuantityUpdateRequest update =
                (OrderQuantityUpdateRequest) request.getData();
        ShopOrder order = shopService.updateOrderQuantity(update.getOrderId(), userUuid,
                update.getQuantity());
        if (order == null) {
            send(sender, request, StatusCode.BAD_REQUEST,
                    "只能修改本人的待支付订单，且数量不能超过库存");
            return;
        }
        send(sender, request, StatusCode.SUCCESS, order);
    }

    private void cancelOrder(Message request, MessageSender sender, String userUuid) {
        if (!(request.getData() instanceof String)) {
            send(sender, request, StatusCode.BAD_REQUEST, null);
            return;
        }
        String orderId = (String) request.getData();
        boolean success = shopService.cancelOrder(orderId, userUuid);
        if (!success) {
            send(sender, request, StatusCode.BAD_REQUEST,
                    "订单不存在、无权操作或当前状态不能取消");
            return;
        }
        send(sender, request, StatusCode.SUCCESS, null);
    }

    private void payOrder(Message request, MessageSender sender, String userUuid) {
        if (!(request.getData() instanceof ShopPaymentRequest)) {
            send(sender, request, StatusCode.BAD_REQUEST, "支付请求格式错误");
            return;
        }
        ShopPaymentRequest payment = (ShopPaymentRequest) request.getData();
        char[] password = payment.getBankPassword();
        try {
            boolean success = shopService.payOrders(payment.getOrderIds(), userUuid, password);
            if (!success) {
                send(sender, request, StatusCode.BAD_REQUEST,
                        "订单不存在、状态异常或库存不足");
                return;
            }
            send(sender, request, StatusCode.SUCCESS, null);
        } finally {
            Arrays.fill(password, '\0');
            payment.clearBankPassword();
        }
    }

    // ==================== 管理员功能 ====================

    /**
     * 管理员推进订单状态（发货、完成等）。
     */
    private void advanceOrderStatus(Message request, MessageSender sender, String userUuid) {
        // TODO: 添加管理员权限校验
        if (!(request.getData() instanceof java.util.Map)) {
            send(sender, request, StatusCode.BAD_REQUEST, "请求数据格式错误");
            return;
        }

        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> data = (java.util.Map<String, Object>) request.getData();
        String orderId = (String) data.get("orderId");
        String statusStr = (String) data.get("status");

        if (orderId == null || statusStr == null) {
            send(sender, request, StatusCode.BAD_REQUEST, "订单ID和状态不能为空");
            return;
        }

        edu.seu.vcampus.common.shop.entity.ShopOrderStatus newStatus;
        try {
            newStatus = edu.seu.vcampus.common.shop.entity.ShopOrderStatus.valueOf(statusStr);
        } catch (IllegalArgumentException e) {
            send(sender, request, StatusCode.BAD_REQUEST, "无效的订单状态");
            return;
        }

        boolean success = shopService.advanceOrderStatus(orderId, newStatus);
        if (!success) {
            send(sender, request, StatusCode.BAD_REQUEST, "更新订单状态失败");
            return;
        }
        send(sender, request, StatusCode.SUCCESS, null);
    }

    /**
     * 管理员新增或更新商品。
     */
    private void upsertItem(Message request, MessageSender sender, String userUuid) {
        // TODO: 添加管理员权限校验
        if (!(request.getData() instanceof ShopItem)) {
            send(sender, request, StatusCode.BAD_REQUEST, "请求数据格式错误");
            return;
        }

        ShopItem item = (ShopItem) request.getData();
        boolean success = shopService.upsertItem(item);
        if (!success) {
            send(sender, request, StatusCode.BAD_REQUEST, "新增或更新商品失败");
            return;
        }
        send(sender, request, StatusCode.SUCCESS, item);
    }

    /**
     * 管理员查询所有订单（分页）。
     */
    private void queryAllOrders(Message request, MessageSender sender, String userUuid) {
        // TODO: 添加管理员权限校验
        OrderQuery query = null;
        if (request.getData() instanceof OrderQuery) {
            query = (OrderQuery) request.getData();
        }

        OrderListResponse response = shopService.queryAllOrders(query);
        send(sender, request, StatusCode.SUCCESS, response);
    }

    /**
     * 从会话中获取用户 UUID。
     *
     * @param request 请求消息
     * @return 用户 UUID，未找到返回 null
     */
    private String getUserUuid(Message request) {
        String token = request.getToken();
        if (token == null || token.trim().isEmpty()) {
            return null;
        }

        edu.seu.vcampus.common.user.entity.SessionEntry session = sessionManager.validate(token);
        if (session == null) {
            return null;
        }

        return session.getUuid();
    }

    private static void send(MessageSender sender, Message request, String statusCode,
            Object data) {
        Message response = new Message(request.getCommand(), data);
        response.setUid(request.getUid());
        response.setStatusCode(statusCode);
        sender.send(response);
    }
}
