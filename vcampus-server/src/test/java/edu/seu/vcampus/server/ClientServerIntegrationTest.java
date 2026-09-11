package edu.seu.vcampus.server;

import edu.seu.vcampus.client.handler.UIUpdateHandler;
import edu.seu.vcampus.client.network.ClientNetworkConfig;
import edu.seu.vcampus.client.network.ClientSocket;
import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.entity.EnrollmentStatus;
import edu.seu.vcampus.common.entity.StudentProfile;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.user.Role;
import edu.seu.vcampus.common.user.dto.LoginChallenge;
import edu.seu.vcampus.common.user.dto.LoginRequest;
import edu.seu.vcampus.common.user.dto.LoginResponse;
import edu.seu.vcampus.common.user.dto.LoginVerify;
import edu.seu.vcampus.common.util.Sha256Util;
import edu.seu.vcampus.server.auth.AuthService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 总分循环集成测试：以 <b>客户端模块的真实网络层</b>（{@link ClientSocket}：含握手、
 * 心跳、接收线程、优雅关闭）连接<b>真实运行的服务器</b>，验证
 * 「client 网络层 ↔ TCP ↔ server 监听/线程池/连接级鉴权/分发/业务处理 ↔ 响应回传」
 * 这一整条链路，而不是任何一层用替身。
 *
 * <p>与 {@code ServerEndToEndTest} 的区别：那边用手写的裸 socket 客户端（只验服务端），
 * 这边用的是产品里真正跑的客户端网络实现，因此能额外覆盖双方约定的对齐情况：
 * 心跳命令码（两边都为 1）、心跳 ACK 处理、超出心跳周期的长连接保持、以及
 * 服务端改动后客户端能否看到新值（序列化方向）。
 */
class ClientServerIntegrationTest {

    /** 预置管理员账号。 */
    private static final String ADMIN_NAME = "loop_admin";

    /** 预置管理员密码。 */
    private static final String ADMIN_PASSWORD = "loop_pwd_2026";

    /** 等待响应的超时（毫秒）。 */
    private static final long RESPONSE_TIMEOUT_MILLIS = 5000L;

    /** 等待服务器开始监听的上限（毫秒）。 */
    private static final long STARTUP_TIMEOUT_MILLIS = 5000L;

    /** 探测学籍主键的扫描上限。 */
    private static final long PROBE_MAX_ID = 50L;

    /** 服务端连接闲置阈值（毫秒），与 {@code ClientThread} 的 soTimeout 保持一致。 */
    private static final long SERVER_IDLE_TIMEOUT_MILLIS = 15000L;

    /** 请求 uid 发号器，用于把响应与请求精确配对。 */
    private static final AtomicLong NEXT_UID = new AtomicLong(1L);

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
     * 客户端全流程：连接 → 登录 → 查空缺 → 登记 → 改状态 → 再查（验证看到新值）。
     *
     * @throws Exception 通信失败
     */
    @Test
    void clientCanLoginAndDriveStudentCommands() throws Exception {
        CollectingHandler handler = new CollectingHandler();
        ClientSocket client = new ClientSocket("127.0.0.1", s_port, handler,
                testConfig());
        try {
            client.connect();
            assertTrue(client.isConnected(), "客户端应成功连上服务器");

            String token = login(client, handler, ADMIN_NAME, ADMIN_PASSWORD);
            assertNotNull(token, "登录（挑战-应答）应拿到 token");

            // 201 查不存在的学籍 → 404
            assertEquals(StatusCode.NOT_FOUND,
                    query(client, handler, token, 9999L).getStatusCode(),
                    "查询不存在的学籍应回 404");

            // 204 登记学籍 → 200
            StudentProfile profile = new StudentProfile("uuid-loop-1", 2026,
                    EnrollmentStatus.ENROLLED);
            assertEquals(StatusCode.SUCCESS,
                    request(client, handler, token,
                            new Message(Command.STUDENT_REGISTER, profile))
                            .getStatusCode(),
                    "管理员登记学籍应成功");

            // 主键由服务端自增分配，客户端按序探测定位
            long allocatedId = findProfileId(client, handler, token,
                    "uuid-loop-1");
            assertTrue(allocatedId > 0, "登记后应能查到该学籍记录");

            // 206 改学籍状态 → 200
            StudentProfile statusChange = new StudentProfile();
            statusChange.setId(allocatedId);
            statusChange.setStatus(EnrollmentStatus.GRADUATED);
            assertEquals(StatusCode.SUCCESS,
                    request(client, handler, token,
                            new Message(Command.STUDENT_CHANGE_STATUS,
                                    statusChange)).getStatusCode(),
                    "管理员改学籍状态应成功");

            // 201 再查同一主键 → 客户端应看到新状态
            Message afterChange = query(client, handler, token, allocatedId);
            assertEquals(StatusCode.SUCCESS, afterChange.getStatusCode(),
                    "已登记的学籍应可查到");
            StudentProfile found = (StudentProfile) afterChange.getData();
            assertEquals("uuid-loop-1", found.getUserUuid(),
                    "查到的学籍应属于登记时的用户 uuid");
            assertEquals(EnrollmentStatus.GRADUATED, found.getStatus(),
                    "客户端应看到更新后的状态（服务端需对每条消息重新序列化）");
        } finally {
            client.close();
        }
    }

