package edu.seu.vcampus.server;

import edu.seu.vcampus.client.network.ClientMessageDispatcher;
import edu.seu.vcampus.client.network.ClientMessageSender;
import edu.seu.vcampus.client.network.ClientNetworkConfig;
import edu.seu.vcampus.client.network.ClientSocketListener;
import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.client.user.UserService;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.user.entity.SessionEntry;
import org.junit.jupiter.api.Assertions;

import java.io.Closeable;
import java.io.IOException;

/**
 * 集成测试用的客户端会话：直接复用客户端模块<b>真实</b>的连接层与分发器 （{@link ClientSocketListener} +
 * {@link ClientMessageDispatcher} + {@link UserService}）， 不另造发号与响应配对逻辑。
 *
 * <p>
 * 连接由本类持有（与生产入口 {@code VCampusClientApp} 同构）：收到的消息由连接直接交给分发器， 出站经
 * {@link ClientMessageSender} 走同一条连接。响应按命令码配对，服务端保证回显请求的命令码。
 */
final class IntegrationTestClient implements Closeable {

    /** 等待响应的超时（毫秒）。 */
    private static final long RESPONSE_TIMEOUT_MILLIS = 5000L;

    /** 测试用网络参数：短超时、不重试、短宽限期，加快用例反馈。 */
    private static final ClientNetworkConfig CONFIG = new ClientNetworkConfig(3000, 10000, 0, 100L,
            200L, 200L, 3000L);

    /** 底层客户端连接。 */
    private final ClientSocketListener m_socket;

    /** 消息分发器：随机序列号发号 + 按命令码配对响应。 */
    private final ClientMessageDispatcher m_dispatcher = new ClientMessageDispatcher();

    /** 用户管理客户端服务：登录走真实挑战-应答，不重复实现 proof 计算。 */
    private final UserService m_userService;

    /**
     * 创建会话（尚未连接）。
     *
     * @param port 服务器端口
     */
    IntegrationTestClient(int port) {
        m_socket = new ClientSocketListener("127.0.0.1", port, m_dispatcher, CONFIG);
        m_dispatcher.bindSender(new ClientMessageSender(m_socket));
        m_userService = new UserService(m_dispatcher, RESPONSE_TIMEOUT_MILLIS);
    }

    /**
     * 连接服务器。
     *
     * @throws IOException 连接失败
     */
    void connect() throws IOException {
        m_socket.connect();
    }

    /** @return 当前是否保持连接 */
    boolean isConnected() {
        return m_socket.isConnected();
    }

    @Override
    public void close() throws IOException {
        m_socket.close();
    }

    /**
     * 发送请求并等待同一命令码的响应。
     *
     * @param token 会话令牌（登录阶段可为 null）
     * @param request 请求消息
     * @return 对应响应
     * @throws Exception 通信失败或超时
     */
    Message request(String token, Message request) throws Exception {
        request.setToken(token);
        Message response = m_dispatcher.request(request, RESPONSE_TIMEOUT_MILLIS);
        Assertions.assertNotNull(response, "未在超时内收到命令 " + request.getCommand() + " 的响应");
        return response;
    }

    /**
     * 走完挑战-应答登录。
     *
     * @param username 用户名
     * @param password 明文密码
     * @return 会话 token；登录被拒绝或超时返回 null
     * @throws Exception 通信失败
     */
    String login(String username, String password) throws Exception {
        try {
            m_userService.login(username, null, password);
        } catch (ApiException e) {
            return null;
        }
        return m_userService.currentToken();
    }

    /** @return 登录会话中的角色；未登录返回 null */
    String role() {
        SessionEntry entry = m_userService.currentSession();
        return entry == null ? null : entry.getRole();
    }
}
