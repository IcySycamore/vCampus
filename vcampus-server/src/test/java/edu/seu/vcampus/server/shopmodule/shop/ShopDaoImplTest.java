package edu.seu.vcampus.server.shopmodule.shop;

import edu.seu.vcampus.common.shop.Order;
import edu.seu.vcampus.common.shop.ShopItem;
import edu.seu.vcampus.server.db.DbHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ShopDaoImpl 集成测试：真连 MySQL 验证 SQL 正确性（见 ADR-0005）。
 *
 * <p>全类以环境变量 {@code DB_NAME} 门控，本地无数据库时自动跳过。用例依赖
 * {@code sql/vCampus.sql} 写入的演示商品 S001/S002/S003 与演示账号 001，
 * 自建数据在每个用例后清理，避免影响其他测试。
 */
@EnabledIfEnvironmentVariable(named = "DB_NAME", matches = ".+")
class ShopDaoImplTest {

    /** 被测对象。 */
    private final ShopDao dao = new ShopDaoImpl();

    /** 临时商品ID，避免与演示数据冲突。 */
    private static final String TEMP_ITEM = "T901";

    /** 临时订单ID。 */
    private static final String TEMP_ORDER = "TO901";

    /**
     * 每个用例后清理自建的订单与商品（先删订单，避免外键约束）。
     *
     * @throws SQLException 清理失败时抛出
     */
    @AfterEach
    void cleanUp() throws SQLException {
        exec("DELETE FROM tblOrder WHERE oId = ?", TEMP_ORDER);
        exec("DELETE FROM tblShopItem WHERE siId = ?", TEMP_ITEM);
    }

    /**
     * 执行一条清理语句。
     *
     * @param sql 待执行 SQL
     * @param param 单个参数
     * @throws SQLException 执行失败时抛出
     */
    private void exec(String sql, String param) throws SQLException {
        try (Connection conn = DbHelper.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, param);
            stmt.executeUpdate();
        }
    }

    /**
     * 构造临时商品。
     *
     * @return 商品对象
     */
    private ShopItem tempItem() {
        return new ShopItem(TEMP_ITEM, "临时商品", new BigDecimal("10.00"), 5, "集成测试用");
    }

    /**
     * 应能查到建库脚本写入的演示商品，且金额精度正确。
     */
    @Test
    void findItemByIdHitsSeedData() {
        ShopItem item = dao.findItemById("S001");

        assertNotNull(item, "应能查到演示商品 S001");
        assertEquals("校园文化衫", item.getSiName());
        assertEquals(new BigDecimal("59.90"), item.getSiPrice(), "金额精度应保持两位小数");
    }

    /**
     * 查询不存在的商品应返回 null。
     */
    @Test
    void findItemByIdReturnsNullWhenAbsent() {
        assertNull(dao.findItemById("S404"));
    }

    /**
     * 商品列表应包含演示数据。
     */
    @Test
    void findAllItemsContainsSeedData() {
        List<ShopItem> items = dao.findAllItems();

        assertNotNull(items, "应返回列表而非 null");
        assertTrue(items.size() >= 3, "应至少包含三件演示商品，实际 " + items.size());
    }

    /**
     * 新增商品后应能查回，更新后字段应变化，删除后应查不到。
     */
    @Test
    void addUpdateDeleteItem() {
        assertTrue(dao.addItem(tempItem()), "新增商品应成功");

        ShopItem saved = dao.findItemById(TEMP_ITEM);
        assertNotNull(saved, "新增后应能查回");
        assertEquals("临时商品", saved.getSiName());

        saved.setSiName("改名后");
        saved.setSiPrice(new BigDecimal("20.00"));
        assertTrue(dao.updateItem(saved), "更新商品应成功");
        assertEquals("改名后", dao.findItemById(TEMP_ITEM).getSiName());

        assertTrue(dao.deleteItem(TEMP_ITEM), "删除商品应成功");
        assertNull(dao.findItemById(TEMP_ITEM), "删除后应查不到");
    }

    /**
     * 库存充足时扣减成功并写回新库存；请求量超过库存时拒绝扣减，库存保持不变。
     */
    @Test
    void reduceStockGuardsAgainstOversell() {
        dao.addItem(tempItem());

        assertTrue(dao.reduceStock(TEMP_ITEM, 2), "库存 5 扣 2 应成功");
        assertEquals(Integer.valueOf(3), dao.findItemById(TEMP_ITEM).getSiStock());

        assertFalse(dao.reduceStock(TEMP_ITEM, 99), "超过库存应拒绝扣减");
        assertEquals(Integer.valueOf(3), dao.findItemById(TEMP_ITEM).getSiStock(),
                "拒绝扣减后库存不应变化");
    }

    /**
     * 新增订单后应能按用户查回，金额与时间字段往返一致。
     */
    @Test
    void addOrderThenFindByUser() {
        dao.addItem(tempItem());
        String userUuid = "550e8400-e29b-41d4-a716-446655440000";
        Order order = new Order(TEMP_ORDER, userUuid, TEMP_ITEM, 2,
                new BigDecimal("20.00"), new Date(), "待支付");

        assertTrue(dao.addOrder(order), "新增订单应成功");

        List<Order> orders = dao.findOrdersByUser(userUuid);
        assertNotNull(orders, "应返回列表而非 null");
        Order found = null;
        for (Order o : orders) {
            if (TEMP_ORDER.equals(o.getoId())) {
                found = o;
            }
        }
        assertNotNull(found, "应能按用户查回刚写入的订单");
        assertEquals(new BigDecimal("20.00"), found.getoTotal());
        assertEquals(Integer.valueOf(2), found.getoQuantity());
        assertNotNull(found.getoTime(), "下单时间应落库");
    }

    /**
     * 查询无订单用户应返回空列表而非 null。
     */
    @Test
    void findOrdersByUserReturnsEmptyListWhenNone() {
        List<Order> orders = dao.findOrdersByUser("660e8400-e29b-41d4-a716-446655440000");

        assertNotNull(orders, "应返回空列表而非 null");
        assertTrue(orders.isEmpty());
    }
}
