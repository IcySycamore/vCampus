package edu.seu.vcampus.server;

import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.user.entity.SessionEntry;
import edu.seu.vcampus.server.bank.BankIdentityResolver;
import edu.seu.vcampus.server.bank.BankModule;
import edu.seu.vcampus.server.bank.BankService;
import edu.seu.vcampus.server.bank.BankStoreJdbc;
import edu.seu.vcampus.server.bank.BankStoreMemory;
import edu.seu.vcampus.server.course.CourseModule;
import edu.seu.vcampus.server.library.BankLibraryFinePayment;
import edu.seu.vcampus.server.library.LibraryModule;
import edu.seu.vcampus.server.library.LibraryService;
import edu.seu.vcampus.server.network.ServerMessageDispatcher;
import edu.seu.vcampus.server.shop.BankAdapter;
import edu.seu.vcampus.server.shop.ShopDaoImpl;
import edu.seu.vcampus.server.shop.ShopModule;
import edu.seu.vcampus.server.shop.ShopService;
import edu.seu.vcampus.server.student.StudentModule;
import edu.seu.vcampus.server.user.AccountProvisioning;
import edu.seu.vcampus.server.user.AuthModule;
import edu.seu.vcampus.server.user.SessionManager;

/** 将共享会话、银行、学籍、选课和图书馆模块装配到全局分发器。 */
final class ServerModuleAssembly {
    private ServerModuleAssembly() {
    }

    static void register(ServerMessageDispatcher dispatcher, final SessionManager sessions,
            AccountProvisioning provisioning, LibraryService library) {
        StudentModule.register(dispatcher, sessions, provisioning);
        CourseModule.register(dispatcher, sessions, provisioning);
        // 与用户/学籍/图书馆同一套开关：缺省不落库，-Dvcampus.store=jdbc 时账户与流水进 MySQL
        boolean jdbc = "jdbc".equalsIgnoreCase(System.getProperty("vcampus.store"));
        BankService bank = new BankService(jdbc ? new BankStoreJdbc() : new BankStoreMemory());
        BankIdentityResolver identity = new BankIdentityResolver() {
            @Override
            public String resolveOwnerUuid(Message request) {
                SessionEntry entry = request == null ? null
                        : sessions.validate(request.getToken());
                return entry == null ? null : entry.getUuid();
            }
        };
        BankModule.register(dispatcher, bank, AuthModule.authService(), identity,
                AuthModule.repository());
        LibraryModule.register(dispatcher, sessions, library,
                new BankLibraryFinePayment(bank), provisioning);
        // 商店必须共用上面这个 bank 实例：ShopService 的便利构造器内部会 new BankService()，
        // 另造一个账户池，支付时查不到用户在该实例开的户，扣款必然失败。
        ShopModule.register(dispatcher, sessions,
                new ShopService(new ShopDaoImpl(), new BankAdapter(bank)));
    }
}
