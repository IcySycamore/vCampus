package edu.seu.vcampus.server.shop;

import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.shop.ShopCommands;
import edu.seu.vcampus.server.bank.BankModule;
import edu.seu.vcampus.server.network.ServerMessageDispatcher;
import edu.seu.vcampus.server.user.SessionManager;

/**
 * 商店命令注册入口。
 *
 * <p>
 * 本模块不持有自己的账户池：支付一律走 {@link BankModule#service()}，否则「用户在界面上开的户」 与「商店扣款时找的户」就在两个实例里，支付必然失败且失败得很安静。
 */
public final class ShopModule {
    private ShopModule() {
    }

    /**
     * 生产装配：商店服务与银行适配器全部指向银行模块的账户池。
     *
     * @param dispatcher 应用共享的分发器
     * @param sessions   账号模块的全服唯一会话表
     */
    public static void register(ServerMessageDispatcher dispatcher, SessionManager sessions) {
        register(dispatcher, sessions,
                new ShopService(new ShopDaoImpl(), new BankAdapter(BankModule.service())));
    }

    /**
     * 注册商店命令 501-509，不创建服务或 DAO 实例。
     *
     * @param dispatcher     应用共享的分发器
     * @param sessionManager 会话管理器
     * @param shopService    应用共享的商店服务
     */
    public static void register(ServerMessageDispatcher dispatcher,
            SessionManager sessionManager, ShopService shopService) {
        if (dispatcher == null) {
            throw new IllegalArgumentException("dispatcher must not be null");
        }
        if (sessionManager == null) {
            throw new IllegalArgumentException("sessionManager must not be null");
        }
        if (shopService == null) {
            throw new IllegalArgumentException("shopService must not be null");
        }

        ShopMessageHandler handler = new ShopMessageHandler(shopService, sessionManager);
        dispatcher.register(Command.SHOP_ITEM_LIST, handler);
        dispatcher.register(Command.SHOP_ITEM_DETAIL, handler);
        dispatcher.register(Command.SHOP_ORDER_CREATE, handler);
        dispatcher.register(Command.SHOP_ORDER_LIST, handler);
        dispatcher.register(Command.SHOP_ORDER_DETAIL, handler);
        dispatcher.register(Command.SHOP_ORDER_CANCEL, handler);
        dispatcher.register(Command.SHOP_ORDER_PAY, handler);
        dispatcher.register(ShopCommands.ORDER_QUANTITY_UPDATE, handler);
        dispatcher.register(Command.SHOP_ORDER_ADVANCE, handler);
        dispatcher.register(Command.SHOP_ITEM_UPSERT, handler);
        dispatcher.register(Command.SHOP_ORDER_QUERY, handler);
    }
}
