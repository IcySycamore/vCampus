package edu.seu.vcampus.server.shop;

import edu.seu.vcampus.common.bank.entity.BankTransaction;
import edu.seu.vcampus.common.bank.entity.BankTransactionType;
import edu.seu.vcampus.common.shop.dto.OrderListResponse;
import edu.seu.vcampus.common.shop.dto.OrderQuery;
import edu.seu.vcampus.common.shop.entity.ShopOrder;
import edu.seu.vcampus.common.shop.entity.ShopOrderStatus;
import edu.seu.vcampus.server.db.DatabaseAvailability;
import edu.seu.vcampus.server.db.DbHelper;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Shop JDBC 订单合并、数量编辑、筛选和支付库存测试。 */
class ShopPaymentIntegrationTest {

    private static final String USER_UUID = "00000000-0000-0000-0000-00000000f071";
    private static final String SHOP_ID = "SHOP-PR71";
    private static final String ITEM_ID = "PR71-I1";

    private ShopDaoImpl dao;
    private BankAdapter bank;
    private ShopService shop;
    private boolean databaseReady;

    @BeforeEach
    void setUp() throws Exception {
        databaseReady = DatabaseAvailability.tablesExist(
                "tblUserCredential", "tblShop", "tblShopItem", "tblOrder");
        Assumptions.assumeTrue(databaseReady, "MySQL 不可用，跳过 Shop JDBC 测试");
        cleanFixture();
        insertFixture();
        dao = new ShopDaoImpl();
        bank = mock(BankAdapter.class);
        shop = new ShopService(dao, bank);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (databaseReady) {
            cleanFixture();
        }
    }

    @Test
    void repeatedItemMergesInDatabaseWithoutReducingStock() {
        ShopOrder first = shop.purchase(USER_UUID, ITEM_ID, 2);
        ShopOrder merged = shop.purchase(USER_UUID, ITEM_ID, 3);

        assertNotNull(first);
        assertEquals(ShopOrderStatus.UNPAID,
                dao.findOrderById(first.getoId()).getoStatus());
        assertEquals(first.getoId(), merged.getoId());
        assertEquals(Integer.valueOf(5), merged.getoQuantity());
        assertEquals(new BigDecimal("49.50"), merged.getoTotal());
        assertEquals(Integer.valueOf(12), dao.findItemById(ITEM_ID).getSiStock());
        assertEquals(1, dao.findOrdersByUser(USER_UUID).size());
    }

    @Test
    void zeroQuantityDeletesOnlyUnpaidOrder() {
        ShopOrder created = shop.purchase(USER_UUID, ITEM_ID, 2);

        ShopOrder removed = shop.updateOrderQuantity(created.getoId(), USER_UUID, 0);

        assertNotNull(removed);
        assertEquals(Integer.valueOf(0), removed.getoQuantity());
        assertNull(dao.findOrderById(created.getoId()));
        assertEquals(Integer.valueOf(12), dao.findItemById(ITEM_ID).getSiStock());
    }

    @Test
    void statusFilterUsesDatabasePagingAndCount() {
        ShopOrder paid = shop.purchase(USER_UUID, ITEM_ID, 1);
        assertTrue(dao.updateOrderStatus(paid.getoId(), ShopOrderStatus.PAID));
        ShopOrder unpaid = shop.purchase(USER_UUID, ITEM_ID, 2);

        OrderListResponse unpaidPage = shop.listOrdersOfUserPaged(USER_UUID,
                new OrderQuery(1, 10, ShopOrderStatus.UNPAID, null));
        OrderListResponse paidPage = shop.listOrdersOfUserPaged(USER_UUID,
                new OrderQuery(1, 10, ShopOrderStatus.PAID, null));

        assertEquals(1L, unpaidPage.getTotalCount());
        assertEquals(unpaid.getoId(), unpaidPage.getOrders().get(0).getoId());
        assertEquals(1L, paidPage.getTotalCount());
        assertEquals(paid.getoId(), paidPage.getOrders().get(0).getoId());
        assertNotEquals(paid.getoId(), unpaid.getoId());
    }

    @Test
    void successfulPaymentThenReducesStockAndMarksOrderPaid() {
        ShopOrder order = shop.purchase(USER_UUID, ITEM_ID, 2);
        char[] password = "bank12345".toCharArray();
        BigDecimal before = new BigDecimal("100.00");
        BankTransaction transaction = new BankTransaction("T-PR71", "A-PR71",
                BankTransactionType.CONSUMPTION, order.getoTotal(), before,
                before.subtract(order.getoTotal()), order.getoId(), "购买商品", new Date());
        when(bank.deduct(eq(USER_UUID), any(char[].class), eq(order.getoTotal()),
                eq(order.getoId()), anyString())).thenReturn(transaction);

        assertTrue(shop.payOrder(order.getoId(), USER_UUID, password));

        assertEquals(Integer.valueOf(10), dao.findItemById(ITEM_ID).getSiStock());
        assertEquals(ShopOrderStatus.PAID,
                dao.findOrderById(order.getoId()).getoStatus());
    }

    private static void insertFixture() throws SQLException {
        Connection connection = DbHelper.getConnection();
        try {
            execute(connection, "INSERT INTO tblUserCredential "
                    + "(ucUsername, ucUuid, ucSalt, ucHash, ucRole, ucEnabled, ucName) "
                    + "VALUES ('pr71-shop-user', '" + USER_UUID + "', "
                    + "'0123456789abcdef0123456789abcdef', "
                    + "'0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef', "
                    + "'学生', 1, 'PR71 Shop Test')");
            execute(connection, "INSERT INTO tblShop "
                    + "(shopId, shopName, shopDescription, shopOwnerUuid, shopEnabled) VALUES "
                    + "('" + SHOP_ID + "', '校园商店测试', 'PR71', '" + USER_UUID + "', 1)");
            execute(connection, "INSERT INTO tblShopItem "
                    + "(siUuid, siId, siName, siPrice, siStock, siDesc, siShopId) VALUES "
                    + "('00000000-0000-0000-0000-00000000f072', '" + ITEM_ID
                    + "', '测试商品', 9.90, 12, 'JDBC fixture', '" + SHOP_ID + "')");
        } finally {
            connection.close();
        }
    }

    private static void cleanFixture() throws SQLException {
        Connection connection = DbHelper.getConnection();
        try {
            delete(connection, "DELETE FROM tblOrder WHERE oUserUuid = ?", USER_UUID);
            delete(connection, "DELETE FROM tblShopItem WHERE siId = ?", ITEM_ID);
            delete(connection, "DELETE FROM tblShop WHERE shopId = ?", SHOP_ID);
            delete(connection, "DELETE FROM tblUserCredential WHERE ucUuid = ?", USER_UUID);
        } finally {
            connection.close();
        }
    }

    private static void execute(Connection connection, String sql) throws SQLException {
        Statement statement = connection.createStatement();
        try {
            statement.executeUpdate(sql);
        } finally {
            statement.close();
        }
    }

    private static void delete(Connection connection, String sql, String value)
            throws SQLException {
        PreparedStatement statement = connection.prepareStatement(sql);
        try {
            statement.setString(1, value);
            statement.executeUpdate();
        } finally {
            statement.close();
        }
    }
}
