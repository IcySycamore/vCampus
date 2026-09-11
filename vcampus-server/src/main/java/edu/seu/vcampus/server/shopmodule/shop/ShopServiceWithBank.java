package edu.seu.vcampus.server.shopmodule.shop;

import edu.seu.vcampus.common.bank.BankService;
import edu.seu.vcampus.common.shop.Order;
import edu.seu.vcampus.common.shop.ShopItem;
import edu.seu.vcampus.server.bankmodule.BankServiceImpl;
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
 *
 * <p>集成银行服务，购买时自动从用户银行账户扣款。
 */
public class ShopServiceWithBank {

    /** 订单初始状态。 */
    private static final String STATUS_UNPAID = "待支付";

    /** 订单已支付状态。 */
    private static final String STATUS_PAID = "已支付";

    /** 数据访问对象。 */
    private final ShopDao shopDao;

    /** 银行服务对象。 */
    private final BankService bankService;

    /**
     * 使用默认的数据访问实现构造服务。
     */
    public ShopServiceWithBank() {
        this(new ShopDaoImpl(), new BankServiceImpl());
    }

    /**
     * 使用指定的数据访问对象构造服务（便于测试时注入替身）。
     *
     * @param shopDao 数据访问对象
     * @param bankService 银行服务对象
     */
    public ShopServiceWithBank(ShopDao shopDao, BankService bankService) {
        this.shopDao = shopDao;
        this.bankService = bankService;
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
     * 购买商品并支付：校验参数与库存，计算总价，扣减银行账户余额，生成订单。
     *
     * <p>流程：
     * 1. 校验参数和商品库存
     * 2. 检查用户银行账户余额是否充足
     * 3. 扣减库存
     * 4. 从银行账户扣款
     * 5. 生成已支付订单
     *
     * <p>任何步骤失败都会回滚已执行的操作。
     *
     * @param userUuid 下单用户的全局UUID
     * @param itemId 商品ID
     * @param quantity 购买数量，须大于 0
     * @return 下单成功返回生成的订单；参数非法、商品不存在、库存不足、余额不足或支付失败时返回 null
     */
    public Order purchaseWithPayment(String userUuid, String itemId, int quantity) {
        // 1. 校验参数
        if (isBlank(userUuid) || isBlank(itemId) || quantity <= 0) {
            return null;
        }

        // 2. 查询商品信息
        ShopItem item = shopDao.findItemById(itemId);
        if (item == null || item.getSiPrice() == null || item.getSiStock() == null
            || item.getSiStock() < quantity) {
            return null;
        }

        // 3. 计算总价
        BigDecimal totalAmount = item.getSiPrice().multiply(BigDecimal.valueOf(quantity));

        // 4. 检查银行账户余额
        if (!bankService.hasSufficientBalance(userUuid, totalAmount)) {
            System.err.println("用户 " + userUuid + " 余额不足，需要: " + totalAmount);
            return null;
        }

        // 5. 扣减库存
        if (!shopDao.reduceStock(itemId, quantity)) {
            System.err.println("商品 " + itemId + " 库存扣减失败");
            return null;
        }

        // 6. 从银行账户扣款
        if (!bankService.deduct(userUuid, totalAmount)) {
            // 扣款失败，回补库存
            shopDao.reduceStock(itemId, -quantity);
            System.err.println("用户 " + userUuid + " 扣款失败，已回滚库存");
            return null;
        }

        // 7. 生成已支付订单
        Order order = buildOrder(userUuid, item, quantity, totalAmount, STATUS_PAID);
        if (!shopDao.addOrder(order)) {
            // 订单创建失败，退款并回补库存
            bankService.deposit(userUuid, totalAmount);
            shopDao.reduceStock(itemId, -quantity);
            System.err.println("订单创建失败，已退款并回滚库存");
            return null;
        }

        return order;
    }

    /**
     * 购买商品（不支付）：校验参数与库存，计算总价并生成待支付订单。
     *
     * <p>库存扣减成功后才写入订单；若订单写入失败，已扣减的库存会被回补，
     * 避免出现"扣了库存却没有订单"的情况。
     *
     * @param userUuid 下单用户的全局UUID
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
        BigDecimal totalAmount = item.getSiPrice().multiply(BigDecimal.valueOf(quantity));
        Order order = buildOrder(userUuid, item, quantity, totalAmount, STATUS_UNPAID);
        if (!shopDao.addOrder(order)) {
            shopDao.reduceStock(itemId, -quantity);
            return null;
        }
        return order;
    }

    /**
     * 查询指定用户的订单，按下单时间倒序。
     *
     * @param userUuid 用户全局UUID
     * @return 订单列表；用户UUID为空时返回空列表
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
     * @param userUuid 下单用户的全局UUID
     * @param item 商品
     * @param quantity 购买数量
     * @param totalAmount 订单总价
     * @param status 订单状态
     * @return 待落库的订单
     */
    private Order buildOrder(String userUuid, ShopItem item, int quantity,
                            BigDecimal totalAmount, String status) {
        Order order = new Order();
        order.setoId(nextOrderId());
        order.setoUserUuid(userUuid);
        order.setoItemId(item.getSiId());
        order.setoQuantity(quantity);
        order.setoTotal(totalAmount);
        order.setoTime(new Date());
        order.setoStatus(status);
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
