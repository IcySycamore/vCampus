package edu.seu.vcampus.server.shop;

import edu.seu.vcampus.common.bank.dto.BankTransactionListResponse;
import edu.seu.vcampus.common.bank.entity.BankTransactionType;
import edu.seu.vcampus.common.bank.security.BankPassword;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.shop.entity.ShopOrder;
import edu.seu.vcampus.common.shop.entity.ShopOrderStatus;
import edu.seu.vcampus.server.bank.BankService;
import java.math.BigDecimal;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 使用真实内存银行服务验证 Shop 支付的余额、流水、库存和订单状态。 */
class ShopPaymentIntegrationTest {

    private static final String USER_UUID = "uuid-shop-payment";

    private BankService bank;
    private ShopDaoMemory dao;
    private ShopService shop;
    private char[] password;

    /** 创建已开户、已充值的银行账户及共享该实例的 Shop 服务。 */
    @BeforeEach
    void setUp() {
        bank = new BankService();
        password = "bank12345".toCharArray();
        byte[] salt = BankPassword.newSalt();
        bank.openAccount(USER_UUID, salt, BankPassword.derive(password, salt));
        bank.recharge(USER_UUID, new BigDecimal("500.00"));
        dao = new ShopDaoMemory();
        shop = new ShopService(dao, new BankAdapter(bank));
    }

    /** 成功支付应只扣一次余额和库存，并写入一条消费流水。 */
    @Test
    void successfulPaymentChangesBankStockAndOrderTogether() {
        ShopOrder order = shop.purchase(USER_UUID, "S001", 2);
        assertEquals(Integer.valueOf(100), dao.findItemById("S001").getSiStock());

        assertTrue(shop.payOrder(order.getoId(), USER_UUID, password));

        assertEquals(new BigDecimal("380.20"),
                bank.queryAccount(USER_UUID).getBalance());
        assertEquals(Integer.valueOf(98), dao.findItemById("S001").getSiStock());
        assertEquals(ShopOrderStatus.PAID,
                dao.findOrderById(order.getoId()).getoStatus());
        BankTransactionListResponse transactions = bank.listTransactions(USER_UUID, null);
        assertEquals(2L, transactions.getTotalCount());
        assertEquals(BankTransactionType.CONSUMPTION,
                transactions.getTransactions().get(1).getType());
        assertEquals(order.getoId(),
                transactions.getTransactions().get(1).getRelatedOrderId());

        assertTrue(!shop.payOrder(order.getoId(), USER_UUID, password));
        assertEquals(new BigDecimal("380.20"),
                bank.queryAccount(USER_UUID).getBalance());
        assertEquals(Integer.valueOf(98), dao.findItemById("S001").getSiStock());
    }

    /** 密码错误时余额、库存和订单状态都必须保持不变。 */
    @Test
    void wrongPasswordRestoresStockAndKeepsOrderUnpaid() {
        final ShopOrder order = shop.purchase(USER_UUID, "S001", 1);
        final char[] wrongPassword = "wrong123".toCharArray();

        ShopPaymentException error = assertThrows(ShopPaymentException.class,
                new Executable() {
                    @Override
                    public void execute() {
                        shop.payOrder(order.getoId(), USER_UUID, wrongPassword);
                    }
                });

        assertEquals(StatusCode.BANK_PASSWORD_INVALID, error.getStatusCode());
        assertEquals(new BigDecimal("500.00"),
                bank.queryAccount(USER_UUID).getBalance());
        assertEquals(Integer.valueOf(100), dao.findItemById("S001").getSiStock());
        assertEquals(ShopOrderStatus.UNPAID,
                dao.findOrderById(order.getoId()).getoStatus());
        Arrays.fill(wrongPassword, '\0');
    }

    /** 余额不足时支付失败，支付阶段暂扣的库存必须完整回补。 */
    @Test
    void insufficientBalanceRestoresStockAndKeepsOrderUnpaid() {
        final ShopOrder order = shop.purchase(USER_UUID, "S001", 9);

        ShopPaymentException error = assertThrows(ShopPaymentException.class,
                new Executable() {
                    @Override
                    public void execute() {
                        shop.payOrder(order.getoId(), USER_UUID, password);
                    }
                });

        assertEquals(StatusCode.BAD_REQUEST, error.getStatusCode());
        assertEquals("银行账户余额不足，请先充值", error.getMessage());
        assertEquals(new BigDecimal("500.00"),
                bank.queryAccount(USER_UUID).getBalance());
        assertEquals(Integer.valueOf(100), dao.findItemById("S001").getSiStock());
        assertEquals(ShopOrderStatus.UNPAID,
                dao.findOrderById(order.getoId()).getoStatus());

        assertTrue(shop.cancelOrder(order.getoId(), USER_UUID));
        assertEquals(ShopOrderStatus.CANCELLED,
                dao.findOrderById(order.getoId()).getoStatus());
        assertEquals(new BigDecimal("500.00"),
                bank.queryAccount(USER_UUID).getBalance());
        assertEquals(Integer.valueOf(100), dao.findItemById("S001").getSiStock());
    }

    /** 多个勾选订单应合计扣款并整批更新订单和库存。 */
    @Test
    void checkoutPaysSelectedOrdersAsOneBatch() {
        ShopOrder shirt = shop.purchase(USER_UUID, "S001", 2);
        ShopOrder notebook = shop.purchase(USER_UUID, "S002", 1);

        assertTrue(shop.payOrders(Arrays.asList(shirt.getoId(), notebook.getoId()),
                USER_UUID, password));

        assertEquals(new BigDecimal("367.70"),
                bank.queryAccount(USER_UUID).getBalance());
        assertEquals(Integer.valueOf(98), dao.findItemById("S001").getSiStock());
        assertEquals(Integer.valueOf(29), dao.findItemById("S002").getSiStock());
        assertEquals(ShopOrderStatus.PAID,
                dao.findOrderById(shirt.getoId()).getoStatus());
        assertEquals(ShopOrderStatus.PAID,
                dao.findOrderById(notebook.getoId()).getoStatus());
        BankTransactionListResponse transactions = bank.listTransactions(USER_UUID, null);
        assertEquals(2L, transactions.getTotalCount());
        assertEquals(new BigDecimal("132.30"),
                transactions.getTransactions().get(1).getAmount());
    }

    /** 合计余额不足时整批失败，不能出现部分订单已支付。 */
    @Test
    void checkoutIsAllOrNothingWhenTotalBalanceIsInsufficient() {
        final ShopOrder shirt = shop.purchase(USER_UUID, "S001", 8);
        final ShopOrder notebooks = shop.purchase(USER_UUID, "S002", 2);

        ShopPaymentException error = assertThrows(ShopPaymentException.class,
                new Executable() {
                    @Override
                    public void execute() {
                        shop.payOrders(Arrays.asList(shirt.getoId(), notebooks.getoId()),
                                USER_UUID, password);
                    }
                });

        assertEquals(StatusCode.BAD_REQUEST, error.getStatusCode());
        assertEquals(new BigDecimal("500.00"),
                bank.queryAccount(USER_UUID).getBalance());
        assertEquals(Integer.valueOf(100), dao.findItemById("S001").getSiStock());
        assertEquals(Integer.valueOf(30), dao.findItemById("S002").getSiStock());
        assertEquals(ShopOrderStatus.UNPAID,
                dao.findOrderById(shirt.getoId()).getoStatus());
        assertEquals(ShopOrderStatus.UNPAID,
                dao.findOrderById(notebooks.getoId()).getoStatus());
    }
}
