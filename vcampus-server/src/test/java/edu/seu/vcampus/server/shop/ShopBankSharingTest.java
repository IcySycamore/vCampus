package edu.seu.vcampus.server.shop;

import edu.seu.vcampus.common.shop.entity.ShopOrder;
import edu.seu.vcampus.common.shop.entity.ShopOrderStatus;
import edu.seu.vcampus.server.bank.BankService;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 锁住「商店必须与银行模块共用同一个 {@link BankService} 实例」这条装配约定。
 *
 * <p>
 * 背景：{@link ShopService} 的单参便利构造器内部 {@code new BankAdapter()}，而 {@link BankAdapter} 的无参构造器又
 * {@code new BankService()}，于是商店在自己的账户池里找用户 —— 用户在界面上开的 户长在另一个实例上，支付必然失败，而且失败得很安静：{@code payOrder}
 * 只返回 false，界面只能 说「支付不成功」。生产装配现在由 {@code ShopModule} 显式传入银行模块的单例，那个无参 便利构造器已删。
 *
 * <p>
 * 这里用两个用例把行为钉死：共用实例时扣款成功；各拿一个实例时失败（正是修复前的症状）。
 */
class ShopBankSharingTest {

    /** 测试用户 uuid。 */
    private static final String USER_UUID = "550e8400-e29b-41d4-a716-446655440000";

    /** 测试订单号。 */
    private static final String ORDER_ID = "O-SHARE-1";

    @Test
    void payingSucceedsWhenShopSharesTheBankInstance() {
        BankService bank = new BankService();
        bank.openAccount(USER_UUID);
        bank.recharge(USER_UUID, BigDecimal.valueOf(100).setScale(2));

        ShopService service = new ShopService(pendingOrderDao(), new BankAdapter(bank));
        assertTrue(service.payOrder(ORDER_ID, USER_UUID),
                "共用银行实例时应能扣到用户在该实例开的户");
    }

    @Test
    void payingFailsWhenShopBringsItsOwnBankInstance() {
        BankService shared = new BankService();
        shared.openAccount(USER_UUID);
        shared.recharge(USER_UUID, BigDecimal.valueOf(100).setScale(2));

        // 各拿一个 BankService 就是修复前的症状：商店在自己那个账户池里找用户
        ShopService isolated = new ShopService(pendingOrderDao(),
                new BankAdapter(new BankService()));
        assertFalse(isolated.payOrder(ORDER_ID, USER_UUID),
                "独立银行实例里没有这个账户，支付必然失败");
    }

    /**
     * 造一个「订单待支付、金额 10 元、状态可写回」的 DAO 替身。
     *
     * @return 替身
     */
    private static ShopDao pendingOrderDao() {
        ShopDao dao = mock(ShopDao.class);
        ShopOrder order = mock(ShopOrder.class);
        when(order.getoUserUuid()).thenReturn(USER_UUID);
        when(order.getoStatus()).thenReturn(ShopOrderStatus.UNPAID);
        when(order.getoTotal()).thenReturn(BigDecimal.TEN.setScale(2));
        when(dao.findOrderById(ORDER_ID)).thenReturn(order);
        when(dao.updateOrderStatus(anyString(), any(ShopOrderStatus.class))).thenReturn(true);
        return dao;
    }
}
