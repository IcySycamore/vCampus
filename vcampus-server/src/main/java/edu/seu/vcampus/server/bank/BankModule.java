package edu.seu.vcampus.server.bank;

import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.server.network.ServerMessageDispatcher;

/** 银行命令注册入口，由应用组装层提供共享服务和可信身份解析器。 */
public final class BankModule {
    private BankModule() {
    }

    /**
     * 注册银行四条命令，不创建会话池或替调用方分配用户主键。
     * @param dispatcher 应用共享的分发器
     * @param service 应用共享的银行服务
     * @param identityResolver 返回稳定用户主键的可信身份解析器
     */
    public static void register(ServerMessageDispatcher dispatcher, BankService service,
            BankIdentityResolver identityResolver) {
        if (dispatcher == null) {
            throw new IllegalArgumentException("dispatcher must not be null");
        }
        BankMessageHandler handler = new BankMessageHandler(service, identityResolver);
        dispatcher.register(Command.BANK_ACCOUNT_QUERY, handler);
        dispatcher.register(Command.BANK_RECHARGE, handler);
        dispatcher.register(Command.BANK_TRANSACTION_LIST, handler);
        dispatcher.register(Command.BANK_ACCOUNT_OPEN, handler);
    }
}
