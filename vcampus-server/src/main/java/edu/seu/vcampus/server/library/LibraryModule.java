package edu.seu.vcampus.server.library;

import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.message.MessageHandler;
import edu.seu.vcampus.common.message.MessageSender;
import edu.seu.vcampus.server.bank.BankModule;
import edu.seu.vcampus.server.network.ServerMessageDispatcher;
import edu.seu.vcampus.server.user.AccountProvisioning;
import edu.seu.vcampus.server.user.SessionManager;

/**
 * 图书馆模块装配入口。
 *
 * <p>
 * 本模块持有连接来源与四个 DAO 的单例，{@link #service()} 由它们组成。罚款走其他模块的 支付能力，因此本模块必须排在银行模块之后装配。
 */
public final class LibraryModule {

    /** 图书馆业务服务单例。 */
    private static volatile LibraryService s_service;

    /** 连接来源单例。 */
    private static volatile LibraryConnectionSource s_source;

    /** 读者账户 DAO 单例。 */
    private static volatile LibraryAccountDao s_accounts;

    /** 馆藏 DAO 单例。 */
    private static volatile BookDao s_books;

    /** 借阅 DAO 单例。 */
    private static volatile BorrowDao s_borrows;

    /** 预约 DAO 单例。 */
    private static volatile ReservationDao s_reservations;

    private LibraryModule() {
    }

    /**
     * 取连接来源单例
     *
     * @return 连接来源
     */
    public static LibraryConnectionSource source() {
        LibraryConnectionSource source = s_source;
        if (source == null) {
            synchronized (LibraryModule.class) {
                source = s_source;
                if (source == null) {
                    source = new LibraryConnectionSourceJdbc();
                    s_source = source;
                }
            }
        }
        return source;
    }

    /**
     * 取读者账户 DAO 单例。
     *
     * @return 读者账户 DAO
     */
    public static LibraryAccountDao accountDao() {
        LibraryAccountDao dao = s_accounts;
        if (dao == null) {
            synchronized (LibraryModule.class) {
                dao = s_accounts;
                if (dao == null) {
                    dao = new LibraryAccountDaoJdbc();
                    s_accounts = dao;
                }
            }
        }
        return dao;
    }

    /**
     * 取馆藏 DAO 单例。
     *
     * @return 馆藏 DAO
     */
    public static BookDao bookDao() {
        BookDao dao = s_books;
        if (dao == null) {
            synchronized (LibraryModule.class) {
                dao = s_books;
                if (dao == null) {
                    dao = new BookDaoJdbc();
                    s_books = dao;
                }
            }
        }
        return dao;
    }

    /**
     * 取借阅 DAO 单例。
     *
     * @return 借阅 DAO
     */
    public static BorrowDao borrowDao() {
        BorrowDao dao = s_borrows;
        if (dao == null) {
            synchronized (LibraryModule.class) {
                dao = s_borrows;
                if (dao == null) {
                    dao = new BorrowDaoJdbc();
                    s_borrows = dao;
                }
            }
        }
        return dao;
    }

    /**
     * 取预约 DAO 单例。
     *
     * @return 预约 DAO
     */
    public static ReservationDao reservationDao() {
        ReservationDao dao = s_reservations;
        if (dao == null) {
            synchronized (LibraryModule.class) {
                dao = s_reservations;
                if (dao == null) {
                    dao = new ReservationDaoJdbc();
                    s_reservations = dao;
                }
            }
        }
        return dao;
    }

    /**
     * 取图书馆业务服务单例：连接来源与四个 DAO 全部指向数据库。
     *
     * <p>
     * 不再提供内存回退。缺库属于配置错误，取连接时就会失败 —— 测试路径也不例外， 那正是重点：测试不该活在一条生产根本不存在的装配路径上。
     *
     * @return 图书馆服务单例
     */
    public static LibraryService service() {
        LibraryService service = s_service;
        if (service == null) {
            synchronized (LibraryModule.class) {
                service = s_service;
                if (service == null) {
                    service = new LibraryService(source(), accountDao(), bookDao(),
                            borrowDao(), reservationDao());
                    s_service = service;
                }
            }
        }
        return service;
    }

    /**
     * 生产装配：图书馆服务、罚款支付与读者开户钩子全部取自本模块与银行模块的单例。
     *
     * @param dispatcher   共享分发器
     * @param sessions     认证模块的共享会话表
     * @param provisioning 用户账户生命周期；null 表示不自动建读者账户
     */
    public static void register(ServerMessageDispatcher dispatcher, SessionManager sessions,
            AccountProvisioning provisioning) {
        register(dispatcher, sessions, provisioning, null);
    }

    /**
     * 注册图书馆命令，可用外部注入的业务服务覆盖本模块单例（测试注入替身走这条）。
     *
     * @param dispatcher   共享分发器
     * @param sessions     认证模块的共享会话表
     * @param provisioning 用户账户生命周期；null 表示不自动建读者账户
     * @param injected     注入的业务服务；null 表示用本模块单例
     */
    public static void register(ServerMessageDispatcher dispatcher, SessionManager sessions,
            AccountProvisioning provisioning, LibraryService injected) {
        register(dispatcher, sessions, injected == null ? service() : injected,
                new BankLibraryFinePayment(BankModule.service()), provisioning);
    }

    /**
     * 登记图书馆支持的命令。
     * 
     * @param dispatcher 共享分发器
     * @param sessions   认证模块的共享会话表
     * @param service    注入数据源和 DAO 的业务服务
     */
    public static void register(ServerMessageDispatcher dispatcher, SessionManager sessions,
            LibraryService service) {
        register(dispatcher, sessions, service, null, null);
    }

    /**
     * 登记图书馆命令，并把读者账户接入用户账户生命周期。
     * 
     * @param dispatcher   共享分发器
     * @param sessions     认证模块的共享会话表
     * @param service      图书馆业务服务
     * @param payment      校园银行罚款支付接口
     * @param provisioning 用户账户生命周期；null 表示不自动建读者账户
     */
    public static void register(ServerMessageDispatcher dispatcher, SessionManager sessions,
            LibraryService service, LibraryFinePayment payment,
            AccountProvisioning provisioning) {
        if (dispatcher == null || sessions == null) {
            throw new IllegalArgumentException("dispatcher and sessions must not be null");
        }
        if (service == null) {
            throw new IllegalArgumentException("service must not be null");
        }
        MessageHandler handler = new LibraryMessageHandler(service, sessions, payment);
        dispatcher.register(Command.LIBRARY_SEARCH, Command.LIBRARY_RENEW, handler);
        dispatcher.register(Command.LIBRARY_CREATE_BOOK,
                Command.LIBRARY_POPULAR_BORROWS, handler);
        LibraryAccountRegistration.register(service, provisioning);
    }
}
