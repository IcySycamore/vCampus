package edu.seu.vcampus.server.shop;

import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.shop.ShopCommands;
import edu.seu.vcampus.server.network.ServerMessageDispatcher;
import edu.seu.vcampus.server.user.SessionManager;

/**
 * 商店命令注册入口，由应用组装层提供共享服务和会话管理器。
 */
public final class ShopModule {
    private ShopModule() {
    }

    /**
     * 注册商店普通用户命令，不创建服务或 DAO 实例。
     *
     * @param dispatcher 应用共享的分发器
     * @param sessionManager 会话管理器
     * @param shopService 应用共享的商店服务
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
    }
}
