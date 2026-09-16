package edu.seu.vcampus.server.shop;

import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.message.MessageHandler;
import edu.seu.vcampus.common.message.MessageSender;
import edu.seu.vcampus.common.shop.entity.ShopItem;
import edu.seu.vcampus.common.shop.entity.ShopOrder;
import edu.seu.vcampus.common.shop.dto.OrderQuery;
import edu.seu.vcampus.common.shop.dto.OrderListResponse;
import edu.seu.vcampus.server.user.SessionManager;

import java.util.List;

/**
 * 商店命令处理器：501-509 商品浏览、订单管理。
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
        } catch (IllegalArgumentException e) {
            send(sender, request, StatusCode.BAD_REQUEST, e.getMessage());
        } catch (RuntimeException e) {
            send(sender, request, StatusCode.INTERNAL_ERROR, null);
        }
    }

    private void listItems(Message request, MessageSender sender) {
        List<ShopItem> items = shopService.listItems();
        send(sender, request, StatusCode.SUCCESS, items);
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
        if (request.getData() == null) {
            send(sender, request, StatusCode.BAD_REQUEST, null);
            return;
        }

        // 期望格式：{ itemId, quantity }
        Object[] orderData = (Object[]) request.getData();
        if (orderData.length < 2) {
            send(sender, request, StatusCode.BAD_REQUEST, null);
            return;
        }

        String itemId = (String) orderData[0];
        int quantity = ((Number) orderData[1]).intValue();

        ShopOrder order = shopService.purchase(userUuid, itemId, quantity);
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

    private void cancelOrder(Message request, MessageSender sender, String userUuid) {
        if (!(request.getData() instanceof String)) {
            send(sender, request, StatusCode.BAD_REQUEST, null);
            return;
        }
        String orderId = (String) request.getData();
        boolean success = shopService.cancelOrder(orderId, userUuid);
        if (!success) {
            send(sender, request, StatusCode.BAD_REQUEST, "取消订单失败");
            return;
        }
        send(sender, request, StatusCode.SUCCESS, null);
    }

    private void payOrder(Message request, MessageSender sender, String userUuid) {
        if (!(request.getData() instanceof String)) {
            send(sender, request, StatusCode.BAD_REQUEST, null);
            return;
        }
        String orderId = (String) request.getData();
        boolean success = shopService.payOrder(orderId, userUuid);
        if (!success) {
            send(sender, request, StatusCode.BAD_REQUEST, "支付订单失败");
            return;
        }
        send(sender, request, StatusCode.SUCCESS, null);
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
        // TODO: 从 SessionManager 获取用户 UUID
        // 暂时从 request.getSender() 获取（需要后续对接会话管理）
        return request.getSender();
    }

    private static void send(MessageSender sender, Message request, String statusCode,
            Object data) {
        Message response = new Message(request.getCommand(), data);
        response.setUid(request.getUid());
        response.setStatusCode(statusCode);
        sender.send(response);
    }
}
