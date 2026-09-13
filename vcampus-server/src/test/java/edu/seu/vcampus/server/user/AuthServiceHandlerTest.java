package edu.seu.vcampus.server.user;

import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.message.MessageSender;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.message.PageResponse;
import edu.seu.vcampus.common.user.dto.BatchResult;
import edu.seu.vcampus.common.user.dto.ChangePasswordRequest;
import edu.seu.vcampus.common.user.dto.LoginChallenge;
import edu.seu.vcampus.common.user.dto.LoginRequest;
import edu.seu.vcampus.common.user.dto.LoginResponse;
import edu.seu.vcampus.common.user.dto.LoginVerify;
import edu.seu.vcampus.common.user.dto.RegisterRequest;
import edu.seu.vcampus.common.user.dto.UserEnabledRequest;
import edu.seu.vcampus.common.user.dto.UserQuery;
import edu.seu.vcampus.common.user.dto.UserRefRequest;
import edu.seu.vcampus.common.user.dto.UserUpdateRequest;
import edu.seu.vcampus.common.util.Sha256Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    private InMemoryUserRepository repository;
    private AuthServiceHandler handler;

    /**
     * 每个用例前构造隔离的服务与处理器。
     */
    @BeforeEach
    void setUp() {
        sessions = new SessionManager();
        repository = new InMemoryUserRepository();
        auth = new AuthService(repository, new NonceManager(), sessions);
        handler = new AuthServiceHandler(auth,
                new UserAdminService(repository, new AccountProvisioning()));
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
        assertEquals("学生", result.m_session.getRole());
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
                dispatchWithToken(Command.USER_REGISTER, req, adminToken).getStatusCode());
        assertEquals(StatusCode.BAD_REQUEST,
                dispatchWithToken(Command.USER_REGISTER, req, adminToken).getStatusCode());
    }

    @Test
    void studentRegisterForbidden() {
        String token = login("stu001", "pw", "学生");
        RegisterRequest req = new RegisterRequest();
        req.m_user_name = "002";
        req.m_role = "教师";
        req.m_password = "secret";

        assertEquals(StatusCode.FORBIDDEN,
                dispatchWithToken(Command.USER_REGISTER, req, token).getStatusCode());
    }

    @Test
    void registerWithoutTokenUnauthorized() {
        RegisterRequest req = new RegisterRequest();
        req.m_user_name = "002";
        req.m_role = "教师";
        req.m_password = "secret";

        assertEquals(StatusCode.UNAUTHORIZED, dispatch(Command.USER_REGISTER, req).getStatusCode());
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
        assertEquals(StatusCode.UNAUTHORIZED, dispatch(logoutRequest).getStatusCode());
    }

    @Test
    void badDataReturns400() {
        Message response = dispatch(Command.USER_LOGIN, "not-a-dto");
        assertEquals(StatusCode.BAD_REQUEST, response.getStatusCode());
    }

    /** 106 需 USER_MANAGE：学生调用被拒 403。 */
    @Test
    void listUsersForbiddenForStudent() {
        String token = login("stu001", "pw", "学生");
        assertEquals(StatusCode.FORBIDDEN,
                dispatchWithToken(Command.USER_LIST, new UserQuery(), token).getStatusCode());
    }

    /** 106 管理员分页查询：返回含自身的分页结果。 */
    @Test
    void listUsersForAdminReturnsPage() {
        String token = login("admin", "root", "管理员");
        auth.register("002", "secret", "教师");

        Message response = dispatchWithToken(Command.USER_LIST, new UserQuery(), token);

        assertEquals(StatusCode.SUCCESS, response.getStatusCode());
        PageResponse<?> page = (PageResponse<?>) response.getData();
        assertEquals(2L, page.getTotal());
    }

    /** 107 编辑姓名，不改角色。 */
    @Test
    void updateUserChangesDisplayName() {
        String token = login("admin", "root", "管理员");
        assertEquals(StatusCode.SUCCESS, dispatchWithToken(Command.USER_UPDATE,
                new UserUpdateRequest("admin", "系统管理员"), token).getStatusCode());
        assertEquals("系统管理员", repository.findByUsername("admin").getDisplayName());
        assertEquals("管理员", repository.findByUsername("admin").getRole());
    }

    /** 108 禁用后该账号登录回 P102。 */
    @Test
    void toggleEnabledBlocksLogin() {
        String token = login("admin", "root", "管理员");
        auth.register("002", "secret", "教师");

        assertEquals(StatusCode.SUCCESS, dispatchWithToken(Command.USER_TOGGLE_ENABLED,
                new UserEnabledRequest("002", false), token).getStatusCode());

        LoginChallenge challenge = challengeOf("002");
        LoginVerify verify = new LoginVerify();
        verify.m_user_name = "002";
        verify.m_proof = clientProof(challenge, "secret");
        assertEquals(StatusCode.USER_DISABLED,
                dispatch(Command.USER_LOGIN_VERIFY, verify).getStatusCode());
    }

    /** 104 注销后账号不存在，再注销回 404。 */
    @Test
    void unregisterRemovesAccount() {
        String token = login("admin", "root", "管理员");
        auth.register("002", "secret", "教师");

        assertEquals(StatusCode.SUCCESS,
                dispatchWithToken(Command.USER_UNREGISTER, new UserRefRequest("002"), token)
                        .getStatusCode());
        assertNull(repository.findByUsername("002"));
        assertEquals(StatusCode.NOT_FOUND,
                dispatchWithToken(Command.USER_UNREGISTER, new UserRefRequest("002"), token)
                        .getStatusCode());
    }

    /** 109 本人改密：proof 校验旧密码，改完新密码可登录。 */
    @Test
    void changeOwnPasswordUsesProof() {
        String token = login("001", "secret", "学生");
        LoginChallenge challenge = challengeOf("001");
        String newSalt = "0123456789abcdef";
        ChangePasswordRequest request = new ChangePasswordRequest(null,
                clientProof(challenge, "secret"), newSalt,
                Sha256Util.sha256Hex(newSalt + "new-secret"));

        assertEquals(StatusCode.SUCCESS,
                dispatchWithToken(Command.USER_CHANGE_PASSWORD, request, token).getStatusCode());

        LoginChallenge again = challengeOf("001");
        LoginVerify verify = new LoginVerify();
        verify.m_user_name = "001";
        verify.m_proof = clientProof(again, "new-secret");
        assertEquals(StatusCode.SUCCESS,
                dispatch(Command.USER_LOGIN_VERIFY, verify).getStatusCode());
    }

    /** 109 旧密码错误回 P100。 */
    @Test
    void changeOwnPasswordWrongOldPassword() {
        String token = login("001", "secret", "学生");
        LoginChallenge challenge = challengeOf("001");
        ChangePasswordRequest request = new ChangePasswordRequest(null,
                clientProof(challenge, "wrong"), "salt-x", "hash-x");
        assertEquals(StatusCode.WRONG_PASSWORD,
                dispatchWithToken(Command.USER_CHANGE_PASSWORD, request, token).getStatusCode());
    }

    /** 109 非本人且无 USER_MANAGE → 403（管理轨要能力，我的轨不要）。 */
    @Test
    void resetOtherPasswordRequiresUserManage() {
        String token = login("stu001", "pw", "学生");
        auth.register("002", "secret", "学生");
        ChangePasswordRequest request = new ChangePasswordRequest("002", null, "salt-x", "hash-x");
        assertEquals(StatusCode.FORBIDDEN,
                dispatchWithToken(Command.USER_CHANGE_PASSWORD, request, token).getStatusCode());
    }

    /** 103 批量注册（仅管理员）返回逐条结果。 */
    @Test
    void batchRegisterReturnsPerItemResult() {
        String token = login("admin", "root", "管理员");
        List<RegisterRequest> requests = new ArrayList<RegisterRequest>();
        requests.add(registerRequest("100", "学生"));
        requests.add(registerRequest("101", "教师"));
        requests.add(registerRequest("100", "学生"));

        Message response = dispatchWithToken(Command.USER_BATCH_REGISTER, requests, token);

        assertEquals(StatusCode.SUCCESS, response.getStatusCode());
        BatchResult result = (BatchResult) response.getData();
        assertEquals(2, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
    }

    /** 105 批量注销（仅管理员）返回逐条结果。 */
    @Test
    void batchUnregisterReturnsPerItemResult() {
        String token = login("admin", "root", "管理员");
        auth.register("200", "pw", "学生");

        Message response = dispatchWithToken(Command.USER_BATCH_UNREGISTER,
                Arrays.asList("200", "ghost"), token);

        assertEquals(StatusCode.SUCCESS, response.getStatusCode());
        BatchResult result = (BatchResult) response.getData();
        assertEquals(1, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertNull(repository.findByUsername("200"));
    }

    /** 103 学生对管理命令无权。 */
    @Test
    void batchRegisterForbiddenForStudent() {
        String token = login("stu001", "pw", "学生");
        assertEquals(StatusCode.FORBIDDEN, dispatchWithToken(Command.USER_BATCH_REGISTER,
                new ArrayList<RegisterRequest>(), token).getStatusCode());
    }

    private RegisterRequest registerRequest(String userName, String role) {
        RegisterRequest request = new RegisterRequest();
        request.m_user_name = userName;
        request.m_role = role;
        request.m_password = "pw";
        return request;
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

    private Message dispatchWithToken(int command, Object data, String token) {
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