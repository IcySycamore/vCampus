package edu.seu.vcampus.server;

import edu.seu.vcampus.client.handler.UIUpdateHandler;
import edu.seu.vcampus.client.network.ClientNetworkConfig;
import edu.seu.vcampus.client.network.ClientSocket;
import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.user.dto.LoginChallenge;
import edu.seu.vcampus.common.user.dto.LoginRequest;
import edu.seu.vcampus.common.user.dto.LoginResponse;
import edu.seu.vcampus.common.user.dto.LoginVerify;
import edu.seu.vcampus.common.util.Sha256Util;
import org.junit.jupiter.api.Assertions;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 集成测试用的客户端会话：封装「连接 → 发请求 → 按 uid 等响应 → 登录」，
 * 内部使用客户端模块<b>真实</b>的网络实现（{@link ClientSocket} + {@link UIUpdateHandler}），
 * 不含任何测试替身，以便验证真实的双端行为。
 *
 * <p>每个请求在发送前分配独立 uid，并按该 uid 精确取回响应，避免多条命令并发时
 * 响应错配；服务端已保证响应回填请求 uid。
 */
final class IntegrationTestClient implements Closeable {

    /** 等待响应的超时（毫秒）。 */
    private static final long RESPONSE_TIMEOUT_MILLIS = 5000L;

    /** 测试用网络参数：短超时、不重试、短宽限期，加快用例反馈。 */
    private static final ClientNetworkConfig CONFIG =
            new ClientNetworkConfig(3000, 10000, 0, 100L, 200L, 200L, 3000L);

    /** 底层客户端连接。 */
    private final ClientSocket m_client;

    /** 响应收集器。 */
    private final CollectingHandler m_handler = new CollectingHandler();

    /** 请求 uid 发号器。 */
    private final AtomicLong m_nextUid = new AtomicLong(1L);

    /**
     * 创建会话（尚未连接）。
     *
     * @param port 服务器端口
     */
    IntegrationTestClient(int port) {
        m_client = new ClientSocket("127.0.0.1", port, m_handler, CONFIG);
    }

    /**
     * 连接服务器。
     *
     * @throws IOException 连接失败
     */
    void connect() throws IOException {
        m_client.connect();
    }

    /**
     * @return 当前是否保持连接
     */
    boolean isConnected() {
        return m_client.isConnected();
    }

    /**
     * 关闭会话。
     *
     * @throws IOException 关闭失败
     */
    @Override
    public void close() throws IOException {
        m_client.close();
    }

    /**
     * 发送一条请求并等待与它同 uid 的响应。
     *
     * @param token   会话令牌（登录阶段可为 null）
     * @param request 请求消息
     * @return 对应响应
     * @throws Exception 通信失败或超时
     */
    Message request(String token, Message request) throws Exception {
        final long uid = m_nextUid.getAndIncrement();
        request.setUid(Long.valueOf(uid));
        request.setToken(token);
        m_client.send(request);

        Message response = m_handler.awaitUid(uid, RESPONSE_TIMEOUT_MILLIS);
        Assertions.assertNotNull(response,
                "未在超时内收到命令 " + request.getCommand() + " 的响应");
        return response;
    }

    /**
     * 走完挑战-应答三步登录。
     *
     * @param username 用户名
     * @param password 明文密码
     * @return 会话 token；任一步失败返回 null
     * @throws Exception 通信失败或超时
     */
    String login(String username, String password) throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.m_user_name = username;

        Message challengeResponse = request(null,
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

        Message tokenResponse = request(null,
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
