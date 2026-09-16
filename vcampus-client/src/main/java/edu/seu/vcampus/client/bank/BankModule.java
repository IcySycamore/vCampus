package edu.seu.vcampus.client.bank;

import edu.seu.vcampus.client.network.ClientMessageDispatcher;
import edu.seu.vcampus.client.user.UserService;

/** 银行客户端装配入口。 */
public final class BankModule {
    private BankModule() {
    }

    /**
     * 创建银行服务并登记连接关闭事件。
     * @param dispatcher 共享分发器
     * @param users 共享用户服务
     * @return 银行 API
     */
    public static BankService register(ClientMessageDispatcher dispatcher, UserService users) {
        BankService service = new BankService(dispatcher, users);
        dispatcher.addConnectionListener(service);
        return service;
    }
}
