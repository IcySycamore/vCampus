package edu.seu.vcampus.server.user;

import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.message.MessageSender;
import edu.seu.vcampus.common.user.dto.LoginChallenge;
import edu.seu.vcampus.common.user.dto.LoginVerify;
import edu.seu.vcampus.common.user.dto.UserProfile;
import edu.seu.vcampus.common.util.Sha256Util;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 「查我的档案」（命令 109）测试：登录后界面靠它拿姓名。
 *
 * <p>
 * 单独建一个文件而不是往 {@code AuthServiceHandlerTest} 里塞，是因为后者已接近文件行数上限；
 * 同类型的用例（身份按会话解析、越权回 401）也应当放得下、看得清。
 */
class UserProfileQueryHandlerTest {

    /** 认证服务。 */
    private AuthService auth;

    /** 会话池。 */
    private SessionManager sessions;

    /** 被测处理器。 */
    private AuthServiceHandler handler;

    /**
     * 每个用例前构造隔离的服务与处理器。
     */
    @BeforeEach
    void setUp() {
        sessions = new SessionManager();
        auth = new AuthService(new InMemoryUserRepository(), new NonceManager(), sessions);
        handler = new AuthServiceHandler(auth);
    }

    /**
     * 师生登录后能查到自己的姓名与角色。
     */
    @Test
    void studentCanQueryOwnProfile() {
        String token = login("001", "secret", "学生", "张三");

        Message response = send(Command.USER_PROFILE_QUERY, null, token);

        assertEquals(StatusCode.SUCCESS, response.getStatusCode());
        UserProfile profile = (UserProfile) response.getData();
        assertNotNull(profile);
        assertEquals("001", profile.getUserName());
        assertEquals("张三", profile.getRealName());
        assertEquals("学生", profile.getRole());
    }

    /**
     * 管理员没有姓名：档案里姓名为空，显示名回退登录名（不是 null）。
     */
    @Test
    void adminProfileHasNoRealName() {
        String token = login("003", "secret", "管理员", null);

        Message response = send(Command.USER_PROFILE_QUERY, null, token);

        assertEquals(StatusCode.SUCCESS, response.getStatusCode());
        UserProfile profile = (UserProfile) response.getData();
        assertNull(profile.getRealName());
        assertEquals("003", profile.getDisplayName());
    }

    /**
     * 未携带 token 应回 401。
     */
    @Test
    void missingTokenReturnsUnauthorized() {
        Message response = send(Command.USER_PROFILE_QUERY, null, null);

        assertEquals(StatusCode.UNAUTHORIZED, response.getStatusCode());
    }

    /**
     * 伪造 token 同样回 401。
     */
    @Test
    void forgedTokenReturnsUnauthorized() {
        Message response = send(Command.USER_PROFILE_QUERY, null, "not-a-real-token");

        assertEquals(StatusCode.UNAUTHORIZED, response.getStatusCode());
    }

    /**
     * 请求体传什么都不影响结果：身份完全按会话解析，客户端伪造不了。
     */
    @Test
    void payloadIsIgnored() {
        String token = login("002", "secret", "教师", "李四");

        Message response = send(Command.USER_PROFILE_QUERY, "别人的-uuid", token);

        assertEquals(StatusCode.SUCCESS, response.getStatusCode());
        UserProfile profile = (UserProfile) response.getData();
        assertEquals("002", profile.getUserName());
        assertEquals("李四", profile.getRealName());
    }

    /**
     * 注册并完成挑战-应答登录，返回 token。
     *
     * @param userName 登录名
     * @param password 明文密码
     * @param role 角色
     * @param realName 真实姓名（可为 null）
     * @return 会话令牌
     */
    private String login(String userName, String password, String role, String realName) {
        auth.register(userName, password, role, realName);
        LoginChallenge challenge = auth.loginChallenge(userName);
        LoginVerify verify = new LoginVerify();
        verify.m_user_name = userName;
        verify.m_proof = Sha256Util.sha256Hex(challenge.m_nonce
                + Sha256Util.sha256Hex(challenge.m_salt + password));
        return auth.loginVerify(userName, verify.m_proof);
    }

    /**
     * 发送一条请求并捕获响应。
     *
     * @param command 命令码
     * @param data 请求数据
     * @param token 会话令牌（可为 null）
     * @return 处理器写出的响应
     */
    private Message send(int command, Object data, String token) {
        Message request = new Message(command, data);
        request.setToken(token);
        final Message[] captured = new Message[1];
        MessageSender sender = new MessageSender() {
            @Override
            public void send(Message response) {
                captured[0] = response;
            }
        };
        handler.handle(request, sender);
        return captured[0];
    }
}
