package edu.seu.vcampus.client.user;

import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.client.network.ClientMessageDispatcher;
import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.message.PageResponse;
import edu.seu.vcampus.common.user.dto.ChangePasswordRequest;
import edu.seu.vcampus.common.user.dto.LoginChallenge;
import edu.seu.vcampus.common.user.dto.LoginResponse;
import edu.seu.vcampus.common.user.dto.LoginVerify;
import edu.seu.vcampus.common.user.dto.UserEnabledRequest;
import edu.seu.vcampus.common.user.dto.UserQuery;
import edu.seu.vcampus.common.user.dto.UserRefRequest;
import edu.seu.vcampus.common.user.entity.Role;
import edu.seu.vcampus.common.user.entity.SessionEntry;
import edu.seu.vcampus.common.user.entity.User;
import edu.seu.vcampus.common.util.Sha256Util;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * UserService 测试：挑战-应答登录、改密、管理轨命令与载荷、会话生命周期。
 *
 * <p>
 * 按 ADR-0009 D10：模块 API 的主战场是同包假分发器单测——断言「方法 → 命令码 + 载荷」与 「状态码 → 异常」，不需要真实连接。
 */
class UserServiceTest {

    /** 请求超时。 */
    private static final long TIMEOUT = 1000L;

    /** 登录成功：token 只进入内存会话，proof 与客户端公式一致。 */
    @Test
    void loginSuccess() {
        FakeDispatcher dispatcher = new FakeDispatcher();
        LoginChallenge challenge = new LoginChallenge();
        challenge.m_salt = "salt-1";
        challenge.m_nonce = "nonce-1";
        dispatcher.reply(Command.USER_LOGIN, response(StatusCode.SUCCESS, challenge));
        LoginResponse loginResponse = new LoginResponse();
        loginResponse.m_token = "token-xyz";
        loginResponse.m_session = new SessionEntry("uuid-1", "001", "学生", 0L);
        dispatcher.reply(Command.USER_LOGIN_VERIFY, response(StatusCode.SUCCESS, loginResponse));

        UserService service = new UserService(dispatcher, TIMEOUT);
        service.login("001", Role.STUDENT, "pw");

        assertTrue(service.isLoggedIn());
        assertEquals("token-xyz", service.currentToken());
        assertNotNull(service.currentSession());
        assertEquals("学生", service.currentSession().getRole());

        LoginVerify verify = (LoginVerify) dispatcher.m_sent.get(1).getData();
        String expected = Sha256Util.sha256Hex("nonce-1" + Sha256Util.sha256Hex("salt-1" + "pw"));
        assertEquals(expected, verify.m_proof);
        assertEquals("001", verify.m_user_name);
    }

    /** 服务器拒绝登录时抛出带状态码的 ApiException。 */
    @Test
    void loginRejected() {
        FakeDispatcher dispatcher = new FakeDispatcher();
        LoginChallenge challenge = new LoginChallenge();
        challenge.m_salt = "s";
        challenge.m_nonce = "n";
        dispatcher.reply(Command.USER_LOGIN, response(StatusCode.SUCCESS, challenge));
        dispatcher.reply(Command.USER_LOGIN_VERIFY, response(StatusCode.UNAUTHORIZED, null));

        UserService service = new UserService(dispatcher, TIMEOUT);
        try {
            service.login("001", Role.STUDENT, "bad");
            fail("expected ApiException");
        } catch (ApiException e) {
            assertEquals(StatusCode.UNAUTHORIZED, e.getStatusCode());
            assertFalse(e.isLocal());
        }
    }

    /** 响应载荷类型不符时抛本地格式异常码。 */
    @Test
    void malformedChallengePayload() {
        FakeDispatcher dispatcher = new FakeDispatcher();
        dispatcher.reply(Command.USER_LOGIN, response(StatusCode.SUCCESS, "not-a-challenge"));

        UserService service = new UserService(dispatcher, TIMEOUT);
        try {
            service.login("001", Role.STUDENT, "pw");
            fail("expected ApiException");
        } catch (ApiException e) {
            assertTrue(e.isLocal());
        }
    }

