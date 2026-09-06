package edu.seu.vcampus.server.module.shop;

import edu.seu.vcampus.common.entity.Order;
import edu.seu.vcampus.common.entity.ShopItem;
import edu.seu.vcampus.server.db.DbHelper;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * 商店数据访问接口实现类.
 */
public class ShopDaoImpl implements ShopDao {

    /** 商品表全部字段，供 SELECT 复用。 */
    private static final String ITEM_COLS = "siId, siName, siPrice, siStock, siDesc";

    /** 订单表全部字段，供 SELECT 复用。 */
    private static final String ORDER_COLS =
            "oId, oUserId, oItemId, oQuantity, oTotal, oTime, oStatus";

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

    @Override
    public List<ShopItem> findAllItems() {
        List<ShopItem> items = new ArrayList<>();
        String sql = "SELECT " + ITEM_COLS + " FROM tblShopItem ORDER BY siId";
        try (Connection conn = DbHelper.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                items.add(extractItem(rs));
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
        String sql = "INSERT INTO tblShopItem (" + ITEM_COLS + ") VALUES (?, ?, ?, ?, ?)";
        return update(sql, item.getSiId(), item.getSiName(), item.getSiPrice(),
                item.getSiStock(), item.getSiDesc());
    }

    @Override
    public boolean updateItem(ShopItem item) {
        String sql = "UPDATE tblShopItem SET siName = ?, siPrice = ?, siStock = ?, "
                + "siDesc = ? WHERE siId = ?";
        return update(sql, item.getSiName(), item.getSiPrice(), item.getSiStock(),
                item.getSiDesc(), item.getSiId());
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
    public boolean addOrder(Order order) {
        String sql = "INSERT INTO tblOrder (" + ORDER_COLS + ") VALUES (?, ?, ?, ?, ?, ?, ?)";
        Timestamp time = order.getoTime() == null ? null
                : new Timestamp(order.getoTime().getTime());
        return update(sql, order.getoId(), order.getoUserId(), order.getoItemId(),
                order.getoQuantity(), order.getoTotal(), time, order.getoStatus());
    }

    @Override
    public List<Order> findOrdersByUser(String userId) {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT " + ORDER_COLS + " FROM tblOrder WHERE oUserId = ? "
                + "ORDER BY oTime DESC";
        try (Connection conn = DbHelper.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    orders.add(extractOrder(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return orders;
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
        item.setSiId(rs.getString("siId"));
        item.setSiName(rs.getString("siName"));
        item.setSiPrice(rs.getBigDecimal("siPrice"));
        item.setSiStock(rs.getInt("siStock"));
        item.setSiDesc(rs.getString("siDesc"));
        return item;
    }

    /**
     * 从结果集当前行提取订单对象.
     *
     * @param rs 结果集
     * @return 订单对象
     * @throws SQLException 读取字段失败时抛出
     */
    private Order extractOrder(ResultSet rs) throws SQLException {
        Order order = new Order();
        order.setoId(rs.getString("oId"));
        order.setoUserId(rs.getString("oUserId"));
        order.setoItemId(rs.getString("oItemId"));
        order.setoQuantity(rs.getInt("oQuantity"));
        order.setoTotal(rs.getBigDecimal("oTotal"));
        order.setoTime(rs.getTimestamp("oTime"));
        order.setoStatus(rs.getString("oStatus"));
        return order;
    }
}
