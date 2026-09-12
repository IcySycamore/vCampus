package edu.seu.vcampus.server;

import edu.seu.vcampus.server.user.AuthService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 演示账号登录端到端测试：走生产启动入口 {@link VCampusServerApp#startServer(int)}，用客户端模块真实的
 * 连接层与分发器完成「挑战 → 验证」，验证装配期预置的演示账号可直接登录， 且签发的 token 落在全局会话表中（即「刚登录就 401」的回归防线）。
 */
class AuthFlowIntegrationTest {

    /** 等待服务器开始监听的上限（毫秒）。 */
    private static final long STARTUP_TIMEOUT_MILLIS = 5000L;

    /** 演示学生账号。 */
    private static final String DEMO_STUDENT = "001";

    /** 演示账号密码。 */
    private static final String DEMO_PASSWORD = "1";

    /** 服务端线程。 */
    private static Thread s_serverThread;

    /** 实际监听端口。 */
    private static int s_port;

    /** 以随机端口启动真实服务器并等待监听就绪。 */
    @BeforeAll
    static void startServer() throws Exception {
        // 账号由服务器本地引导文件导入（账户库同样落在临时目录，不污染工作目录）。
        File directory = Files.createTempDirectory("vcampus-authflow").toFile();
        directory.deleteOnExit();
        File bootstrap = new File(directory, "admins.tsv");
        Writer writer = new OutputStreamWriter(new FileOutputStream(bootstrap),
                Charset.forName("UTF-8"));
        try {
            writer.write(DEMO_STUDENT + "\t演示学生\t" + DEMO_PASSWORD + "\t学生\n");
        } finally {
            writer.close();
        }
        System.setProperty("vcampus.users.file", new File(directory, "users.tsv").getPath());
        System.setProperty("vcampus.admins.file", bootstrap.getPath());

        s_serverThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    VCampusServerApp.startServer(0);
                } catch (IOException e) {
                    System.err.println("测试服务器退出: " + e.getMessage());
                }
            }
        }, "auth-flow-server");
        s_serverThread.setDaemon(true);
        s_serverThread.start();

        final long deadline = System.currentTimeMillis() + STARTUP_TIMEOUT_MILLIS;
        while (s_port <= 0 && System.currentTimeMillis() < deadline) {
            s_port = VCampusServerApp.getPort();
            if (s_port <= 0) {
                Thread.sleep(20L);
            }
        }
        assertTrue(s_port > 0, "测试服务器未在 5 秒内开始监听");
    }

    /** 停止测试服务器。 */
    @AfterAll
    static void stopServer() throws Exception {
        VCampusServerApp.stopServer();
        s_serverThread.join(3000L);
    }

    /** 演示账号可完成挑战-应答登录，拿到的 token 与角色均正确。 */
    @Test
    void demoAccountLoginEndToEnd() throws Exception {
        IntegrationTestClient client = new IntegrationTestClient(s_port);
        try {
            client.connect();
            String token = client.login(DEMO_STUDENT, DEMO_PASSWORD);

            assertNotNull(token, "演示账号应能完成挑战-应答登录");
            assertEquals("学生", client.role(), "登录角色应与演示账号一致");
            assertNotNull(AuthService.getInstance().validateToken(token),
                    "登录签发的 token 必须在全局会话表中可校验");
        } finally {
            client.close();
        }
    }
}
