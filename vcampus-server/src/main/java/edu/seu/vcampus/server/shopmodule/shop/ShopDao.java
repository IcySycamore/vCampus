package edu.seu.vcampus.server.shopmodule.shop;

import edu.seu.vcampus.common.shop.Order;
import edu.seu.vcampus.common.shop.ShopItem;
import java.util.List;

/**
 * 商店数据访问接口：商品与订单的持久化操作。
 *
 * <p>本接口只负责数据存取，购买流程中的库存校验与金额计算由
 * {@link ShopService} 承担；查询未命中一律返回 {@code null}，
 * 列表查询无数据时返回空列表而非 {@code null}。
 */
public interface ShopDao {

    /**
     * 查询全部商品。
     *
     * @return 商品列表；无数据时为空列表
     */
    List<ShopItem> findAllItems();

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
    boolean addOrder(Order order);

    /**
     * 查询指定用户的订单，按下单时间倒序。
     *
     * @param userId 用户登录ID
     * @return 订单列表；无数据时为空列表
     */
    List<Order> findOrdersByUser(String userUuid);
}
