package edu.seu.vcampus.client.user;

import edu.seu.vcampus.client.network.ClientMessageDispatcher;

/**
 * 用户管理客户端模块装配入口：与 server 的 {@code AuthModule.register} 对称。
 *
 * <p>
 * 创建服务、登记处理器，并登记「会话随连接失效」的连接事件监听：模块把自己的接线规则带走， 应用入口只需调用本方法，不必了解模块内部构造。
 */
public final class UserModule {

    /** 私有构造器，禁止实例化装配入口。 */
    private UserModule() {
    }

    /**
     * 装配用户管理客户端模块（与 server 的 {@code XxxModule.register} 同名同形）。
     *
     * @param dispatcher 客户端消息分发器（处理器在此按命令码登记）
     * @return 用户管理客户端服务
     * @throws IllegalArgumentException 参数为 null
     */
    public static UserService register(ClientMessageDispatcher dispatcher) {
        if (dispatcher == null) {
            throw new IllegalArgumentException("dispatcher must not be null");
        }
        // 会话随连接失效：连接断开时清空内存会话
        UserService service = new UserService(dispatcher);
        dispatcher.addConnectionListener(service);
        return service;
    }
}
