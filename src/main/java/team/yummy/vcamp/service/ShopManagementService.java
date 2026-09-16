package team.yummy.vcamp.service;

import team.yummy.vcamp.dao.ShopDao;
import team.yummy.vcamp.entity.Shop;
import team.yummy.vcamp.entity.ShopItem;
import team.yummy.vcamp.entity.Order;
import team.yummy.vcamp.util.Result;

import java.util.List;
import java.util.UUID;

/**
 * 商店管理服务
 * 提供商店的 CRUD 操作和权限隔离
 * 参考 BankService 的设计模式：每个店主管理自己的商店
 */
public class ShopManagementService {
    private final ShopDao shopDao;

    public ShopManagementService(ShopDao shopDao) {
        this.shopDao = shopDao;
    }

    /**
     * 创建新商店
     * @param ownerUuid 店主的用户UUID
     * @param shopId 商店ID（业务ID）
     * @param shopName 商店名称
     * @param description 商店描述
     * @return 创建结果
     */
    public Result<Shop> createShop(String ownerUuid, String shopId, String shopName, String description) {
        // 参数校验
        if (ownerUuid == null || ownerUuid.trim().isEmpty()) {
            return Result.error("店主UUID不能为空");
        }
        if (shopId == null || shopId.trim().isEmpty()) {
            return Result.error("商店ID不能为空");
        }
        if (shopName == null || shopName.trim().isEmpty()) {
            return Result.error("商店名称不能为空");
        }

        // 检查商店ID是否已存在
        Shop existing = shopDao.findShopById(shopId);
        if (existing != null) {
            return Result.error("商店ID已存在: " + shopId);
        }

        // 创建商店对象
        Shop shop = new Shop();
        shop.setShopId(shopId);
        shop.setShopName(shopName);
        shop.setShopDescription(description);
        shop.setShopOwnerUuid(ownerUuid);
        shop.setShopEnabled(true);

        // 保存到数据库
        boolean success = shopDao.addShop(shop);
        if (!success) {
            return Result.error("创建商店失败");
        }

        return Result.success(shop);
    }

    /**
     * 更新商店信息
     * @param shopId 商店ID
     * @param ownerUuid 操作者UUID（用于权限校验）
     * @param shopName 新的商店名称
     * @param description 新的商店描述
     * @param enabled 是否启用
     * @return 更新结果
     */
    public Result<Shop> updateShop(String shopId, String ownerUuid, String shopName,
                                   String description, Boolean enabled) {
        // 查找商店
        Shop shop = shopDao.findShopById(shopId);
        if (shop == null) {
            return Result.error("商店不存在: " + shopId);
        }

        // 权限校验：只有店主可以修改
        if (!shop.getShopOwnerUuid().equals(ownerUuid)) {
            return Result.error("无权修改该商店");
        }

        // 更新字段
        if (shopName != null && !shopName.trim().isEmpty()) {
            shop.setShopName(shopName);
        }
        if (description != null) {
            shop.setShopDescription(description);
        }
        if (enabled != null) {
            shop.setShopEnabled(enabled);
        }

        // 保存到数据库
        boolean success = shopDao.updateShop(shop);
        if (!success) {
            return Result.error("更新商店失败");
        }

        return Result.success(shop);
    }

    /**
     * 删除商店
     * @param shopId 商店ID
     * @param ownerUuid 操作者UUID（用于权限校验）
     * @return 删除结果
     */
    public Result<Void> deleteShop(String shopId, String ownerUuid) {
        // 查找商店
        Shop shop = shopDao.findShopById(shopId);
        if (shop == null) {
            return Result.error("商店不存在: " + shopId);
        }

        // 权限校验：只有店主可以删除
        if (!shop.getShopOwnerUuid().equals(ownerUuid)) {
            return Result.error("无权删除该商店");
        }

        // 检查是否有商品（可选：是否允许删除有商品的商店）
        List<ShopItem> items = shopDao.findItemsByShop(shopId);
        if (!items.isEmpty()) {
            return Result.error("商店中还有商品，无法删除");
        }

        // 删除商店
        boolean success = shopDao.deleteShop(shopId);
        if (!success) {
            return Result.error("删除商店失败");
        }

        return Result.success(null);
    }

    /**
     * 查询用户拥有的所有商店
     * @param ownerUuid 店主UUID
     * @return 商店列表
     */
    public Result<List<Shop>> getMyShops(String ownerUuid) {
        if (ownerUuid == null || ownerUuid.trim().isEmpty()) {
            return Result.error("用户UUID不能为空");
        }

        List<Shop> shops = shopDao.findShopsByOwner(ownerUuid);
        return Result.success(shops);
    }

    /**
     * 查询商店详情
     * @param shopId 商店ID
     * @return 商店信息
     */
    public Result<Shop> getShopById(String shopId) {
        if (shopId == null || shopId.trim().isEmpty()) {
            return Result.error("商店ID不能为空");
        }

        Shop shop = shopDao.findShopById(shopId);
        if (shop == null) {
            return Result.error("商店不存在: " + shopId);
        }

        return Result.success(shop);
    }

    /**
     * 查询所有商店（管理员功能）
     * @return 所有商店列表
     */
    public Result<List<Shop>> getAllShops() {
        List<Shop> shops = shopDao.findAllShops();
        return Result.success(shops);
    }

    /**
     * 查询商店的所有商品
     * @param shopId 商店ID
     * @return 商品列表
     */
    public Result<List<ShopItem>> getShopItems(String shopId) {
        // 检查商店是否存在
        Shop shop = shopDao.findShopById(shopId);
        if (shop == null) {
            return Result.error("商店不存在: " + shopId);
        }

        List<ShopItem> items = shopDao.findItemsByShop(shopId);
        return Result.success(items);
    }

    /**
     * 查询商店的所有订单
     * @param shopId 商店ID
     * @param ownerUuid 操作者UUID（用于权限校验）
     * @return 订单列表
     */
    public Result<List<Order>> getShopOrders(String shopId, String ownerUuid) {
        // 查找商店
        Shop shop = shopDao.findShopById(shopId);
        if (shop == null) {
            return Result.error("商店不存在: " + shopId);
        }

        // 权限校验：只有店主可以查看订单
        if (!shop.getShopOwnerUuid().equals(ownerUuid)) {
            return Result.error("无权查看该商店的订单");
        }

        List<Order> orders = shopDao.findOrdersByShop(shopId);
        return Result.success(orders);
    }

    /**
     * 校验用户是否是指定商店的店主
     * @param shopId 商店ID
     * @param ownerUuid 用户UUID
     * @return 是否是店主
     */
    public boolean isShopOwner(String shopId, String ownerUuid) {
        Shop shop = shopDao.findShopById(shopId);
        return shop != null && shop.getShopOwnerUuid().equals(ownerUuid);
    }
}
