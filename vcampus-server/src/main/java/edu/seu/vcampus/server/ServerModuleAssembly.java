package edu.seu.vcampus.server;

import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.user.entity.SessionEntry;
import edu.seu.vcampus.server.bank.BankIdentityResolver;
import edu.seu.vcampus.server.bank.BankModule;
import edu.seu.vcampus.server.bank.BankService;
import edu.seu.vcampus.server.library.BankLibraryFinePayment;
import edu.seu.vcampus.server.library.LibraryModule;
import edu.seu.vcampus.server.library.LibraryService;
import edu.seu.vcampus.server.network.ServerMessageDispatcher;
import edu.seu.vcampus.server.student.StudentModule;
import edu.seu.vcampus.server.user.AccountProvisioning;
import edu.seu.vcampus.server.user.AuthModule;
import edu.seu.vcampus.server.user.SessionManager;

/** 将共享会话、银行、学籍和图书馆模块装配到全局分发器。 */
final class ServerModuleAssembly {
    private ServerModuleAssembly() {
    }

    static void register(ServerMessageDispatcher dispatcher, final SessionManager sessions,
            AccountProvisioning provisioning, LibraryService library) {
        StudentModule.register(dispatcher, sessions, provisioning);
        BankService bank = new BankService();
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
    }
}
