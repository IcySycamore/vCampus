package edu.seu.vcampus.server;

import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.client.api.ClientApis;
import edu.seu.vcampus.client.network.ClientMessageDispatcher;
import edu.seu.vcampus.client.network.ClientMessageSender;
import edu.seu.vcampus.client.network.ClientSocketListener;
import edu.seu.vcampus.common.bank.dto.BankTransactionListResponse;
import edu.seu.vcampus.common.bank.dto.BankTransactionQueryRequest;
import edu.seu.vcampus.common.bank.entity.BankAccountStatus;
import edu.seu.vcampus.common.bank.entity.BankTransactionType;
import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.user.entity.Role;
import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 真实服务端入口与客户端 API 的开户、充值、分页和重新登录测试。 */
class BankFlowIntegrationTest {
    @Test
    void bankPageApiCompletesAuthenticatedFlow() throws Exception {
        File directory = Files.createTempDirectory("vcampus-bank-flow").toFile();
        Files.write(new File(directory, "admins.tsv").toPath(),
                "admin\t测试管理员\tadmin123\t管理员\n".getBytes(StandardCharsets.UTF_8));
        String oldUsers = System.getProperty("vcampus.users.file");
        String oldAdmins = System.getProperty("vcampus.admins.file");
        System.setProperty("vcampus.users.file", new File(directory, "users.tsv").getPath());
        System.setProperty("vcampus.admins.file", new File(directory, "admins.tsv").getPath());
        Thread server = new Thread(new Runnable() {
            @Override
            public void run() {
                try { VCampusServerApp.startServer(0); }
                catch (Exception e) { throw new IllegalStateException(e); }
            }
        });
        server.setDaemon(true);
        ClientSocketListener socket = null;
        try {
            server.start();
            long deadline = System.currentTimeMillis() + 5000;
            while (VCampusServerApp.getPort() <= 0 && System.currentTimeMillis() < deadline) {
                Thread.sleep(20);
            }
            assertTrue(VCampusServerApp.getPort() > 0);
            ClientMessageDispatcher dispatcher = new ClientMessageDispatcher();
            socket = new ClientSocketListener("127.0.0.1", VCampusServerApp.getPort(), dispatcher);
            dispatcher.bindSender(new ClientMessageSender(socket));
            final ClientApis apis = ClientApis.create(dispatcher);
            socket.connect();
            apis.user().login("admin", Role.ADMIN, "admin123");
            assertEquals(Command.BANK_ACCOUNT_NOT_OPENED, assertThrows(ApiException.class,
                    new Executable() {
                        @Override public void execute() { apis.bank().queryMyAccount(); }
                    }).getStatusCode());
            final String originalToken = apis.user().currentToken();
            assertThrows(ApiException.class, new Executable() {
                @Override public void execute() {
                    apis.bank().openAccount("admin", "wrong".toCharArray(),
                            "bank12345".toCharArray());
                }
            });
            assertEquals(originalToken, apis.user().currentToken());
            String account = apis.bank().openAccount("admin", "admin123".toCharArray(),
                    "bank12345".toCharArray()).getAccountId();
            assertEquals(originalToken, apis.user().currentToken());
            for (int i = 0; i < 21; i++) {
                apis.bank().recharge(new BigDecimal("1.25"));
            }
            assertEquals(new BigDecimal("26.25"), apis.bank().queryMyAccount().getBalance());
            BankTransactionListResponse page = apis.bank().listMyTransactions(
                    new BankTransactionQueryRequest(2, 20, BankTransactionType.RECHARGE));
            assertEquals(21, page.getTotalCount());
            assertEquals(1, page.getTransactions().size());
            assertEquals(0, apis.bank().listMyTransactions(new BankTransactionQueryRequest(
                    1, 20, BankTransactionType.CONSUMPTION)).getTotalCount());
            apis.user().logout();
            apis.user().login("admin", Role.ADMIN, "admin123");
            assertEquals(account, apis.bank().openAccount("admin", "admin123".toCharArray(),
                    "bank12345".toCharArray()).getAccountId());
            assertEquals(new BigDecimal("26.25"), apis.bank().queryMyAccount().getBalance());
        } finally {
            if (socket != null) { socket.close(); }
            VCampusServerApp.stopServer();
            server.join(3000);
            restore("vcampus.users.file", oldUsers);
            restore("vcampus.admins.file", oldAdmins);
        }
    }

    /** 管理员通过正式装配重置密码后，目标用户立即只能使用新密码。 */
    @Test
    void adminResetPasswordUpdatesTheUserBankAccount() throws Exception {
        File directory = Files.createTempDirectory("vcampus-bank-admin-reset").toFile();
        Files.write(new File(directory, "admins.tsv").toPath(),
                "admin\t测试管理员\tadmin123\t管理员\n".getBytes(StandardCharsets.UTF_8));
        String oldUsers = System.getProperty("vcampus.users.file");
        String oldAdmins = System.getProperty("vcampus.admins.file");
        System.setProperty("vcampus.users.file", new File(directory, "users.tsv").getPath());
        System.setProperty("vcampus.admins.file", new File(directory, "admins.tsv").getPath());
        Thread server = new Thread(new Runnable() {
            @Override
            public void run() {
                try { VCampusServerApp.startServer(0); }
                catch (Exception e) { throw new IllegalStateException(e); }
            }
        });
        server.setDaemon(true);
        ClientSocketListener socket = null;
        try {
            server.start();
            long deadline = System.currentTimeMillis() + 5000;
            while (VCampusServerApp.getPort() <= 0 && System.currentTimeMillis() < deadline) {
                Thread.sleep(20);
            }
            assertTrue(VCampusServerApp.getPort() > 0);
            ClientMessageDispatcher dispatcher = new ClientMessageDispatcher();
            socket = new ClientSocketListener("127.0.0.1", VCampusServerApp.getPort(), dispatcher);
            dispatcher.bindSender(new ClientMessageSender(socket));
            final ClientApis apis = ClientApis.create(dispatcher);
            socket.connect();

            apis.user().login("admin", Role.ADMIN, "admin123");
            apis.userAdmin().register("student", "测试学生", Role.STUDENT, "student123");
            apis.user().logout();

            apis.user().login("student", Role.STUDENT, "student123");
            apis.bank().openAccount("student", "student123".toCharArray(),
                    "old-bank-password".toCharArray());
            apis.user().logout();

            apis.user().login("admin", Role.ADMIN, "admin123");
            assertTrue(apis.bank().resetPassword("student",
                    "new-bank-password".toCharArray()).isOpened());
            apis.user().logout();

            apis.user().login("student", Role.STUDENT, "student123");
            ApiException rejected = assertThrows(ApiException.class, new Executable() {
                @Override public void execute() {
                    apis.bank().freezeAccount("old-bank-password".toCharArray());
                }
            });
            assertEquals(StatusCode.BANK_PASSWORD_INVALID, rejected.getStatusCode());
            assertEquals(BankAccountStatus.FROZEN,
                    apis.bank().freezeAccount("new-bank-password".toCharArray()).getStatus());
        } finally {
            if (socket != null) { socket.close(); }
            VCampusServerApp.stopServer();
            server.join(3000);
            restore("vcampus.users.file", oldUsers);
            restore("vcampus.admins.file", oldAdmins);
        }
    }

    private static void restore(String key, String value) {
        if (value == null) { System.clearProperty(key); }
        else { System.setProperty(key, value); }
    }
}
