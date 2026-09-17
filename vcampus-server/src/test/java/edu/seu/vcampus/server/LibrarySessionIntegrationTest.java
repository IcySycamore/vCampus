package edu.seu.vcampus.server;

import edu.seu.vcampus.client.VCampusClientApp;
import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.client.api.ClientApis;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.library.entity.Book;
import edu.seu.vcampus.common.library.entity.BorrowRecord;
import edu.seu.vcampus.server.db.DatabaseAvailability;
import edu.seu.vcampus.server.db.DbHelper;
import edu.seu.vcampus.server.library.BookDaoJdbc;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 正式双端入口的登录、图书馆借还与登出：走生产启动入口 {@link VCampusServerApp#startServer(int)} 的真实装配路径（真库、真线程池、真网络层）。
 *
 * <p>
 * 本类不注入任何替身 —— 入口本身也不再接受注入。这是有意的：只要能从这里把替身塞进去， 「测试绿了」就不再说明生产那条路能跑。需要一本书来验借还，就往真库里放一本（生产 DAO，
 * 不是替身）；装配补全之后这条链路才第一次被真正执行过。
 */
class LibrarySessionIntegrationTest {

    /**
     * 本类专属的读者账号。
     *
     * <p>
     * 账号库是共用的 MySQL（整个测试套件共一个库），引导导入又是幂等的（已存在就跳过）， 所以同一个登录名只能有一个口令：别处再用同名账号就会把这里的口令顶掉，表现是 「登个录就
     * 401」。账号名按类分开是这套集成测试的硬约定。
     */
    private static final String STUDENT_NAME = "lib_session_student";

    /** 本类专属账号的口令。 */
    private static final String STUDENT_PASSWORD = "secret";

    /** 借还流程用的馆藏编号。 */
    private static final String ISBN = "9787302423287";

    /** 等待服务器开始监听的上限（秒）。 */
    private static final long STARTUP_TIMEOUT_SECONDS = 5L;

    /** 临时目录：引导文件放这里，不污染工作目录。 */
    @TempDir
    Path directory;

    /**
     * 登录后同一张会话表同时驱动用户模块与图书馆模块，借还走真实业务链路，登出后立即失效。
     *
     * @throws Exception 启动或数据准备失败
     */
    @Test
    void existingUserSessionDrivesLibraryOverTheSameRealConnection() throws Exception {
        Assumptions.assumeTrue(DatabaseAvailability.isReady(), "MySQL 不可用，跳过");

        String previousAdmins = System.getProperty("vcampus.admins.file");
        Path bootstrap = directory.resolve("admins.tsv");
        Files.write(bootstrap, (STUDENT_NAME + "\t图书馆测试学生\t" + STUDENT_PASSWORD
                + "\t学生\n").getBytes(StandardCharsets.UTF_8));
        System.setProperty("vcampus.admins.file", bootstrap.toString());
        seedBook();

        ExecutorService pool = Executors.newSingleThreadExecutor();
        Future<Void> server = pool.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                VCampusServerApp.startServer(0);
                return null;
            }
        });
        try {
            long deadline = System.nanoTime()
                    + TimeUnit.SECONDS.toNanos(STARTUP_TIMEOUT_SECONDS);
            while (VCampusServerApp.getPort() <= 0 && System.nanoTime() < deadline) {
                if (server.isDone()) {
                    server.get();
                }
                Thread.sleep(10);
            }
            assertTrue(VCampusServerApp.getPort() > 0);

            final ClientApis apis = VCampusClientApp.connect("127.0.0.1",
                    VCampusServerApp.getPort());
            apis.user().login(STUDENT_NAME, STUDENT_PASSWORD);
            String uuid = apis.user().currentSession().getUuid();
            assertNotEquals(STUDENT_NAME, uuid, "对外标识应是账户 uuid，不是登录名");
            assertSame(apis.user().currentSession(), apis.library().currentSession(),
                    "用户模块与图书馆模块必须共用同一张会话表");
            assertTrue(apis.library().listMyBorrows().isEmpty(), "新账号初始没有借阅");

            BorrowRecord borrowed = apis.library().borrowBook(ISBN);
            assertEquals(uuid, borrowed.getUserId());
            assertEquals(borrowed.getId(),
                    apis.library().returnBook(borrowed.getId()).getId(), "归还的应是同一笔记录");

            // listMyBorrows 是借阅历史（含已归还），不是「当前在借」；用记录自身的状态判定
            List<BorrowRecord> history = apis.library().listMyBorrows();
            assertEquals(1, history.size(), "借还各一次，只应留下一条记录");
            assertTrue(history.get(0).isReturned(), "该记录应已归还");

            apis.user().logout();
            ApiException rejected = assertThrows(ApiException.class, new Executable() {
                @Override
                public void execute() {
                    apis.library().listMyBorrows();
                }
            });
            assertEquals(StatusCode.UNAUTHORIZED, rejected.getStatusCode(),
                    "登出后原 token 必须立即失效");

            ClientApis nextLogin = VCampusClientApp.connect("127.0.0.1",
                    VCampusServerApp.getPort());
            nextLogin.user().login(STUDENT_NAME, STUDENT_PASSWORD);
            VCampusClientApp.stopAsync(apis);
            assertTrue(nextLogin.user().isLoggedIn(), "重新登录应拿到新会话");
            assertEquals(1, nextLogin.library().listMyBorrows().size(), "新会话应看到同一份借阅历史");
        } finally {
            VCampusClientApp.stop();
            VCampusServerApp.stopServer();
            try {
                server.get(STARTUP_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } finally {
                pool.shutdownNow();
                restore("vcampus.admins.file", previousAdmins);
            }
        }
    }

    /**
     * 往真库放一本可借馆藏（用生产 DAO，不用替身）。
     *
     * @throws SQLException 写入失败
     */
    private static void seedBook() throws SQLException {
        Connection connection = DbHelper.getConnection();
        try {
            new BookDaoJdbc().insertBook(connection,
                    new Book(ISBN, "会话联调图书", "测试作者", "计算机", 1, 1));
        } finally {
            connection.close();
        }
    }

    /**
     * 还原系统属性。
     *
     * @param key   属性名
     * @param value 原值；null 表示清除
     */
    private static void restore(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }
}