    /** 注册请求需携带当前（管理员）令牌，且角色以显示名上线。 */
    @Test
    void registerCarriesAdminToken() {
        FakeDispatcher dispatcher = new FakeDispatcher();
        dispatcher.reply(Command.USER_REGISTER, response(StatusCode.SUCCESS, null));
        UserService service = new UserService(dispatcher, TIMEOUT);
        service.session().cache("admin-token", new SessionEntry("uuid-admin", "admin", "管理员", 0L));

        service.register("002", Role.TEACHER, "pw");

        Message sent = dispatcher.m_sent.get(0);
        assertEquals(Command.USER_REGISTER, sent.getCommand());
        assertEquals("admin-token", sent.getToken());
    }

    /** 注销发送用户引用载荷。 */
    @Test
    void unregisterSendsUserRef() {
        FakeDispatcher dispatcher = new FakeDispatcher();
        dispatcher.reply(Command.USER_UNREGISTER, response(StatusCode.SUCCESS, null));
        UserService service = new UserService(dispatcher, TIMEOUT);
        service.session().cache("token", new SessionEntry("uuid-1", "admin", "管理员", 0L));

        service.unregister("002");

        Message sent = dispatcher.m_sent.get(0);
        assertEquals(Command.USER_UNREGISTER, sent.getCommand());
        assertEquals("002", ((UserRefRequest) sent.getData()).getUserName());
    }

    /** 启用/禁用发送状态载荷。 */
    @Test
    void toggleEnabledSendsState() {
        FakeDispatcher dispatcher = new FakeDispatcher();
        dispatcher.reply(Command.USER_TOGGLE_ENABLED, response(StatusCode.SUCCESS, null));
        UserService service = new UserService(dispatcher, TIMEOUT);
        service.session().cache("token", new SessionEntry("uuid-1", "admin", "管理员", 0L));

        service.toggleUserEnabled("002", false);

        UserEnabledRequest payload = (UserEnabledRequest) dispatcher.m_sent.get(0).getData();
        assertEquals("002", payload.getUserName());
        assertFalse(payload.isEnabled());
    }

    /** 分页查询返回同一批用户，载荷为查询条件。 */
    @Test
    void listUsersReturnsPage() {
        FakeDispatcher dispatcher = new FakeDispatcher();
        User user = new User("002", "李四", null, Role.TEACHER);
        user.setUuid("uuid-2");
        PageResponse<User> page = new PageResponse<User>(Arrays.asList(user), 1L, 1, 20);
        dispatcher.reply(Command.USER_LIST, response(StatusCode.SUCCESS, page));
        UserService service = new UserService(dispatcher, TIMEOUT);
        service.session().cache("token", new SessionEntry("uuid-1", "admin", "管理员", 0L));

        PageResponse<User> result = service
                .listUsers(new UserQuery("李", Role.TEACHER, null, 1, 20));

        assertEquals(1, result.getItems().size());
        assertEquals("李四", result.getItems().get(0).getDisplayName());
        assertEquals(1L, result.getTotal());
        assertEquals(Command.USER_LIST, dispatcher.m_sent.get(0).getCommand());
    }

    /** 非分页载荷视为协议异常。 */
    @Test
    void listUsersRejectsMalformedPayload() {
        FakeDispatcher dispatcher = new FakeDispatcher();
        dispatcher.reply(Command.USER_LIST, response(StatusCode.SUCCESS, "oops"));
        final UserService service = new UserService(dispatcher, TIMEOUT);
        service.session().cache("token", new SessionEntry("uuid-1", "admin", "管理员", 0L));

        assertThrows(ApiException.class, new Executable() {
            @Override
            public void execute() {
                service.listUsers(null);
            }
        });
    }

