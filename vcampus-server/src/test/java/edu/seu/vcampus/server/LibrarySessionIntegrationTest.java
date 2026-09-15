package edu.seu.vcampus.server;

import edu.seu.vcampus.client.VCampusClientApp;
import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.client.api.ClientApis;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.library.entity.BorrowRecord;
import edu.seu.vcampus.common.library.entity.LibraryAccount;
import edu.seu.vcampus.common.user.entity.Role;
import edu.seu.vcampus.server.library.LibraryAccountDao;
import edu.seu.vcampus.server.library.LibraryAccountProvisioner;
import edu.seu.vcampus.server.library.LibraryService;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 正式双端入口的登录、图书馆借还和登出；仅数据库业务服务使用替身。 */
class LibrarySessionIntegrationTest {
    @TempDir
    Path directory;

    @Test
    void existingUserSessionDrivesLibraryOverTheSameRealConnection() throws Exception {
        String previousUsers = System.getProperty("vcampus.users.file");
        String previousAdmins = System.getProperty("vcampus.admins.file");
        Path bootstrap = directory.resolve("admins.tsv");
        Files.write(bootstrap, "001\t图书馆测试学生\tsecret\t学生\n"
                .getBytes(StandardCharsets.UTF_8));
        System.setProperty("vcampus.users.file", directory.resolve("users.tsv").toString());
        System.setProperty("vcampus.admins.file", bootstrap.toString());
        final LibraryService library = mock(LibraryService.class);
        LibraryAccountDao accounts = mock(LibraryAccountDao.class);
        when(accounts.insert(any(LibraryAccount.class))).thenReturn(true);
        when(library.getAccountProvisioner()).thenReturn(
                new LibraryAccountProvisioner(accounts));
        ExecutorService pool = Executors.newSingleThreadExecutor();
        Future<Void> server = pool.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                VCampusServerApp.startServer(0, library);
                return null;
            }
        });
        try {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (VCampusServerApp.getPort() <= 0 && System.nanoTime() < deadline) {
                if (server.isDone()) {
                    server.get();
                }
                Thread.sleep(10);
            }
            assertTrue(VCampusServerApp.getPort() > 0);
            final ClientApis apis = VCampusClientApp.connect("127.0.0.1",
                    VCampusServerApp.getPort());
            apis.user().login("001", Role.STUDENT, "secret");
            String uuid = apis.user().currentSession().getUuid();
            assertNotEquals("001", uuid);
            assertSame(apis.user().currentSession(), apis.library().currentSession());
            when(library.listBorrows(uuid)).thenReturn(Collections.<BorrowRecord>emptyList());
            BorrowRecord record = new BorrowRecord(uuid, "9787302423287", "Java",
                    new Date(), new Date());
            record.setId(1L);
            when(library.borrow(uuid, record.getIsbn())).thenReturn(record);
            when(library.returnBook(uuid, 1L)).thenReturn(record);
            assertTrue(apis.library().listMyBorrows().isEmpty());
            assertEquals(uuid, apis.library().borrowBook(record.getIsbn()).getUserId());
            assertEquals(Long.valueOf(1L), apis.library().returnBook(1L).getId());
            verify(library).borrow(uuid, record.getIsbn());
            verify(library).returnBook(uuid, 1L);
            apis.user().logout();
            ApiException rejected = assertThrows(ApiException.class, new Executable() {
                @Override
                public void execute() {
                    apis.library().listMyBorrows();
                }
            });
            assertEquals(StatusCode.UNAUTHORIZED, rejected.getStatusCode());
            ClientApis nextLogin = VCampusClientApp.connect("127.0.0.1",
                    VCampusServerApp.getPort());
            nextLogin.user().login("001", Role.STUDENT, "secret");
            VCampusClientApp.stopAsync(apis);
            assertTrue(nextLogin.user().isLoggedIn());
            assertTrue(nextLogin.library().listMyBorrows().isEmpty());
        } finally {
            VCampusClientApp.stop();
            VCampusServerApp.stopServer();
            try {
                server.get(5, TimeUnit.SECONDS);
            } finally {
                pool.shutdownNow();
                restore("vcampus.users.file", previousUsers);
                restore("vcampus.admins.file", previousAdmins);
            }
        }
    }

    private void restore(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }
}
