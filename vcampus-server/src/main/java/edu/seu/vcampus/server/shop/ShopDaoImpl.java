package edu.seu.vcampus.server.shop;

import edu.seu.vcampus.common.shop.entity.Shop;
import edu.seu.vcampus.common.shop.entity.ShopOrder;
import edu.seu.vcampus.common.shop.entity.ShopItem;
import edu.seu.vcampus.common.shop.entity.ShopOrderStatus;
import edu.seu.vcampus.common.shop.dto.OrderQuery;
import edu.seu.vcampus.common.shop.dto.OrderListResponse;
import edu.seu.vcampus.server.db.DatabaseAccessException;
import edu.seu.vcampus.server.db.DbHelper;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 商店数据访问接口实现类.
 */
public class ShopDaoImpl implements ShopDao {

    /** 商店表全部字段，供 SELECT 复用。 */
    private static final String SHOP_COLS = "shopId, shopName, shopDescription, shopOwnerUuid, shopEnabled";

    /** 商品表全部字段，供 SELECT 复用。 */
    private static final String ITEM_COLS = "siUuid, siId, siName, siPrice, siStock, siDesc, siShopId";

    /** 订单表全部字段，供 SELECT 复用。 */
    private static final String ORDER_COLS =
            "oId, oUserUuid, oShopId, oItemId, oQuantity, oTotal, oTime, oStatus";