    /** 改密：先用挑战校验旧密码（proof 不入明文），再提交新盐与新哈希。 */
    @Test
    void changePasswordUsesProofAndNewSalt() {
        FakeDispatcher dispatcher = new FakeDispatcher();
        LoginChallenge challenge = new LoginChallenge();
        challenge.m_salt = "salt-9";
        challenge.m_nonce = "nonce-9";
        dispatcher.reply(Command.USER_LOGIN, response(StatusCode.SUCCESS, challenge));
        dispatcher.reply(Command.USER_CHANGE_PASSWORD, response(StatusCode.SUCCESS, null));
        UserService service = new UserService(dispatcher, TIMEOUT);
        service.session().cache("token-xyz", new SessionEntry("uuid-1", "001", "学生", 0L));

        service.changePassword("old-pw", "new-pw");

        ChangePasswordRequest payload = (ChangePasswordRequest) dispatcher.m_sent.get(1).getData();
        assertNull(payload.getUserName());// 我的轨：服务端按会话定身份
        assertEquals(Sha256Util.sha256Hex("nonce-9" + Sha256Util.sha256Hex("salt-9" + "old-pw")),
                payload.getProof());
        assertNotNull(payload.getNewSalt());
        assertEquals(Sha256Util.sha256Hex(payload.getNewSalt() + "new-pw"), payload.getNewHash());
        assertEquals("token-xyz", dispatcher.m_sent.get(1).getToken());
    }

    /** 未登录时改密直接抛 401，不发请求。 */
    @Test
    void changePasswordRequiresSession() {
        FakeDispatcher dispatcher = new FakeDispatcher();
        final UserService service = new UserService(dispatcher, TIMEOUT);
        try {
            service.changePassword("old", "new");
            fail("expected ApiException");
        } catch (ApiException e) {
            assertEquals(StatusCode.UNAUTHORIZED, e.getStatusCode());
        }
        assertTrue(dispatcher.m_sent.isEmpty());
    }

    /** 登出后内存会话被清空，不再持有令牌。 */
    @Test
    void logoutClearsSession() {
        FakeDispatcher dispatcher = new FakeDispatcher();
        dispatcher.reply(Command.USER_LOGOUT, response(StatusCode.SUCCESS, null));
        UserService service = new UserService(dispatcher, TIMEOUT);
        service.session().cache("token-xyz", new SessionEntry("uuid-1", "001", "学生", 0L));

        service.logout();

        assertFalse(service.isLoggedIn());
        assertNull(service.currentToken());
        assertNull(service.currentSession());
        assertEquals("token-xyz", dispatcher.m_sent.get(0).getToken());
    }

    /** 连接断开即丢弃内存会话（token 不跨连接存活，不落盘）。 */
    @Test
    void connectionClosedClearsSession() {
        UserService service = new UserService(new FakeDispatcher(), TIMEOUT);
        service.session().cache("token-xyz", new SessionEntry("uuid-1", "001", "学生", 0L));

        service.connectionClosed(new IOException("peer reset"));

        assertFalse(service.isLoggedIn());
        assertNull(service.currentSession());
    }

    /** 无响应（超时）归一为本地超时码。 */
    @Test
    void timeoutBecomesLocalCode() {
        FakeDispatcher dispatcher = new FakeDispatcher();
        UserService service = new UserService(dispatcher, TIMEOUT);
        try {
            service.logout();
            fail("expected ApiException");
        } catch (ApiException e) {
            assertTrue(e.isLocal());
            assertEquals("服务器无响应（超时）", e.getMessage());
        }
    }

    /** 分发器为 null 时构造失败。 */
    @Test
    void rejectsNullDispatcher() {
        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() {
                new UserService(null, TIMEOUT);
            }
        });
    }

    private Message response(String status, Object data) {
        Message message = new Message(0, data);
        message.setStatusCode(status);
        return message;
    }

    /**
     * 假分发器：按命令码返回预置响应，并记录发送的消息（不建真实连接）。
     */
    private static final class FakeDispatcher extends ClientMessageDispatcher {

        /** 已发送消息。 */
        private final List<Message> m_sent = new ArrayList<Message>();

        /** 预置响应。 */
        private final Map<Integer, Message> m_replies = new HashMap<Integer, Message>();

        void reply(int command, Message response) {
            m_replies.put(Integer.valueOf(command), response);
        }

        @Override
        public void send(Message message) {
            m_sent.add(message);
        }

        @Override
        public Message request(Message request, long timeoutMillis) {
            m_sent.add(request);
            return m_replies.get(Integer.valueOf(request.getCommand()));
        }
    }
}
