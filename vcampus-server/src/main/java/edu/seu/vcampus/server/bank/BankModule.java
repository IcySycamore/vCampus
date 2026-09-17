package edu.seu.vcampus.server.bank;

import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.user.entity.SessionEntry;
import edu.seu.vcampus.server.network.ServerMessageDispatcher;
import edu.seu.vcampus.server.user.AuthModule;
import edu.seu.vcampus.server.user.AuthService;
import edu.seu.vcampus.server.user.SessionManager;
import edu.seu.vcampus.server.user.UserRepository;

/**
 * 银行命令注册入口。
 *
 * <p>
 * 本模块持有全服唯一的 {@link BankService}：商店模块与图书馆罚款都要复用同一个账户池， 自建一份就会「在另一个实例里找用户」，症状是支付 / 罚款必然失败。
 */
public final class BankModule {

    /** 银行服务进程级单例。 */
    private static volatile BankService s_service;

    private BankModule() {
    }

    /**
     * 取全服唯一的银行服务（账户池落地 MySQL，构造时把已落库的账户与流水读回来）。
     *
     * @return 银行服务单例
     */
    public static BankService service() {
        BankService service = s_service;
        if (service == null) {
            synchronized (BankModule.class) {
                service = s_service;
                if (service == null) {
                    service = new BankService(new BankStoreJdbc());
                    s_service = service;
                }
            }
        }
        return service;
    }

    /**
     * 生产装配：银行账户池、身份解析与账户库全部取自本模块与账号模块的单例。
     *
     * @param dispatcher 应用共享的分发器
     * @param sessions   账号模块的全服唯一会话表
     * @throws IllegalArgumentException 参数为 null
     */
    public static void register(ServerMessageDispatcher dispatcher, SessionManager sessions) {
        if (sessions == null) {
            throw new IllegalArgumentException("sessions must not be null");
        }
        register(dispatcher, service(), AuthModule.authService(),
                identityResolver(sessions), AuthModule.repository());
    }

    /**
     * 以会话表为准解析调用者 uuid：token 无效则不给身份，业务侧自行判空。
     *
     * @param sessions 全服唯一会话表
     * @return 身份解析器
     */
    private static BankIdentityResolver identityResolver(final SessionManager sessions) {
        return new BankIdentityResolver() {
            @Override
            public String resolveOwnerUuid(Message request) {
                SessionEntry entry = request == null ? null
                        : sessions.validate(request.getToken());
                return entry == null ? null : entry.getUuid();
            }
        };
    }

    /**
     * 注册银行命令，并注入共享用户仓库以启用管理轨（610-614）。
     *
     * <p>
     * 这是本模块唯一的登记入口
     *
     * @param dispatcher       应用共享的分发器
     * @param service          应用共享的银行服务
     * @param auth             与用户模块相同的认证服务
     * @param identityResolver 返回稳定用户主键的可信身份解析器
     * @param users            共享用户仓库；null 表示未装配，管理轨命令回500
     */
    public static void register(ServerMessageDispatcher dispatcher, BankService service,
            AuthService auth, BankIdentityResolver identityResolver, UserRepository users) {
        if (dispatcher == null) {
            throw new IllegalArgumentException("dispatcher must not be null");
        }
        if (auth == null) {
            throw new IllegalArgumentException("auth must not be null");
        }
        BankAdminService admin = users == null ? null : new BankAdminService(service, users);
        BankMessageHandler handler = new BankMessageHandler(service, identityResolver, auth, admin);
        dispatcher.register(Command.BANK_ACCOUNT_QUERY, handler);
        dispatcher.register(Command.BANK_RECHARGE, handler);
        dispatcher.register(Command.BANK_TRANSACTION_LIST, handler);
        dispatcher.register(Command.BANK_ACCOUNT_OPEN, handler);
        dispatcher.register(Command.BANK_ACCOUNT_FREEZE, handler);
        dispatcher.register(Command.BANK_ACCOUNT_UNFREEZE, handler);
        dispatcher.register(Command.BANK_PASSWORD_CHANGE, handler);
        dispatcher.register(Command.BANK_PASSWORD_VERIFY_CHALLENGE, handler);
        dispatcher.register(Command.BANK_PASSWORD_VERIFY, handler);
        dispatcher.register(Command.BANK_ADMIN_LIST_ACCOUNTS, handler);
        dispatcher.register(Command.BANK_ADMIN_QUERY_ACCOUNT, handler);
        dispatcher.register(Command.BANK_ADMIN_TRANSACTION_LIST, handler);
        dispatcher.register(Command.BANK_ADMIN_SET_FROZEN, handler);
        dispatcher.register(Command.BANK_ADMIN_RESET_PASSWORD, handler);
    }
}
