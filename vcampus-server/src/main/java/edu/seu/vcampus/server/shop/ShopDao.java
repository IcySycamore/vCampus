package edu.seu.vcampus.server.shop;

import edu.seu.vcampus.common.shop.entity.Shop;
import edu.seu.vcampus.common.shop.entity.ShopOrder;
import edu.seu.vcampus.common.shop.entity.ShopItem;
import edu.seu.vcampus.common.shop.dto.OrderQuery;
import edu.seu.vcampus.common.shop.dto.OrderListResponse;
import java.util.List;

/**
 * 商店数据访问接口：商店、商品与订单的持久化操作。
 *
 * <p>本接口只负责数据存取，购买流程中的库存校验与金额计算由
 * {@link ShopService} 承担；查询未命中一律返回 {@code null}，
 * 列表查询无数据时返回空列表而非 {@code null}。
 */
public interface ShopDao {

    // ========== 商店管理 ==========

    /**
     * 创建新商店。
     *
     * @param shop 待创建的商店
     * @return 创建成功返回 true
     */
    boolean addShop(Shop shop);

    /**
     * 更新商店信息（店主操作）。
     *
     * @param shop 含新值的商店，以商店ID定位记录
     * @return 更新成功返回 true
     */
    boolean updateShop(Shop shop);

    /**
     * 按商店ID删除商店。
     *
     * @param shopId 商店ID
     * @return 删除成功返回 true
     */
    boolean deleteShop(String shopId);

    /**
     * 按商店ID查询商店。
     *
     * @param shopId 商店ID
     * @return 对应商店；不存在时返回 null
     */
    Shop findShopById(String shopId);

    /**
     * 查询指定用户拥有的所有商店。
     *
     * @param ownerUuid 店主UUID
     * @return 商店列表；无数据时为空列表
     */
    List<Shop> findShopsByOwner(String ownerUuid);

    /**
     * 查询所有商店。
     *
     * @return 商店列表；无数据时为空列表
     */
    List<Shop> findAllShops();

    // ========== 商品管理 ==========

    /**
     * 查询全部商品（所有商店）。
     *
     * @return 商品列表；无数据时为空列表
     */
    List<ShopItem> findAllItems();

    /**
     * 查询指定商店的所有商品。
     *
     * @param shopId 商店ID
     * @return 商品列表；无数据时为空列表
     */
    List<ShopItem> findItemsByShop(String shopId);

    /**
     * 按商品ID查询商品。
     *
     * @param itemId 商品ID
     * @return 对应商品；不存在时返回 null
     */
    ShopItem findItemById(String itemId);

    /**
     * 新增商品（管理员操作）。
     *
     * @param item 待新增的商品
     * @return 新增成功返回 true
     */
    boolean addItem(ShopItem item);

    /**
     * 更新商品信息（管理员操作）。
     *
     * @param item 含新值的商品，以商品ID定位记录
     * @return 更新成功返回 true
     */
    boolean updateItem(ShopItem item);

    /**
     * 按商品ID删除商品（管理员操作）。
     *
     * @param itemId 商品ID
     * @return 删除成功返回 true
     */
    boolean deleteItem(String itemId);

    /**
     * 扣减库存：仅当当前库存足够时才扣减，用于避免超卖。
     *
     * @param itemId 商品ID
     * @param quantity 扣减数量
     * @return 库存充足并扣减成功返回 true；库存不足或商品不存在返回 false
     */
    boolean reduceStock(String itemId, int quantity);

    /**
     * 新增订单。
     *
     * @param order 待新增的订单，总价须由服务层计算后填入
     * @return 新增成功返回 true
     */
    boolean addOrder(ShopOrder order);

    /**
     * 查询指定用户的订单，按下单时间倒序。
     *
     * @param userId 用户登录ID
     * @return 订单列表；无数据时为空列表
     */
    List<ShopOrder> findOrdersByUser(String userUuid);

    /**
     * 查询指定商店的所有订单，按下单时间倒序。
     *
     * @param shopId 商店ID
     * @return 订单列表；无数据时为空列表
     */
    List<ShopOrder> findOrdersByShop(String shopId);

    /**
     * 分页查询指定用户的订单，按下单时间倒序。
     *
     * @param userUuid 用户UUID
     * @param pageNumber 页码，从1开始
     * @param pageSize 每页记录数
     * @return 订单列表；无数据时为空列表
     */
    List<ShopOrder> findOrdersByUserPaged(String userUuid, int pageNumber, int pageSize);

    /**
     * 统计指定用户的订单总数。
     *
     * @param userUuid 用户UUID
     * @return 订单总数
     */
    long countOrdersByUser(String userUuid);

    /**
     * 按订单ID查询订单。
     *
     * @param orderId 订单ID
     * @return 对应订单；不存在时返回 null
     */
    ShopOrder findOrderById(String orderId);

    /**
     * 更新订单状态。
     *
     * @param orderId 订单ID
     * @param status 新的订单状态
     * @return 更新成功返回 true
     */
    boolean updateOrderStatus(String orderId, edu.seu.vcampus.common.shop.entity.ShopOrderStatus status);

    // ========== 管理员功能 ==========

    /**
     * 新增商品（用于管理员的 upsert 操作）。
     *
     * @param item 待新增的商品
     * @return 新增成功返回 true
     */
    boolean insertItem(ShopItem item);

    /**
     * 管理员查询所有订单（分页）。
     *
     * @param query 查询条件（包含分页参数、状态筛选等）
     * @return 订单列表响应
     */
    OrderListResponse queryAllOrders(OrderQuery query);
}