    /**
     * 心跳保活：服务端连接的闲置阈值是 15 秒（{@code ClientThread} 的 soTimeout），
     * 客户端每 3 秒发一次心跳；闲置超过该阈值后连接应仍然可用，证明两端的心跳
     * 约定（命令码均为 1）确实生效。
     *
     * <p>注意：心跳属网络层内部机制，客户端 {@code MessageReceiver} 会把它过滤掉、
     * 不回调到 UI 层，因此无法从 UI 回调观察 ACK，只能用「连接是否存活 + 业务是否
     * 仍可往返」间接验证；同时客户端读超时为 10 秒，也只有持续收到 ACK 才不会被判定超时。
     *
     * @throws Exception 通信失败
     */
    @Test
    void heartbeatKeepsIdleConnectionAlive() throws Exception {
        CollectingHandler handler = new CollectingHandler();
        ClientSocket client = new ClientSocket("127.0.0.1", s_port, handler,
                testConfig());
        try {
            client.connect();
            String token = login(client, handler, ADMIN_NAME, ADMIN_PASSWORD);
            assertNotNull(token, "登录应拿到 token");

            // 闲置超过服务端 15 秒空闲阈值
            Thread.sleep(SERVER_IDLE_TIMEOUT_MILLIS + 2000L);

            assertTrue(client.isConnected(),
                    "客户端心跳应让连接跨过服务端 15 秒空闲阈值而不断开");

            assertEquals(StatusCode.NOT_FOUND,
                    query(client, handler, token, 9999L).getStatusCode(),
                    "跨过空闲阈值后业务命令仍应正常往返");
        } finally {
            client.close();
        }
    }

    /**
     * 测试用网络参数：短超时、不重试、短宽限期，加快用例反馈。
     *
     * @return 网络参数
     */
    private static ClientNetworkConfig testConfig() {
        return new ClientNetworkConfig(3000, 10000, 0, 100L, 200L, 200L,
                3000L);
    }

    /**
     * 发送一条请求并等待与它同 uid 的响应。
     *
     * @param client  客户端连接
     * @param handler 响应收集器
     * @param token   会话令牌（登录阶段可为 null）
     * @param request 请求消息
     * @return 对应响应
     * @throws Exception 通信失败或超时
     */
    private static Message request(ClientSocket client,
            CollectingHandler handler, String token, Message request)
            throws Exception {
        final long uid = NEXT_UID.getAndIncrement();
        request.setUid(Long.valueOf(uid));
        request.setToken(token);
        client.send(request);

        Message response = handler.awaitUid(uid, RESPONSE_TIMEOUT_MILLIS);
        assertNotNull(response, "未在超时内收到命令 "
                + request.getCommand() + " 的响应");
        return response;
    }