    /**
     * 执行一条更新语句（INSERT/UPDATE/DELETE）。
     *
     * @param sql 待执行的 SQL
     * @param params 按占位符顺序排列的参数
     * @return 受影响行数大于 0 时返回 true
     */
    private boolean update(String sql, Object... params) {
        try (Connection conn = DbHelper.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ========== 商店管理实现 ==========

    @Override
    public boolean addShop(Shop shop) {
        String sql = "INSERT INTO tblShop (" + SHOP_COLS + ") VALUES (?, ?, ?, ?, ?)";
        return update(sql, shop.getShopId(), shop.getShopName(),
                shop.getShopDescription(), shop.getShopOwnerUuid(), shop.getShopEnabled());
    }

    @Override
    public boolean updateShop(Shop shop) {
        String sql = "UPDATE tblShop SET shopName = ?, shopDescription = ?, "
                + "shopOwnerUuid = ?, shopEnabled = ? WHERE shopId = ?";
        return update(sql, shop.getShopName(), shop.getShopDescription(),
                shop.getShopOwnerUuid(), shop.getShopEnabled(), shop.getShopId());
    }

    @Override
    public boolean deleteShop(String shopId) {
        return update("DELETE FROM tblShop WHERE shopId = ?", shopId);
    }

    @Override
    public Shop findShopById(String shopId) {
        String sql = "SELECT " + SHOP_COLS + " FROM tblShop WHERE shopId = ?";
        try (Connection conn = DbHelper.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, shopId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return extractShop(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<Shop> findShopsByOwner(String ownerUuid) {
        List<Shop> shops = new ArrayList<>();
        String sql = "SELECT " + SHOP_COLS + " FROM tblShop WHERE shopOwnerUuid = ?";
        try (Connection conn = DbHelper.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, ownerUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    shops.add(extractShop(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return shops;
    }

    @Override
    public List<Shop> findAllShops() {
        List<Shop> shops = new ArrayList<>();
        String sql = "SELECT " + SHOP_COLS + " FROM tblShop ORDER BY shopId";
        try (Connection conn = DbHelper.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                shops.add(extractShop(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return shops;
    }

    /**
     * 从结果集当前行提取商店对象.
     *
     * @param rs 结果集
     * @return 商店对象
     * @throws SQLException 读取字段失败时抛出
     */
    private Shop extractShop(ResultSet rs) throws SQLException {
        Shop shop = new Shop();
        shop.setShopId(rs.getString("shopId"));
        shop.setShopName(rs.getString("shopName"));
        shop.setShopDescription(rs.getString("shopDescription"));
        shop.setShopOwnerUuid(rs.getString("shopOwnerUuid"));
        shop.setShopEnabled(rs.getBoolean("shopEnabled"));
        return shop;
    }

    // ========== 商品管理实现 ==========


    @Override
    public List<ShopItem> findAllItems() {
        System.out.println("[ShopDaoImpl] 开始执行findAllItems查询");
        List<ShopItem> items = new ArrayList<>();
        String sql = "SELECT " + ITEM_COLS + " FROM tblShopItem ORDER BY siId";
        System.out.println("[ShopDaoImpl] SQL: " + sql);
        try (Connection conn = DbHelper.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {
            System.out.println("[ShopDaoImpl] 数据库连接成功，开始读取结果集");
            int count = 0;
            while (rs.next()) {
                items.add(extractItem(rs));
                count++;
            }
            System.out.println("[ShopDaoImpl] 成功读取 " + count + " 条商品记录");
        } catch (SQLException e) {
            System.err.println("[ShopDaoImpl] SQL异常: " + e.getMessage());
            System.err.println("[ShopDaoImpl] SQL状态: " + e.getSQLState());
            System.err.println("[ShopDaoImpl] 错误代码: " + e.getErrorCode());
            e.printStackTrace();
        }
        return items;
    }

    @Override
    public List<ShopItem> findItemsByShop(String shopId) {
        List<ShopItem> items = new ArrayList<>();
        String sql = "SELECT " + ITEM_COLS + " FROM tblShopItem WHERE siShopId = ? ORDER BY siId";
        try (Connection conn = DbHelper.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, shopId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    items.add(extractItem(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }

    @Override
    public ShopItem findItemById(String itemId) {
        String sql = "SELECT " + ITEM_COLS + " FROM tblShopItem WHERE siId = ?";
        try (Connection conn = DbHelper.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, itemId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return extractItem(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean addItem(ShopItem item) {
        String sql = "INSERT INTO tblShopItem (" + ITEM_COLS + ") VALUES (?, ?, ?, ?, ?, ?, ?)";
        if (item.getSiUuid() == null || item.getSiUuid().trim().isEmpty()) {
            item.setSiUuid(UUID.randomUUID().toString());
        }
        return update(sql, item.getSiUuid(), item.getSiId(), item.getSiName(),
                item.getSiPrice(), item.getSiStock(), item.getSiDesc(), item.getSiShopId());
    }

    @Override
    public boolean updateItem(ShopItem item) {
        String sql = "UPDATE tblShopItem SET siId = ?, siName = ?, siPrice = ?, "
            + "siStock = ?, siDesc = ?, siShopId = ? WHERE siUuid = ?";
        return update(sql, item.getSiId(), item.getSiName(), item.getSiPrice(),
            item.getSiStock(), item.getSiDesc(), item.getSiShopId(), item.getSiUuid());
    }

    @Override
    public boolean deleteItem(String itemId) {
        return update("DELETE FROM tblShopItem WHERE siId = ?", itemId);
    }

    @Override
    public boolean reduceStock(String itemId, int quantity) {
        String sql = "UPDATE tblShopItem SET siStock = siStock - ? "
                + "WHERE siId = ? AND siStock >= ?";
        return update(sql, quantity, itemId, quantity);
    }

    @Override
    public boolean addOrder(ShopOrder order) {
        String sql = "INSERT INTO tblOrder (" + ORDER_COLS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        Timestamp time = order.getoTime() == null ? null
                : new Timestamp(order.getoTime().getTime());
        return update(sql, order.getoId(), order.getoUserUuid(), order.getoShopId(),
                order.getoItemId(), order.getoQuantity(), order.getoTotal(), time, order.getoStatus());
    }

    @Override
    public List<ShopOrder> findOrdersByUser(String userUuid) {
        List<ShopOrder> orders = new ArrayList<>();
        String sql = "SELECT " + ORDER_COLS + " FROM tblOrder WHERE oUserUuid = ? "
                + "ORDER BY oTime DESC";
        try (Connection conn = DbHelper.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    orders.add(extractOrder(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseAccessException("查询用户订单失败: " + userUuid, e);
        }
        return orders;
    }

    @Override
    public List<ShopOrder> findOrdersByShop(String shopId) {
        List<ShopOrder> orders = new ArrayList<>();
        String sql = "SELECT " + ORDER_COLS + " FROM tblOrder WHERE oShopId = ? "
                + "ORDER BY oTime DESC";
        try (Connection conn = DbHelper.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, shopId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    orders.add(extractOrder(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseAccessException("查询商店订单失败: " + shopId, e);
        }
        return orders;
    }

    @Override
    public List<ShopOrder> findOrdersByUserPaged(String userUuid, int pageNumber, int pageSize) {
        List<ShopOrder> orders = new ArrayList<>();
        int offset = (pageNumber - 1) * pageSize;
        String sql = "SELECT " + ORDER_COLS + " FROM tblOrder WHERE oUserUuid = ? "
                + "ORDER BY oTime DESC LIMIT ? OFFSET ?";
        try (Connection conn = DbHelper.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userUuid);
            stmt.setInt(2, pageSize);
            stmt.setInt(3, offset);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    orders.add(extractOrder(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseAccessException("分页查询用户订单失败: " + userUuid, e);
        }
        return orders;
    }

    @Override
    public long countOrdersByUser(String userUuid) {
        String sql = "SELECT COUNT(*) FROM tblOrder WHERE oUserUuid = ?";
        try (Connection conn = DbHelper.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseAccessException("统计用户订单数失败: " + userUuid, e);
        }
        return 0;
    }

    /**
     * 从结果集当前行提取商品对象.
     *
     * @param rs 结果集
     * @return 商品对象
     * @throws SQLException 读取字段失败时抛出
     */
    private ShopItem extractItem(ResultSet rs) throws SQLException {
        ShopItem item = new ShopItem();
        item.setSiUuid(rs.getString("siUuid"));
        item.setSiId(rs.getString("siId"));
        item.setSiName(rs.getString("siName"));
        item.setSiPrice(rs.getBigDecimal("siPrice"));
        item.setSiStock(rs.getInt("siStock"));
        item.setSiDesc(rs.getString("siDesc"));
        item.setSiShopId(rs.getString("siShopId"));
        return item;
    }

    /**
     * 从结果集当前行提取订单对象.
     *
     * @param rs 结果集
     * @return 订单对象
     * @throws SQLException 读取字段失败时抛出
     */
    private ShopOrder extractOrder(ResultSet rs) throws SQLException {
        ShopOrder order = new ShopOrder();
        order.setoId(rs.getString("oId"));
        order.setoUserUuid(rs.getString("oUserUuid"));
        order.setoShopId(rs.getString("oShopId"));
        order.setoItemId(rs.getString("oItemId"));
        order.setoQuantity(rs.getInt("oQuantity"));
        order.setoTotal(rs.getBigDecimal("oTotal"));
        order.setoTime(rs.getTimestamp("oTime"));
        String statusStr = rs.getString("oStatus");
        order.setoStatus(statusStr != null ? ShopOrderStatus.fromDisplayName(statusStr) : null);
        return order;
    }

    @Override
    public ShopOrder findOrderById(String orderId) {
        String sql = "SELECT " + ORDER_COLS + " FROM tblOrder WHERE oId = ?";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, orderId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return extractOrder(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean updateOrderStatus(String orderId, ShopOrderStatus status) {
        String sql = "UPDATE tblOrder SET oStatus = ? WHERE oId = ?";
        return update(sql, status.getDisplayName(), orderId);
    }

    // ========== 管理员功能 ==========

    @Override
    public boolean insertItem(ShopItem item) {
        // 复用现有的 addItem 方法
        return addItem(item);
    }

    @Override
    public OrderListResponse queryAllOrders(OrderQuery query) {
        StringBuilder sql = new StringBuilder(
            "SELECT " + ORDER_COLS + " FROM tblOrder WHERE 1=1"
        );

        // 状态筛选
        if (query.getStatus() != null) {
            sql.append(" AND oStatus = '").append(query.getStatus().getDisplayName()).append("'");
        }

        // 用户筛选
        if (query.getUserUuid() != null && !query.getUserUuid().trim().isEmpty()) {
            sql.append(" AND oUserUuid = '").append(query.getUserUuid()).append("'");
        }

        // 按下单时间倒序
        sql.append(" ORDER BY oTime DESC");

        // 分页
        int pageNumber = query.getPageNumber() > 0 ? query.getPageNumber() : 1;
        int pageSize = query.getPageSize() > 0 ? query.getPageSize() : 10;
        int offset = (pageNumber - 1) * pageSize;
        sql.append(" LIMIT ").append(pageSize).append(" OFFSET ").append(offset);

        List<ShopOrder> orders = new ArrayList<>();
        long totalCount = 0;

        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString());
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                orders.add(extractOrder(rs));
            }

            // 查询总数
            String countSql = "SELECT COUNT(*) FROM tblOrder WHERE 1=1";
            if (query.getStatus() != null) {
                countSql += " AND oStatus = '" + query.getStatus().getDisplayName() + "'";
            }
            if (query.getUserUuid() != null && !query.getUserUuid().trim().isEmpty()) {
                countSql += " AND oUserUuid = '" + query.getUserUuid() + "'";
            }

            try (PreparedStatement countStmt = conn.prepareStatement(countSql);
                 ResultSet countRs = countStmt.executeQuery()) {
                if (countRs.next()) {
                    totalCount = countRs.getLong(1);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return new OrderListResponse(orders, pageNumber, pageSize, totalCount);
    }
}
