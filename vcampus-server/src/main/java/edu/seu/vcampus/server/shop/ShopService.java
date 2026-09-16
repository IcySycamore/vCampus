package edu.seu.vcampus.server.shop;

import edu.seu.vcampus.common.shop.entity.ShopItem;
import edu.seu.vcampus.common.shop.entity.ShopOrder;
import edu.seu.vcampus.common.shop.entity.ShopOrderStatus;
import edu.seu.vcampus.common.shop.dto.OrderListResponse;
import edu.seu.vcampus.common.shop.dto.OrderQuery;
import edu.seu.vcampus.common.bank.entity.BankTransaction;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * 商店业务逻辑：商品浏览、购买下单与订单查询。
 *
 * <p>金额一律由本层按"单价 × 数量"计算，客户端提交的金额不予采信；库存扣减
 * 依赖 {@link ShopDao#reduceStock} 的原子语义，扣减失败即视为库存不足，不再落单。
 */
public class ShopService {

    /** 订单初始状态。 */
    private static final ShopOrderStatus STATUS_UNPAID = ShopOrderStatus.UNPAID;

    /** 数据访问对象。 */
    private final ShopDao shopDao;

    /** 银行适配器。 */
    private final BankAdapter bankAdapter;

    /**
     * 使用默认的数据访问实现构造服务。
     */
    public ShopService() {
        this(new ShopDaoImpl(), new BankAdapter());
    }

    /**
     * 使用指定的数据访问对象构造服务（便于测试时注入替身）。
     *
     * @param shopDao 数据访问对象
     */
    public ShopService(ShopDao shopDao) {
        this(shopDao, new BankAdapter());
    }

    /**
     * 使用指定的数据访问对象和银行适配器构造服务（用于测试）。
     *
     * @param shopDao 数据访问对象
     * @param bankAdapter 银行适配器
     */
    public ShopService(ShopDao shopDao, BankAdapter bankAdapter) {
        this.shopDao = shopDao;
        this.bankAdapter = bankAdapter;
    }

    /**
     * 查询全部商品，供客户端展示商品列表。
     *
     * @return 商品列表；无数据时为空列表
     */
    public List<ShopItem> listItems() {
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
     * <p>库存扣减成功后才写入订单;若订单写入失败,已扣减的库存会被回补,
     * 避免出现"扣了库存却没有订单"的情况。
     *
     * @param userId 下单用户的登录ID
     * @param itemId 商品ID
     * @param quantity 购买数量,须大于 0
     * @return 下单成功返回生成的订单;参数非法、商品不存在或库存不足时返回 null
     */
    public ShopOrder purchase(String userUuid, String itemId, int quantity) {
        if (isBlank(userUuid) || isBlank(itemId) || quantity <= 0) {
            return null;
        }
        ShopItem item = shopDao.findItemById(itemId);
        if (item == null || item.getSiPrice() == null || item.getSiStock() == null
            || item.getSiStock() < quantity) {
            return null;
        }
        if (!shopDao.reduceStock(itemId, quantity)) {
            return null;
        }
        ShopOrder order = buildOrder(userUuid, item, quantity);
        if (!shopDao.addOrder(order)) {
            shopDao.reduceStock(itemId, -quantity);
            return null;
        }
        return order;
    }

    /**
     * 查询指定用户的订单,按下单时间倒序。
     *
     * @param userId 用户登录ID
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
        if (isBlank(userUuid)) {
            return new OrderListResponse(new ArrayList<ShopOrder>(),
                query.getPageNumber(), query.getPageSize(), 0);
        }

        List<ShopOrder> orders = shopDao.findOrdersByUserPaged(
            userUuid, query.getPageNumber(), query.getPageSize());
        long totalCount = shopDao.countOrdersByUser(userUuid);

        return new OrderListResponse(orders,
            query.getPageNumber(), query.getPageSize(), totalCount);
    }

    /**
     * 支付订单:扣除用户账户余额并更新订单状态为已支付。
     *
     * <p>仅支持状态为UNPAID的订单。支付成功后订单状态更新为PAID。
     * 如果银行扣款失败,订单状态不变。
     *
     * @param orderId 订单ID
     * @param userUuid 用户UUID(用于验证订单所有权)
     * @return 支付成功返回true,失败返回false
     */
    public boolean payOrder(String orderId, String userUuid) {
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

        // 验证订单状态
        if (order.getoStatus() != ShopOrderStatus.UNPAID) {
            return false;
        }

        // 扣款
        String remark = "购买商品 - 订单:" + orderId;
        BankTransaction transaction = bankAdapter.deduct(userUuid, order.getoTotal(), orderId, remark);
        if (transaction == null) {
            return false;
        }

        // 更新订单状态为已支付
        return shopDao.updateOrderStatus(orderId, ShopOrderStatus.PAID);
    }

    /**
     * 取消订单:退还金额并更新订单状态为已取消。
     *
     * <p>仅支持状态为PAID的订单。取消成功后订单状态更新为CANCELLED,
     * 并退还订单金额到用户账户,同时恢复商品库存。
     *
     * @param orderId 订单ID
     * @param userUuid 用户UUID(用于验证订单所有权)
     * @return 取消成功返回true,失败返回false
     */
    public boolean cancelOrder(String orderId, String userUuid) {
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

        // 验证订单状态(只有已支付的订单才能取消)
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

    /**
     * 按"单价 × 数量"组装订单对象,总价在服务端计算。
     *
     * @param userId 下单用户的登录ID
     * @param item 商品
     * @param quantity 购买数量
     * @return 待落库的订单
     */
    private ShopOrder buildOrder(String userUuid, ShopItem item, int quantity) {
        BigDecimal total = item.getSiPrice().multiply(BigDecimal.valueOf(quantity));
        ShopOrder order = new ShopOrder();
        order.setoId(nextOrderId());
        order.setoUserUuid(userUuid);
        order.setoItemId(item.getSiId());
        order.setoQuantity(quantity);
        order.setoTotal(total);
        order.setoTime(new Date());
        order.setoStatus(STATUS_UNPAID);
        return order;
    }

    /**
     * 生成订单ID（32 位以内，去掉 UUID 中的连字符）。
     *
     * @return 订单ID
     */
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

    /**
     * 检查订单状态转换是否合法。
     * 合法路径：PAID → SHIPPED → COMPLETED
     *
     * @param from 当前状态
     * @param to   目标状态
     * @return 转换合法返回 true
     */
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

    /**
     * 判断字符串是否为空或仅含空白字符。
     *
     * @param value 待判断的字符串
     * @return 为 null、空串或全空白时返回 true
     */
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
