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
 * AuthServiceHandler 协议适配测试：验证 4 个用户命令的收发、token 分发与鉴权。
 */
class AuthServiceHandlerTest {

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

    @Test
    void loginVerifyOk() {
        auth.register("001", "secret", "学生");
        LoginChallenge ch = challengeOf("001");

        LoginVerify verify = new LoginVerify();
        verify.m_user_name = "001";
        verify.m_proof = clientProof(ch, "secret");

        Message response = dispatch(Command.USER_LOGIN_VERIFY, verify);

        assertEquals(StatusCode.SUCCESS, response.getStatusCode());
        LoginResponse result = (LoginResponse) response.getData();
        assertNotNull(result.m_token);// token 经 LoginResponse 一次性分发
        assertEquals("学生", result.m_role);
    }

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

    @Test
    void adminRegisterOkAndDuplicate() {
        String adminToken = login("admin", "root", "管理员");
        RegisterRequest req = new RegisterRequest();
        req.m_user_name = "002";
        req.m_role = "教师";
        req.m_password = "secret";

        assertEquals(StatusCode.SUCCESS,
                dispatchWithToken(Command.USER_REGISTER, req, adminToken)
                        .getStatusCode());
        assertEquals(StatusCode.BAD_REQUEST,
                dispatchWithToken(Command.USER_REGISTER, req, adminToken)
                        .getStatusCode());
    }

    @Test
    void studentRegisterForbidden() {
        String token = login("stu001", "pw", "学生");
        RegisterRequest req = new RegisterRequest();
        req.m_user_name = "002";
        req.m_role = "教师";
        req.m_password = "secret";

        assertEquals(StatusCode.FORBIDDEN,
                dispatchWithToken(Command.USER_REGISTER, req, token)
                        .getStatusCode());
    }

    @Test
    void registerWithoutTokenUnauthorized() {
        RegisterRequest req = new RegisterRequest();
        req.m_user_name = "002";
        req.m_role = "教师";
        req.m_password = "secret";

        assertEquals(StatusCode.UNAUTHORIZED,
                dispatch(Command.USER_REGISTER, req).getStatusCode());
    }

    @Test
    void logoutInvalidatesToken() {
        String token = login("001", "secret", "学生");

        Message logoutRequest = new Message(Command.USER_LOGOUT, null);
        logoutRequest.setToken(token);
        Message response = dispatch(logoutRequest);

        assertEquals(StatusCode.SUCCESS, response.getStatusCode());
        assertNull(sessions.validate(token));
    }

    @Test
    void logoutWithoutTokenUnauthorized() {
        Message logoutRequest = new Message(Command.USER_LOGOUT, null);
        assertEquals(StatusCode.UNAUTHORIZED,
                dispatch(logoutRequest).getStatusCode());
    }

    @Test
    void badDataReturns400() {
        Message response = dispatch(Command.USER_LOGIN, "not-a-dto");
        assertEquals(StatusCode.BAD_REQUEST, response.getStatusCode());
    }

    private LoginChallenge challengeOf(String username) {
        LoginRequest req = new LoginRequest();
        req.m_user_name = username;
        return (LoginChallenge) dispatch(Command.USER_LOGIN, req).getData();
    }

    private String login(String username, String password, String role) {
        auth.register(username, password, role);
        LoginChallenge ch = auth.loginChallenge(username);
        return auth.loginVerify(username, clientProof(ch, password));
    }

    private Message dispatchWithToken(int command, Object data,
            String token) {
        Message request = new Message(command, data);
        request.setToken(token);
        return dispatch(request);
    }

    private String clientProof(LoginChallenge challenge, String password) {
        String inner = Sha256Util.sha256Hex(challenge.m_salt + password);
        return Sha256Util.sha256Hex(challenge.m_nonce + inner);
    }

    private Message dispatch(int command, Object data) {
        return dispatch(new Message(command, data));
    }

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