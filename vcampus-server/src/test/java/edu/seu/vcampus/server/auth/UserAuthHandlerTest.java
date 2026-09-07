package edu.seu.vcampus.server.auth;

import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.handler.MessageSender;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.user.dto.LoginChallenge;
import edu.seu.vcampus.common.user.dto.LoginRequest;
import edu.seu.vcampus.common.user.dto.LoginResponse;
import edu.seu.vcampus.common.user.dto.LoginVerify;
import edu.seu.vcampus.common.user.dto.RegisterRequest;
import edu.seu.vcampus.common.util.Sha256Util;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * UserAuthHandler 协议适配测试：验证 4 个用户命令的 Message 收发。
 */
class UserAuthHandlerTest {

    private AuthService auth;
    private SessionManager sessions;
    private AuthServiceHandler handler;

    /**
     * 每个用例前构造隔离的服务与处理器。
     */
    @BeforeEach
    void setUp() {
        sessions = new SessionManager();
        auth = new AuthService(new InMemoryUserRepository(),
                new NonceManager(), sessions);
        handler = new AuthServiceHandler(auth);
    }

    /**
     * 登录第①步：收到 LoginRequest 应回 LoginChallenge（含 salt/nonce）。
     */
    @Test
    void challengeLoginOk() {
        auth.register("001", "secret", "学生");
        LoginRequest req = new LoginRequest();
        req.m_user_name = "001";

        Message response = dispatch(Command.USER_LOGIN, req);

        assertEquals(StatusCode.SUCCESS, response.getStatusCode());
        LoginChallenge challenge = (LoginChallenge) response.getData();
        assertNotNull(challenge.m_salt);
        assertNotNull(challenge.m_nonce);
    }

    /**
     * 完整登录：正确 proof 通过后，token 放 Message.token、data 回真实角色。
     */
    @Test
    void loginVerifyOk() {
        auth.register("001", "secret", "学生");
        LoginChallenge ch = challengeOf("001");

        LoginVerify verify = new LoginVerify();
        verify.m_user_name = "001";
        verify.m_proof = clientProof(ch, "secret");

        Message response = dispatch(Command.USER_LOGIN_VERIFY, verify);

        assertEquals(StatusCode.SUCCESS, response.getStatusCode());
        assertNotNull(response.getToken());
        LoginResponse result = (LoginResponse) response.getData();
        assertEquals("学生", result.m_role);
    }

    /**
     * 密码错误：第③步回 401。
     */
    @Test
    void loginVerifyWrongPassword() {
        auth.register("001", "secret", "学生");
        LoginChallenge ch = challengeOf("001");

        LoginVerify verify = new LoginVerify();
        verify.m_user_name = "001";
        verify.m_proof = clientProof(ch, "wrong");

        Message response = dispatch(Command.USER_LOGIN_VERIFY, verify);

        assertEquals(StatusCode.UNAUTHORIZED, response.getStatusCode());
        assertNull(response.getToken());
    }

    /**
     * 注册成功回 200；重复注册回 400。
     */
    @Test
    void registerDuplicate() {
        RegisterRequest req = new RegisterRequest();
        req.m_user_name = "002";
        req.m_role = "教师";
        req.m_password = "secret";

        assertEquals(StatusCode.SUCCESS,
                dispatch(Command.USER_REGISTER, req).getStatusCode());
        assertEquals(StatusCode.BAD_REQUEST,
                dispatch(Command.USER_REGISTER, req).getStatusCode());
    }

    /**
     * 登出：携带 token 回 200，之后该 token 失效。
     */
    @Test
    void logoutInvalidatesToken() {
        auth.register("001", "secret", "学生");
        LoginChallenge ch = challengeOf("001");
        LoginVerify verify = new LoginVerify();
        verify.m_user_name = "001";
        verify.m_proof = clientProof(ch, "secret");
        Message loginResponse = dispatch(Command.USER_LOGIN_VERIFY, verify);
        String token = loginResponse.getToken();

        Message logoutRequest = new Message(Command.USER_LOGOUT, null);
        logoutRequest.setToken(token);
        Message response = dispatch(logoutRequest);

        assertEquals(StatusCode.SUCCESS, response.getStatusCode());
        assertNull(sessions.validate(token));
    }

    /**
     * data 类型不符时回 400。
     */
    @Test
    void badDataReturns400() {
        Message response = dispatch(Command.USER_LOGIN, "not-a-dto");
        assertEquals(StatusCode.BAD_REQUEST, response.getStatusCode());
    }

    /**
     * 先走第①②步拿挑战。
     *
     * @param username 用户名
     * @return 挑战
     */
    private LoginChallenge challengeOf(String username) {
        LoginRequest req = new LoginRequest();
        req.m_user_name = username;
        return (LoginChallenge) dispatch(Command.USER_LOGIN, req).getData();
    }

    /**
     * 按客户端视角计算 proof。
     *
     * @param challenge 挑战
     * @param password  明文密码
     * @return proof
     */
    private String clientProof(LoginChallenge challenge, String password) {
        String inner = Sha256Util.sha256Hex(challenge.m_salt + password);
        return Sha256Util.sha256Hex(challenge.m_nonce + inner);
    }

    /**
     * 派发一条请求并捕获响应。
     *
     * @param command 命令码
     * @param data    载荷
     * @return 响应消息
     */
    private Message dispatch(int command, Object data) {
        return dispatch(new Message(command, data));
    }

    /**
     * 派发一条请求并捕获响应。
     *
     * @param request 请求消息
     * @return 响应消息
     */
    private Message dispatch(final Message request) {
        final Message[] sent = new Message[1];
        MessageSender sender = new MessageSender() {
            @Override
            public void send(Message response) {
                sent[0] = response;
            }
        };
        handler.handle(request, sender);
        return sent[0];
    }
}