    /**
     * 发送 201 查询学籍。
     *
     * @param client  客户端连接
     * @param handler 响应收集器
     * @param token   会话令牌
     * @param id      学籍主键
     * @return 响应
     * @throws Exception 通信失败或超时
     */
    private static Message query(ClientSocket client,
            CollectingHandler handler, String token, long id) throws Exception {
        return request(client, handler, token,
                new Message(Command.STUDENT_QUERY, Long.valueOf(id)));
    }

    /**
     * 按序探测学籍主键，定位属于指定用户 uuid 的记录。
     *
     * @param client   客户端连接
     * @param handler  响应收集器
     * @param token    会话令牌
     * @param userUuid 目标用户 uuid
     * @return 学籍主键；未找到返回 -1
     * @throws Exception 通信失败或超时
     */
    private static long findProfileId(ClientSocket client,
            CollectingHandler handler, String token, String userUuid)
            throws Exception {
        long candidate = 1L;
        while (candidate <= PROBE_MAX_ID) {
            Message response = query(client, handler, token, candidate);
            if (StatusCode.SUCCESS.equals(response.getStatusCode())) {
                StudentProfile found = (StudentProfile) response.getData();
                if (userUuid.equals(found.getUserUuid())) {
                    return candidate;
                }
            }
            candidate = candidate + 1L;
        }
        return -1L;
    }

    /**
     * 走完挑战-应答三步登录。
     *
     * @param client   客户端连接
     * @param handler  响应收集器
     * @param username 用户名
     * @param password 明文密码
     * @return 会话 token；任一步失败返回 null
     * @throws Exception 通信失败或超时
     */
    private static String login(ClientSocket client, CollectingHandler handler,
            String username, String password) throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.m_user_name = username;

        Message challengeResponse = request(client, handler, null,
                new Message(Command.USER_LOGIN, loginRequest));
        if (!StatusCode.SUCCESS.equals(challengeResponse.getStatusCode())) {
            return null;
        }
        LoginChallenge challenge = (LoginChallenge) challengeResponse.getData();

        String saltedHash = Sha256Util.sha256Hex(challenge.m_salt + password);
        String proof = Sha256Util.sha256Hex(challenge.m_nonce + saltedHash);

        LoginVerify verify = new LoginVerify();
        verify.m_user_name = username;
        verify.m_proof = proof;

        Message tokenResponse = request(client, handler, null,
                new Message(Command.USER_LOGIN_VERIFY, verify));
        if (!StatusCode.SUCCESS.equals(tokenResponse.getStatusCode())) {
            return null;
        }
        return ((LoginResponse) tokenResponse.getData()).m_token;
    }

    /**
     * 客户端网络事件收集器：把服务端返回的消息放入队列，供测试按 uid 等待。
     */
    private static final class CollectingHandler implements UIUpdateHandler {

        /** 收到的消息。 */
        private final BlockingQueue<Message> m_messages =
                new LinkedBlockingQueue<Message>();

        /**
         * {@inheritDoc}
         */
        @Override
        public void handleMessage(Message message) {
            m_messages.offer(message);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void connectionClosed(Exception cause) {
            // 集成测试不模拟重连，连接关闭由断言（isConnected）体现
        }

        /**
         * 等待指定 uid 的响应；无 uid 的消息（心跳确认等）直接跳过。
         *
         * @param uid           请求 uid
         * @param timeoutMillis 超时（毫秒）
         * @return 对应响应；超时返回 null
         * @throws InterruptedException 等待被中断
         */
        Message awaitUid(long uid, long timeoutMillis)
                throws InterruptedException {
            final long deadline = System.currentTimeMillis() + timeoutMillis;
            while (true) {
                final long remain = deadline - System.currentTimeMillis();
                if (remain <= 0) {
                    return null;
                }
                Message message = m_messages.poll(remain, TimeUnit.MILLISECONDS);
                if (message == null) {
                    return null;
                }
                Long id = message.getUid();
                if (id != null && id.longValue() == uid) {
                    return message;
                }
            }
        }
    }
}
