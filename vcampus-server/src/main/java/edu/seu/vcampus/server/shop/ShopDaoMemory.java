package edu.seu.vcampus.server.shop;

import edu.seu.vcampus.common.shop.entity.Shop;
import edu.seu.vcampus.common.shop.entity.ShopItem;
import edu.seu.vcampus.common.shop.entity.ShopOrder;
import edu.seu.vcampus.common.shop.entity.ShopOrderStatus;
import edu.seu.vcampus.common.shop.dto.OrderQuery;
import edu.seu.vcampus.common.shop.dto.OrderListResponse;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 商店数据访问的内存实现，不依赖数据库。
 *
 * <p>用于在数据库就绪前打通 Service / 消息链路，并提供待支付订单的数量编辑能力。
 *
 * <p>线程安全：所有写读操作都以 {@code synchronized} 保证互斥，尤其
 * {@link #reduceStock} 的"检查库存 + 扣减"必须原子，避免并发支付超卖。
 *
     * <p>构造时会写入一批单店铺演示种子数据，
 * 便于本地直接浏览商品、走通下单流程；不需要时删掉 {@link #seed()} 调用即可。
 */
public class ShopDaoMemory implements ShopDao, ShopOrderQuantityStore {

    /** 商店存储：shopId → Shop。 */
    private final Map<String, Shop> m_shops = new LinkedHashMap<String, Shop>();

    /** 商品存储：siId → ShopItem。 */
    private final Map<String, ShopItem> m_items = new LinkedHashMap<String, ShopItem>();

    /** 订单存储：oId → ShopOrder。 */
    private final Map<String, ShopOrder> m_orders = new LinkedHashMap<String, ShopOrder>();

    /** 订单按下单时间倒序的比较器（时间相同则保持稳定）。 */
    private static final Comparator<ShopOrder> ORDER_BY_TIME_DESC =
            new Comparator<ShopOrder>() {
                @Override
                public int compare(ShopOrder left, ShopOrder right) {
                    long l = left.getoTime() == null ? Long.MIN_VALUE
                            : left.getoTime().getTime();
                    long r = right.getoTime() == null ? Long.MIN_VALUE
                            : right.getoTime().getTime();
                    return l == r ? 0 : l < r ? 1 : -1;
                }
            };

    /**
     * 构造内存实现并写入演示种子数据。
     */
    public ShopDaoMemory() {
        seed();
    }

    // ========== 商店管理 ==========

    @Override
    public synchronized boolean addShop(Shop shop) {
        if (shop == null || isBlank(shop.getShopId())) {
            return false;
        }
        if (m_shops.containsKey(shop.getShopId())) {
            return false;
        }
        m_shops.put(shop.getShopId(), shop);
        return true;
    }

    @Override
    public synchronized boolean updateShop(Shop shop) {
        if (shop == null || isBlank(shop.getShopId())) {
            return false;
        }
        if (!m_shops.containsKey(shop.getShopId())) {
            return false;
        }
        m_shops.put(shop.getShopId(), shop);
        return true;
    }

    @Override
    public synchronized boolean deleteShop(String shopId) {
        if (isBlank(shopId)) {
            return false;
        }
        return m_shops.remove(shopId) != null;
    }

    @Override
    public synchronized Shop findShopById(String shopId) {
        return isBlank(shopId) ? null : m_shops.get(shopId);
    }

    @Override
    public synchronized List<Shop> findShopsByOwner(String ownerUuid) {
        List<Shop> result = new ArrayList<Shop>();
        for (Shop shop : m_shops.values()) {
            if (same(shop.getShopOwnerUuid(), ownerUuid)) {
                result.add(shop);
            }
        }
        return result;
    }

    @Override
    public synchronized List<Shop> findAllShops() {
        return new ArrayList<Shop>(m_shops.values());
    }

    // ========== 商品管理 ==========

    @Override
    public synchronized List<ShopItem> findAllItems() {
        return new ArrayList<ShopItem>(m_items.values());
    }

    @Override
    public synchronized List<ShopItem> findItemsByShop(String shopId) {
        List<ShopItem> result = new ArrayList<ShopItem>();
        for (ShopItem item : m_items.values()) {
            if (same(item.getSiShopId(), shopId)) {
                result.add(item);
            }
        }
        return result;
    }

    @Override
    public synchronized ShopItem findItemById(String itemId) {
        return isBlank(itemId) ? null : m_items.get(itemId);
    }

    @Override
    public synchronized boolean addItem(ShopItem item) {
        if (item == null || isBlank(item.getSiId())) {
            return false;
        }
        if (m_items.containsKey(item.getSiId())) {
            return false;
        }
        if (isBlank(item.getSiUuid())) {
            item.setSiUuid(UUID.randomUUID().toString());
        }
        m_items.put(item.getSiId(), item);
        return true;
    }

    @Override
    public synchronized boolean updateItem(ShopItem item) {
        if (item == null || isBlank(item.getSiId())) {
            return false;
        }
        if (!m_items.containsKey(item.getSiId())) {
            return false;
        }
        m_items.put(item.getSiId(), item);
        return true;
    }

    @Override
    public synchronized boolean deleteItem(String itemId) {
        if (isBlank(itemId)) {
            return false;
        }
        return m_items.remove(itemId) != null;
    }

    @Override
    public synchronized boolean reduceStock(String itemId, int quantity) {
        ShopItem item = isBlank(itemId) ? null : m_items.get(itemId);
        if (item == null || item.getSiStock() == null) {
            return false;
        }
        // 与 JDBC 实现一致：当前库存足够时才扣减，避免超卖；负数用于回补库存。
        if (item.getSiStock() < quantity) {
            return false;
        }
        item.setSiStock(item.getSiStock() - quantity);
        return true;
    }

    // ========== 订单管理 ==========

    @Override
    public synchronized boolean addOrder(ShopOrder order) {
        if (order == null || isBlank(order.getoId())) {
            return false;
        }
        ShopOrder existing = findMergeableOrder(order);
        if (existing != null) {
            if (!hasStockForMerge(existing, order)) {
                return false;
            }
            mergeOrder(existing, order);
            return true;
        }
        if (m_orders.containsKey(order.getoId())) {
            return false;
        }
        m_orders.put(order.getoId(), order);
        return true;
    }

    @Override
    public synchronized List<ShopOrder> findOrdersByUser(String userUuid) {
        List<ShopOrder> result = new ArrayList<ShopOrder>();
        for (ShopOrder order : m_orders.values()) {
            if (same(order.getoUserUuid(), userUuid)) {
                result.add(order);
            }
        }
        java.util.Collections.sort(result, ORDER_BY_TIME_DESC);
        return result;
    }

    @Override
    public synchronized List<ShopOrder> findOrdersByShop(String shopId) {
        List<ShopOrder> result = new ArrayList<ShopOrder>();
        for (ShopOrder order : m_orders.values()) {
            if (same(order.getoShopId(), shopId)) {
                result.add(order);
            }
        }
        java.util.Collections.sort(result, ORDER_BY_TIME_DESC);
        return result;
    }

    @Override
    public synchronized List<ShopOrder> findOrdersByUserPaged(String userUuid,
            int pageNumber, int pageSize) {
        List<ShopOrder> all = findOrdersByUser(userUuid);
        int offset = (pageNumber - 1) * pageSize;
        if (offset >= all.size()) {
            return new ArrayList<ShopOrder>();
        }
        int end = Math.min(offset + pageSize, all.size());
        return new ArrayList<ShopOrder>(all.subList(offset, end));
    }

    @Override
    public synchronized long countOrdersByUser(String userUuid) {
        long count = 0;
        for (ShopOrder order : m_orders.values()) {
            if (same(order.getoUserUuid(), userUuid)) {
                count++;
            }
        }
        return count;
    }

    @Override
    public synchronized ShopOrder findOrderById(String orderId) {
        return isBlank(orderId) ? null : m_orders.get(orderId);
    }

    @Override
    public synchronized boolean updateOrderStatus(String orderId, ShopOrderStatus status) {
        if (isBlank(orderId) || status == null) {
            return false;
        }
        ShopOrder order = m_orders.get(orderId);
        if (order == null) {
            return false;
        }
        order.setoStatus(status);
        return true;
    }

    @Override
    public synchronized boolean updateUnpaidOrderQuantity(String orderId, int quantity,
            BigDecimal total) {
        if (isBlank(orderId) || quantity <= 0 || total == null) {
            return false;
        }
        ShopOrder order = m_orders.get(orderId);
        if (order == null || order.getoStatus() != ShopOrderStatus.UNPAID) {
            return false;
        }
        order.setoQuantity(Integer.valueOf(quantity));
        order.setoTotal(total);
        return true;
    }

    @Override
    public synchronized ShopOrder removeUnpaidOrder(String orderId) {
        ShopOrder order = isBlank(orderId) ? null : m_orders.get(orderId);
        if (order == null || order.getoStatus() != ShopOrderStatus.UNPAID) {
            return null;
        }
        return m_orders.remove(orderId);
    }

    // ========== 管理员功能 ==========

    @Override
    public synchronized boolean insertItem(ShopItem item) {
        return addItem(item);
    }

    @Override
    public synchronized OrderListResponse queryAllOrders(OrderQuery query) {
        ShopOrderStatus status = query.getStatus();
        String userUuid = query.getUserUuid();

        List<ShopOrder> matched = new ArrayList<ShopOrder>();
        for (ShopOrder order : m_orders.values()) {
            if (status != null && status != order.getoStatus()) {
                continue;
            }
            if (!isBlank(userUuid) && !same(order.getoUserUuid(), userUuid)) {
                continue;
            }
            matched.add(order);
        }
        java.util.Collections.sort(matched, ORDER_BY_TIME_DESC);

        int pageNumber = query.getPageNumber() > 0 ? query.getPageNumber() : 1;
        int pageSize = query.getPageSize() > 0 ? query.getPageSize() : 10;
        int offset = (pageNumber - 1) * pageSize;
        int end = Math.min(offset + pageSize, matched.size());
        List<ShopOrder> page = offset >= matched.size()
                ? new ArrayList<ShopOrder>()
                : new ArrayList<ShopOrder>(matched.subList(offset, end));

        return new OrderListResponse(page, pageNumber, pageSize, matched.size());
    }

    // ========== 私有辅助 ==========

    /**
     * 查找同一用户、同一商品的待支付订单。
     *
     * @param incoming 本次待写入的订单
     * @return 可合并的订单；不存在时返回 null
     */
    private ShopOrder findMergeableOrder(ShopOrder incoming) {
        if (incoming.getoStatus() != ShopOrderStatus.UNPAID) {
            return null;
        }
        for (ShopOrder existing : m_orders.values()) {
            if (existing.getoStatus() == ShopOrderStatus.UNPAID
                    && same(existing.getoUserUuid(), incoming.getoUserUuid())
                    && same(existing.getoItemId(), incoming.getoItemId())) {
                return existing;
            }
        }
        return null;
    }

    /**
     * 校验合并后的待支付数量没有超过当前库存。
     *
     * @param existing 内存中已有的待支付订单
     * @param incoming 本次待合并的订单
     * @return 累计数量不超过库存时返回 true
     */
    private boolean hasStockForMerge(ShopOrder existing, ShopOrder incoming) {
        ShopItem item = m_items.get(incoming.getoItemId());
        if (item == null || item.getSiStock() == null
                || existing.getoQuantity() == null || incoming.getoQuantity() == null) {
            return false;
        }
        int quantity = existing.getoQuantity().intValue()
                + incoming.getoQuantity().intValue();
        return quantity <= item.getSiStock().intValue();
    }

    /**
     * 将本次购买数量与金额累加到已有订单，并让返回对象反映合并结果。
     *
     * @param existing 内存中已有的待支付订单
     * @param incoming 本次待写入的订单
     */
    private void mergeOrder(ShopOrder existing, ShopOrder incoming) {
        int quantity = existing.getoQuantity().intValue()
                + incoming.getoQuantity().intValue();
        BigDecimal total = existing.getoTotal().add(incoming.getoTotal());
        existing.setoQuantity(Integer.valueOf(quantity));
        existing.setoTotal(total);

        incoming.setoId(existing.getoId());
        incoming.setoShopId(existing.getoShopId());
        incoming.setoQuantity(existing.getoQuantity());
        incoming.setoTotal(existing.getoTotal());
        incoming.setoTime(existing.getoTime());
        incoming.setoStatus(existing.getoStatus());
    }

    /** 判断字符串是否为空或仅含空白字符。 */
    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /** 空安全比较两个字符串是否相等。 */
    private static boolean same(String left, String right) {
        return left == null ? right == null : left.equals(right);
    }

    /** 写入单店铺演示种子数据。 */
    private void seed() {
        String owner = "00000000-0000-0000-0000-000000000003";

        addShop(new Shop("SHOP001", "校园 Shop", "提供学习、生活与校园文创商品",
                owner, Boolean.TRUE));

        seedItem("S001", "校园文化衫", "59.90", 100, "纯棉短袖，多尺码可选");
        seedItem("S002", "A5 线圈笔记本", "12.50", 30, "横线内页，适合课堂记录");
        seedItem("S003", "不锈钢保温杯", "88.00", 20, "500ml 容量，便携防漏");
        seedItem("S004", "校徽钥匙扣", "9.90", 80, "金属校徽造型，轻巧耐用");
        seedItem("S005", "校园帆布袋", "36.00", 45, "加厚棉布，可容纳教材和电脑");
        seedItem("S006", "校园明信片套装", "18.00", 60, "六张校园建筑主题明信片");
        seedItem("S007", "校名证件挂绳", "8.90", 120, "可拆卸卡扣，适配校园卡");
        seedItem("S008", "晴雨折叠伞", "42.00", 28, "轻量伞骨，晴雨两用");
        seedItem("S009", "校园建筑书签", "6.90", 75, "金属镂空书签，附流苏");
        seedItem("S010", "珐琅纪念徽章", "15.90", 50, "校园主题珐琅工艺徽章");
        seedItem("S011", "中性笔三支装", "9.90", 90, "0.5mm 黑色速干笔芯");
        seedItem("S012", "荧光标记笔", "12.80", 55, "四色组合，柔和不透纸");
        seedItem("S013", "便签组合", "6.50", 100, "索引贴与方形便签组合装");
        seedItem("S014", "资料收纳袋", "5.90", 70, "A4 透明按扣文件袋");
        seedItem("S015", "32GB U盘", "49.90", 24, "USB 3.0，高速便携存储");
        seedItem("S016", "Type-C 数据线", "19.90", 65, "1.5 米编织线，支持快充");
        seedItem("S017", "10000mAh 移动电源", "89.00", 18, "双接口输出，带电量显示");
        seedItem("S018", "有线耳机", "39.90", 26, "3.5mm 接口，带线控麦克风");
        seedItem("S019", "USB 护眼台灯", "69.00", 16, "三档色温，亮度可调");
        seedItem("S020", "科学计算器", "32.00", 35, "适合基础课程与日常计算");
        seedItem("S021", "速干运动毛巾", "24.90", 40, "轻薄吸汗，附收纳袋");
        seedItem("S022", "防滑瑜伽垫", "49.00", 14, "加厚防滑，适合宿舍锻炼");
        seedItem("S023", "羽毛球拍套装", "58.00", 12, "双拍组合，附三只训练球");
        seedItem("S024", "运动水壶", "29.90", 32, "700ml 大容量，单手开盖");
        seedItem("S025", "洗衣液", "16.90", 48, "低泡易漂洗，1kg 瓶装");
        seedItem("S026", "抽纸三包装", "8.50", 85, "原生木浆，宿舍日常装");
        seedItem("S027", "宿舍门锁", "12.00", 38, "黄铜锁芯，附两把钥匙");
        seedItem("S028", "便携雨衣", "15.00", 42, "加厚可重复使用，带帽设计");
        seedItem("S029", "能量零食组合", "19.80", 36, "坚果与谷物棒组合装");
        seedItem("S030", "挂耳咖啡", "24.80", 25, "五包装，中度烘焙");
    }

    private void seedItem(String id, String name, String price, int stock,
            String description) {
        addItem(new ShopItem(id, name, new BigDecimal(price), Integer.valueOf(stock),
                description, "SHOP001"));
    }
}
