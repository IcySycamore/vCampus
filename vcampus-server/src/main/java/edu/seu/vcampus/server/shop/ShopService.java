package edu.seu.vcampus.server.shop;

import edu.seu.vcampus.common.shop.entity.ShopItem;
import edu.seu.vcampus.common.shop.entity.ShopOrder;
import edu.seu.vcampus.common.shop.entity.ShopOrderStatus;
import edu.seu.vcampus.common.shop.dto.OrderListResponse;
import edu.seu.vcampus.common.shop.dto.OrderQuery;
import edu.seu.vcampus.common.bank.entity.BankTransaction;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 商店业务逻辑：商品浏览、购买下单与订单查询。
 *
 * <p>金额一律由本层按"单价 × 数量"计算，客户端提交的金额不予采信。创建待支付
 * 订单时不扣库存；支付时依赖 {@link ShopDao#reduceStock} 的原子语义校验并扣减库存。
 */
public class ShopService {

    /** 订单初始状态。 */
    private static final ShopOrderStatus STATUS_UNPAID = ShopOrderStatus.UNPAID;

    /** 数据访问对象。 */
    private final ShopDao shopDao;

    /** 银行适配器。 */
    private final BankAdapter bankAdapter;

    /** 当前服务实例是否已经检查过数据库商品目录。 */
    private volatile boolean catalogReady;
    /**
     * 使用指定的数据访问对象和银行适配器构造服务（用于测试）。
     *
     * @param shopDao 数据访问对象
     * @param bankAdapter 银行适配器
     */
    public ShopService(ShopDao shopDao, BankAdapter bankAdapter) {
        if (shopDao == null || bankAdapter == null) {
            throw new IllegalArgumentException("shopDao and bankAdapter must not be null");
        }
        this.shopDao = shopDao;
        this.bankAdapter = bankAdapter;
    }

    /**
     * 查询全部商品，供客户端展示商品列表。
     *
     * @return 商品列表；无数据时为空列表
     */
    public List<ShopItem> listItems() {
        ensureCatalog();
        return shopDao.findAllItems();
    }

    /**
     * 按商品ID查询商品详情。
     *
     * @param itemId 商品ID
     * @return 对应商品；商品ID为空或不存在时返回 null
     */
    public ShopItem getItem(String itemId) {
        if (isBlank(itemId)) {
            return null;
        }
        return shopDao.findItemById(itemId);
    }

    /**
     * 购买商品:校验参数与库存,计算总价并生成订单。
     *
     * <p>创建订单只记录待支付数量，不扣减库存；库存在支付时再校验并扣减。
     *
     * @param userUuid 下单用户的全局身份UUID
     * @param itemId 商品ID
     * @param quantity 购买数量,须大于 0
     * @return 下单成功返回生成的订单;参数非法、商品不存在或库存不足时返回 null
     */
    public synchronized ShopOrder purchase(String userUuid, String itemId, int quantity) {
        if (isBlank(userUuid) || isBlank(itemId) || quantity <= 0) {
            return null;
        }
        ensureCatalog();
        ShopItem item = shopDao.findItemById(itemId);
        if (item == null || item.getSiPrice() == null || item.getSiStock() == null
                || item.getSiStock() < quantity) {
            return null;
        }
        ShopOrder existing = shopDao.findUnpaidOrder(userUuid, itemId);
        if (existing != null) {
            int mergedQuantity = existing.getoQuantity().intValue() + quantity;
            if (mergedQuantity > item.getSiStock().intValue()) {
                return null;
            }
            BigDecimal total = item.getSiPrice().multiply(BigDecimal.valueOf(mergedQuantity));
            if (!shopDao.updateUnpaidOrder(existing.getoId(), userUuid,
                    mergedQuantity, total)) {
                return null;
            }
            existing.setoQuantity(Integer.valueOf(mergedQuantity));
            existing.setoTotal(total);
            return existing;
        }

        ShopOrder order = buildOrder(userUuid, item, quantity);
        if (!shopDao.addOrder(order)) {
            return null;
        }
        return order;
    }

    /**
     * 查询指定用户的订单,按下单时间倒序。
     *
     * @param userUuid 用户的全局身份UUID
     * @return 订单列表;用户ID为空时返回空列表
     */
    public List<ShopOrder> listOrdersOfUser(String userUuid) {
        if (isBlank(userUuid)) {
            return new ArrayList<>();
        }
        return shopDao.findOrdersByUser(userUuid);
    }

    /**
     * 分页查询指定用户的订单,按下单时间倒序。
     *
     * @param userUuid 用户UUID
     * @param query 分页查询参数
     * @return 订单分页响应
     */
    public OrderListResponse listOrdersOfUserPaged(String userUuid, OrderQuery query) {
        if (query == null) {
            query = new OrderQuery();
        }
        if (isBlank(userUuid)) {
            return new OrderListResponse(new ArrayList<ShopOrder>(),
                    query.getPageNumber(), query.getPageSize(), 0);
        }

        List<ShopOrder> orders = shopDao.findOrdersByUserPaged(userUuid,
                query.getStatus(), query.getPageNumber(), query.getPageSize());
        long totalCount = shopDao.countOrdersByUser(userUuid, query.getStatus());

        return new OrderListResponse(orders,
                query.getPageNumber(), query.getPageSize(), totalCount);
    }

    /**
     * 修改当前用户待支付订单的商品数量。
     *
     * <p>数量修改只更新购物车订单，不扣减库存；总价始终由服务端使用当前商品单价
     * 重新计算。数量超过当前库存时拒绝修改，数量为零时移除该订单行。
     *
     * @param orderId 订单ID
     * @param userUuid 当前用户UUID
     * @param quantity 新数量
     * @return 更新后的订单；校验失败或当前存储不支持修改时返回 null
     */
    public synchronized ShopOrder updateOrderQuantity(String orderId, String userUuid,
            int quantity) {
        if (isBlank(orderId) || isBlank(userUuid) || quantity < 0) {
            return null;
        }
        ShopOrder order = shopDao.findOrderById(orderId);
        if (order == null || !userUuid.equals(order.getoUserUuid())
                || order.getoStatus() != ShopOrderStatus.UNPAID) {
            return null;
        }
        if (quantity == 0) {
            if (!shopDao.deleteUnpaidOrder(orderId, userUuid)) {
                return null;
            }
            order.setoQuantity(Integer.valueOf(0));
            order.setoTotal(BigDecimal.ZERO);
            return order;
        }
        ShopItem item = shopDao.findItemById(order.getoItemId());
        if (item == null || item.getSiPrice() == null || item.getSiStock() == null
                || item.getSiStock().intValue() < quantity) {
            return null;
        }
        BigDecimal total = item.getSiPrice().multiply(BigDecimal.valueOf(quantity));
        if (!shopDao.updateUnpaidOrder(orderId, userUuid, quantity, total)) {
            return null;
        }
        order.setoQuantity(Integer.valueOf(quantity));
        order.setoTotal(total);
        return order;
    }

    /**
     * 支付订单:扣除用户账户余额并更新订单状态为已支付。
     *
     * <p>仅支持状态为UNPAID的订单。支付时才扣减库存；库存不足或银行扣款失败时，
     * 订单仍保持待支付，且不保留库存扣减。
     *
     * @param orderId 订单ID
     * @param userUuid 用户UUID(用于验证订单所有权)
     * @param bankPassword 当前用户的银行密码
     * @return 支付成功返回true,失败返回false
     */
    public boolean payOrder(String orderId, String userUuid, char[] bankPassword) {
        return payOrders(Collections.singletonList(orderId), userUuid, bankPassword);
    }

    /**
     * 一次结算多个待支付订单，库存和银行扣款按整批处理。
     *
     * @param orderIds 待支付订单ID列表
     * @param userUuid 用户UUID
     * @param bankPassword 当前用户的银行密码
     * @return 整批支付成功返回 true，任一订单无效或库存不足返回 false
     */
    public synchronized boolean payOrders(List<String> orderIds, String userUuid,
            char[] bankPassword) {
        if (orderIds == null || orderIds.isEmpty() || isBlank(userUuid)
                || bankPassword == null || bankPassword.length == 0) {
            return false;
        }

        List<ShopOrder> orders = new ArrayList<ShopOrder>();
        Set<String> uniqueIds = new HashSet<String>();
        Map<String, Integer> quantities = new LinkedHashMap<String, Integer>();
        BigDecimal total = BigDecimal.ZERO;
        for (String orderId : orderIds) {
            if (isBlank(orderId) || !uniqueIds.add(orderId)) {
                return false;
            }
            ShopOrder order = shopDao.findOrderById(orderId);
            if (!isPayableOrder(order, userUuid)) {
                return false;
            }
            orders.add(order);
            total = total.add(order.getoTotal());
            Integer current = quantities.get(order.getoItemId());
            quantities.put(order.getoItemId(), Integer.valueOf(
                    (current == null ? 0 : current.intValue())
                            + order.getoQuantity().intValue()));
        }

        for (Map.Entry<String, Integer> entry : quantities.entrySet()) {
            ShopItem item = shopDao.findItemById(entry.getKey());
            if (item == null || item.getSiStock() == null
                    || item.getSiStock().intValue() < entry.getValue().intValue()) {
                return false;
            }
        }

        Map<String, Integer> reduced = new LinkedHashMap<String, Integer>();
        for (Map.Entry<String, Integer> entry : quantities.entrySet()) {
            if (!shopDao.reduceStock(entry.getKey(), entry.getValue().intValue())) {
                restoreStocks(reduced);
                return false;
            }
            reduced.put(entry.getKey(), entry.getValue());
        }

        String reference = paymentReference(orderIds);
        String remark = orders.size() == 1
                ? "购买商品 - 订单:" + reference : "购买商品 - 合并结算:" + reference;
        BankTransaction transaction;
        try {
            transaction = bankAdapter.deduct(userUuid, bankPassword,
                    total, reference, remark);
        } catch (RuntimeException e) {
            restoreStocks(reduced);
            throw e;
        }
        if (transaction == null) {
            restoreStocks(reduced);
            return false;
        }

        List<ShopOrder> updated = new ArrayList<ShopOrder>();
        for (ShopOrder order : orders) {
            if (!shopDao.updateOrderStatus(order.getoId(), ShopOrderStatus.PAID)) {
                for (ShopOrder paid : updated) {
                    shopDao.updateOrderStatus(paid.getoId(), ShopOrderStatus.UNPAID);
                }
                restoreStocks(reduced);
                bankAdapter.refund(userUuid, total, reference,
                        "支付状态更新失败退款 - 合并结算:" + reference);
                return false;
            }
            updated.add(order);
        }
        return true;
    }

    private boolean isPayableOrder(ShopOrder order, String userUuid) {
        return order != null && userUuid.equals(order.getoUserUuid())
                && order.getoStatus() == ShopOrderStatus.UNPAID
                && order.getoItemId() != null && order.getoQuantity() != null
                && order.getoQuantity().intValue() > 0 && order.getoTotal() != null
                && order.getoTotal().compareTo(BigDecimal.ZERO) > 0;
    }

    private void restoreStocks(Map<String, Integer> quantities) {
        for (Map.Entry<String, Integer> entry : quantities.entrySet()) {
            shopDao.reduceStock(entry.getKey(), -entry.getValue().intValue());
        }
    }

    private String paymentReference(List<String> orderIds) {
        if (orderIds.size() == 1) {
            return orderIds.get(0);
        }
        List<String> sorted = new ArrayList<String>(orderIds);
        Collections.sort(sorted);
        StringBuilder reference = new StringBuilder();
        for (String orderId : sorted) {
            reference.append(orderId).append('\n');
        }
        return UUID.nameUUIDFromBytes(reference.toString()
                .getBytes(StandardCharsets.UTF_8)).toString().replace("-", "");
    }

    /**
     * 取消订单并更新订单状态为已取消。
     *
     * <p>待支付订单可直接取消，不涉及余额和库存；已支付订单取消时退还订单金额，
     * 同时恢复商品库存。其他状态不能取消。
     *
     * @param orderId 订单ID
     * @param userUuid 用户UUID(用于验证订单所有权)
     * @return 取消成功返回true,失败返回false
     */
    public synchronized boolean cancelOrder(String orderId, String userUuid) {
        if (isBlank(orderId) || isBlank(userUuid)) {
            return false;
        }

        // 查询订单
        ShopOrder order = shopDao.findOrderById(orderId);
        if (order == null) {
            return false;
        }

        // 验证订单所有权
        if (!userUuid.equals(order.getoUserUuid())) {
            return false;
        }

        if (order.getoStatus() == ShopOrderStatus.UNPAID) {
            return shopDao.updateOrderStatus(orderId, ShopOrderStatus.CANCELLED);
        }

        if (order.getoStatus() != ShopOrderStatus.PAID) {
            return false;
        }

        // 退款
        String remark = "订单取消退款 - 订单:" + orderId;
        BankTransaction transaction = bankAdapter.refund(userUuid, order.getoTotal(), orderId, remark);
        if (transaction == null) {
            return false;
        }

        // 恢复库存
        shopDao.reduceStock(order.getoItemId(), -order.getoQuantity());

        // 更新订单状态为已取消
        return shopDao.updateOrderStatus(orderId, ShopOrderStatus.CANCELLED);
    }

    private ShopOrder buildOrder(String userUuid, ShopItem item, int quantity) {
        BigDecimal total = item.getSiPrice().multiply(BigDecimal.valueOf(quantity));
        ShopOrder order = new ShopOrder();
        order.setoId(nextOrderId());
        order.setoUserUuid(userUuid);
        order.setoItemId(item.getSiId());
        order.setoShopId(item.getSiShopId());
        order.setoQuantity(quantity);
        order.setoTotal(total);
        order.setoTime(new Date());
        order.setoStatus(STATUS_UNPAID);
        return order;
    }

    private String nextOrderId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 按订单ID查询订单。
     *
     * @param orderId 订单ID
     * @return 对应订单；不存在时返回 null
     */
    public ShopOrder findOrderById(String orderId) {
        if (isBlank(orderId)) {
            return null;
        }
        return shopDao.findOrderById(orderId);
    }

    // ==================== 管理员功能 ====================

    /**
     * 管理员推进订单状态（如发货、完成）。
     *
     * @param orderId   订单ID
     * @param newStatus 目标状态
     * @return 更新成功返回 true；订单不存在或状态转换不合法时返回 false
     */
    public boolean advanceOrderStatus(String orderId, ShopOrderStatus newStatus) {
        if (isBlank(orderId) || newStatus == null) {
            return false;
        }

        ShopOrder order = shopDao.findOrderById(orderId);
        if (order == null) {
            return false;
        }

        // 检查状态转换的合法性
        ShopOrderStatus currentStatus = order.getoStatus();
        if (!isValidStatusTransition(currentStatus, newStatus)) {
            return false;
        }

        return shopDao.updateOrderStatus(orderId, newStatus);
    }

    private boolean isValidStatusTransition(ShopOrderStatus from, ShopOrderStatus to) {
        if (from == to) {
            return false; // 不允许设置为相同状态
        }

        switch (from) {
        case PAID:
            return to == ShopOrderStatus.SHIPPED;
        case SHIPPED:
            return to == ShopOrderStatus.COMPLETED;
        default:
            return false;
        }
    }

    /**
     * 管理员新增或更新商品。
     * 如果商品ID为空或不存在，则新增；否则更新现有商品。
     *
     * @param item 商品信息
     * @return 操作成功返回 true
     */
    public boolean upsertItem(ShopItem item) {
        if (item == null || isBlank(item.getSiName()) || item.getSiPrice().compareTo(BigDecimal.ZERO) < 0 || item.getSiStock() < 0) {
            return false;
        }

        // 如果没有ID，生成新ID（新增商品）
        if (isBlank(item.getSiId())) {
            item.setSiId(nextOrderId());
            return shopDao.insertItem(item);
        }

        // 如果有ID，检查是否存在
        ShopItem existing = shopDao.findItemById(item.getSiId());
        if (existing == null) {
            // ID不存在，作为新商品插入
            return shopDao.insertItem(item);
        } else {
            // ID存在，更新商品
            return shopDao.updateItem(item);
        }
    }

    /**
     * 管理员查询所有订单（分页）。
     *
     * @param query 查询条件（包含分页参数、状态筛选等）
     * @return 订单列表响应
     */
    public OrderListResponse queryAllOrders(OrderQuery query) {
        if (query == null) {
            query = new OrderQuery();
        }
        return shopDao.queryAllOrders(query);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private synchronized void ensureCatalog() {
        // 不能只看「本进程补过一次」这个标志：库被重建（重新执行 sql/vCampus.sql）之后，
        // 那 30 件商品不会自己回来，界面就一直是空的（现象：日志里「成功读取 0 条商品记录」）。
        // 所以先看库里到底有没有，空了就再补一次；ShopCatalogBootstrap.ensure 本身按 siId 去重。
        if (!catalogReady || shopDao.findAllItems().isEmpty()) {
            catalogReady = ShopCatalogBootstrap.ensure(shopDao);
        }
    }
}
