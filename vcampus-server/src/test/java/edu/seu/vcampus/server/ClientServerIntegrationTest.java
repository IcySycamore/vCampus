package edu.seu.vcampus.server;

import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.user.Role;
import edu.seu.vcampus.server.auth.AuthService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 总分循环集成测试：以客户端模块真实的网络层（{@link IntegrationTestClient}，
 * 内部即 {@code ClientSocket}：握手、心跳调度、接收线程、优雅关闭）连接<b>真实运行的
 * 服务器</b>，验证「client 网络层 ↔ TCP ↔ server 监听/线程池/连接级鉴权/分发/响应回传」
 * 整条链路。
 *
 * <p>两端都不使用测试替身：服务端走生产入口 {@link VCampusServerApp#startServer(int)}
 * 的真实装配路径（线程池 + {@code ClientThread} + 全局分发器 + 处理器），客户端走产品里
 * 真正运行的同名类。因此本测试通过即意味着真实启动路径可用，而不是"测试专用路径"可用。
 *
 * <p>覆盖点：未携带 token 的命令被连接层拦下、挑战-应答登录、鉴权通过后进入分发器、
 * 登出后 token 立即失效、心跳维持长连接跨过服务端闲置阈值。
 */
class ClientServerIntegrationTest {

    /** 预置管理员账号。 */
    private static final String ADMIN_NAME = "loop_admin";

    /** 预置管理员密码。 */
    private static final String ADMIN_PASSWORD = "loop_pwd_2026";

    /** 等待服务器开始监听的上限（毫秒）。 */
    private static final long STARTUP_TIMEOUT_MILLIS = 5000L;

    /** 服务端连接闲置阈值（毫秒），与 {@code ClientThread} 的 soTimeout 保持一致。 */
    private static final long SERVER_IDLE_TIMEOUT_MILLIS = 15000L;

    /** 服务端线程。 */
    private static Thread s_serverThread;

    /** 实际监听端口。 */
    private static int s_port;

    /**
     * 启动测试服务器并预置管理员。
     *
     * @throws Exception 启动失败
     */
    @BeforeAll
    static void startServer() throws Exception {
        // 注册命令要求管理员会话，冷启动时库中无任何账号，故直接经认证服务落库。
        try {
            AuthService.getInstance().register(ADMIN_NAME, ADMIN_PASSWORD,
                    Role.ADMIN.getDisplayName());
        } catch (IllegalStateException alreadyExists) {
            // 认证服务为全局单例，重复启动时账号已存在，忽略即可
        }

        s_serverThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // 端口 0：由系统分配随机端口，避免与本机占用冲突
                    VCampusServerApp.startServer(0);
                } catch (IOException e) {
                    System.err.println("测试服务器退出: " + e.getMessage());
                }
            }
        }, "loop-server");
        s_serverThread.setDaemon(true);
        s_serverThread.start();

        final long deadline = System.currentTimeMillis()
                + STARTUP_TIMEOUT_MILLIS;
        while (s_port <= 0 && System.currentTimeMillis() < deadline) {
            s_port = VCampusServerApp.getPort();
            if (s_port <= 0) {
                Thread.sleep(20L);
            }
        }
        assertTrue(s_port > 0, "测试服务器未在 5 秒内开始监听");
    }

    /**
     * 停止测试服务器。
     *
     * @throws Exception 停止失败
     */
    @AfterAll
    static void stopServer() throws Exception {
        VCampusServerApp.stopServer();
        s_serverThread.join(3000L);
    }

    /**
     * 未携带 token 的业务命令应由连接层直接拦下回 401，
     * 且该拒绝响应须能被客户端按 uid 配对（不能因缺 uid 而"看起来像超时"）。
     *
     * @throws Exception 通信失败
     */
    @Test
    void unauthenticatedCommandIsRejectedByConnectionLayer() throws Exception {
        IntegrationTestClient client = new IntegrationTestClient(s_port);
        try {
            client.connect();

            Message response = client.request(null,
                    new Message(Command.USER_LOGOUT, null));

            assertEquals(StatusCode.UNAUTHORIZED, response.getStatusCode(),
                    "未带 token 的业务命令应由连接层拦下回 401");
            assertTrue(client.isConnected(),
                    "拦下未鉴权命令不应断开连接");
        } finally {
            client.close();
        }
    }

    /**
     * 登录与登出：挑战-应答拿到 token、鉴权通过后进入分发器、登出后 token 立即失效。
     *
     * @throws Exception 通信失败
     */
    @Test
    void clientCanLoginAndLogout() throws Exception {
        IntegrationTestClient client = new IntegrationTestClient(s_port);
        try {
            client.connect();
            String token = client.login(ADMIN_NAME, ADMIN_PASSWORD);
            assertNotNull(token, "登录（挑战-应答）应拿到 token");

            // 带 token 的命令已通过连接级鉴权并进入分发器。本分支未注册用户列表处理器，
            // 因此分发器回 400（而不是 401/403），据此可确认鉴权已放行。
            Message routed = client.request(token,
                    new Message(Command.USER_LIST, null));
            assertEquals(StatusCode.BAD_REQUEST, routed.getStatusCode(),
                    "带 token 的命令应通过鉴权并进入分发器（未注册命令回 400）");

            // 登出 → 200
            assertEquals(StatusCode.SUCCESS,
                    client.request(token,
                            new Message(Command.USER_LOGOUT, null))
                            .getStatusCode(),
                    "登出应成功");

            // 原 token 已失效 → 401
            assertEquals(StatusCode.UNAUTHORIZED,
                    client.request(token,
                            new Message(Command.USER_LOGOUT, null))
                            .getStatusCode(),
                    "登出后原 token 应立即失效");
        } finally {
            client.close();
        }
    }

    /**
     * 心跳保活：服务端连接的闲置阈值是 15 秒（{@code ClientThread} 的 soTimeout），
     * 客户端每 3 秒发一次心跳；闲置超过该阈值后连接应仍然可用，证明两端的心跳约定
     * （命令码均为 1）确实生效。
     *
     * <p>注意：心跳属网络层内部机制，客户端 {@code MessageReceiver} 会把它过滤掉、
     * 不回调到 UI 层，因此无法从 UI 回调观察 ACK，只能用「连接是否存活 + 命令是否
     * 仍可往返」间接验证；同时客户端读超时为 10 秒，也只有持续收到 ACK 才不会被判超时。
     *
     * @throws Exception 通信失败
     */
    @Test
    void heartbeatKeepsIdleConnectionAlive() throws Exception {
        IntegrationTestClient client = new IntegrationTestClient(s_port);
        try {
            client.connect();
            String token = client.login(ADMIN_NAME, ADMIN_PASSWORD);
            assertNotNull(token, "登录应拿到 token");

            // 闲置超过服务端 15 秒空闲阈值
            Thread.sleep(SERVER_IDLE_TIMEOUT_MILLIS + 2000L);

            assertTrue(client.isConnected(),
                    "客户端心跳应让连接跨过服务端 15 秒空闲阈值而不断开");

            assertEquals(StatusCode.BAD_REQUEST,
                    client.request(token, new Message(Command.USER_LIST, null))
                            .getStatusCode(),
                    "跨过空闲阈值后命令仍应正常往返");
        } finally {
            client.close();
        }
    }
}
