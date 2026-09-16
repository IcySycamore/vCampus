package edu.seu.vcampus.client.shop;

import edu.seu.vcampus.client.network.ClientMessageDispatcher;
import edu.seu.vcampus.client.user.UserService;

/**
 * 商店客户端装配入口。
 */
public final class ShopModule {
    private ShopModule() {
    }

    /**
     * 创建商店服务并登记连接关闭事件。
     *
     * @param dispatcher 共享分发器
     * @param users 共享用户服务
     * @return 商店 API
     */
    public static ShopService register(ClientMessageDispatcher dispatcher, UserService users) {
        ShopService service = new ShopService(dispatcher, users);
        dispatcher.addConnectionListener(service);
        return service;
    }
}
