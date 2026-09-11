package edu.seu.vcampus.server.shopmodule.shop;

import edu.seu.vcampus.common.shop.Order;
import edu.seu.vcampus.common.shop.ShopItem;
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
    private static final String STATUS_UNPAID = "待支付";

    /** 数据访问对象。 */
    private final ShopDao shopDao;

    /**
     * 使用默认的数据访问实现构造服务。
     */
    public ShopService() {
        this(new ShopDaoImpl());
    }

    /**
     * 使用指定的数据访问对象构造服务（便于测试时注入替身）。
     *
     * @param shopDao 数据访问对象
     */
    public ShopService(ShopDao shopDao) {
        this.shopDao = shopDao;
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
     * 购买商品：校验参数与库存，计算总价并生成订单。
     *
     * <p>库存扣减成功后才写入订单；若订单写入失败，已扣减的库存会被回补，
     * 避免出现"扣了库存却没有订单"的情况。
     *
     * @param userId 下单用户的登录ID
     * @param itemId 商品ID
     * @param quantity 购买数量，须大于 0
     * @return 下单成功返回生成的订单；参数非法、商品不存在或库存不足时返回 null
     */
    public Order purchase(String userUuid, String itemId, int quantity) {
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
        Order order = buildOrder(userUuid, item, quantity);
        if (!shopDao.addOrder(order)) {
            shopDao.reduceStock(itemId, -quantity);
            return null;
        }
        return order;
    }

    /**
     * 查询指定用户的订单，按下单时间倒序。
     *
     * @param userId 用户登录ID
     * @return 订单列表；用户ID为空时返回空列表
     */
    public List<Order> listOrdersOfUser(String userUuid) {
        if (isBlank(userUuid)) {
            return new ArrayList<>();
        }
        return shopDao.findOrdersByUser(userUuid);
    }

    /**
     * 按"单价 × 数量"组装订单对象，总价在服务端计算。
     *
     * @param userId 下单用户的登录ID
     * @param item 商品
     * @param quantity 购买数量
     * @return 待落库的订单
     */
    private Order buildOrder(String userUuid, ShopItem item, int quantity) {
        BigDecimal total = item.getSiPrice().multiply(BigDecimal.valueOf(quantity));
        Order order = new Order();
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
     * 判断字符串是否为空或仅含空白字符。
     *
     * @param value 待判断的字符串
     * @return 为 null、空串或全空白时返回 true
     */
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
