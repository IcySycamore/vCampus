package edu.seu.vcampus.client.user;

import edu.seu.vcampus.client.network.ClientMessageDispatcher;
import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.user.dto.LoginChallenge;
import edu.seu.vcampus.common.user.dto.LoginResponse;
import edu.seu.vcampus.common.user.dto.LoginVerify;
import edu.seu.vcampus.common.user.entity.SessionEntry;
import edu.seu.vcampus.common.user.dto.UserProfile;
import edu.seu.vcampus.common.util.Sha256Util;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * UserService 测试：挑战-应答登录、proof 计算、注册携带管理员令牌。
 */
class UserServiceTest {

    /** 请求超时。 */
    private static final long TIMEOUT = 1000L;

    /** 登录成功：token 只进入内存会话，proof 与客户端公式一致。 */
    @Test
    void loginSuccess() throws Exception {
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

        service.login("001", "学生", "pw");

        assertTrue(service.isLoggedIn());
        assertEquals("token-xyz", service.getSession().getToken());
        assertEquals("学生", service.getSession().getEntry().getRole());

        LoginVerify verify = (LoginVerify) dispatcher.m_sent.get(1).getData();
        String expected = Sha256Util.sha256Hex("nonce-1" + Sha256Util.sha256Hex("salt-1" + "pw"));
        assertEquals(expected, verify.m_proof);
        assertEquals("001", verify.m_user_name);
    }

    /** 服务器拒绝登录时抛出带状态码的 AuthException。 */
    @Test
    void loginRejected() throws Exception {
        FakeDispatcher dispatcher = new FakeDispatcher();
        LoginChallenge challenge = new LoginChallenge();
        challenge.m_salt = "s";
        challenge.m_nonce = "n";
        dispatcher.reply(Command.USER_LOGIN, response(StatusCode.SUCCESS, challenge));
        dispatcher.reply(Command.USER_LOGIN_VERIFY, response(StatusCode.UNAUTHORIZED, null));

        UserService service = new UserService(dispatcher, TIMEOUT);
        try {
            service.login("001", "学生", "bad");
            fail("expected AuthException");
        } catch (AuthException e) {
            assertEquals(StatusCode.UNAUTHORIZED, e.getStatusCode());
        }
    }

    /** 注册请求需携带当前（管理员）令牌。 */
    @Test
    void registerCarriesAdminToken() throws Exception {
        FakeDispatcher dispatcher = new FakeDispatcher();
        dispatcher.reply(Command.USER_REGISTER, response(StatusCode.SUCCESS, null));
        UserService service = new UserService(dispatcher, TIMEOUT);
        service.getSession().cache("admin-token",
                new SessionEntry("uuid-admin", "admin", "管理员", 0L));

        service.register("002", "教师", "pw");

        assertEquals(Command.USER_REGISTER, dispatcher.m_sent.get(0).getCommand());
        assertEquals("admin-token", dispatcher.m_sent.get(0).getToken());
    }

    /** 登出后内存会话被清空，不再持有令牌。 */
    @Test
    void logoutClearsSession() throws Exception {
        FakeDispatcher dispatcher = new FakeDispatcher();
        dispatcher.reply(Command.USER_LOGOUT, response(StatusCode.SUCCESS, null));
        UserService service = new UserService(dispatcher, TIMEOUT);
        service.getSession().cache("token-xyz", new SessionEntry("uuid-1", "001", "学生", 0L));

        service.logout();

        assertFalse(service.isLoggedIn());
        assertNull(service.getSession().getToken());
        assertEquals("token-xyz", dispatcher.m_sent.get(0).getToken());
    }

    /** 连接断开即丢弃内存会话（token 不跨连接存活，不落盘）。 */
    @Test
    void connectionClosedClearsSession() {
        UserService service = new UserService(new FakeDispatcher(), TIMEOUT);
        service.getSession().cache("token-xyz", new SessionEntry("uuid-1", "001", "学生", 0L));

        service.connectionClosed(new IOException("peer reset"));

        assertFalse(service.isLoggedIn());
        assertNull(service.getSession().getToken());
        assertNull(service.getSession().getEntry());
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

    /** 查个人档案：回包里的姓名可直接用于界面显示，且请求带上会话 token。 */
    @Test
    void queryMyProfileReturnsRealName() throws Exception {
        FakeDispatcher dispatcher = new FakeDispatcher();
        UserProfile profile = new UserProfile("uuid-1", "001", "张三", "学生");
        dispatcher.reply(Command.USER_PROFILE_QUERY, response(StatusCode.SUCCESS, profile));
        UserService service = new UserService(dispatcher, TIMEOUT);
        service.getSession().cache("token-xyz", new SessionEntry("uuid-1", "001", "学生", 0L));

        UserProfile result = service.queryMyProfile();

        assertEquals("张三", result.getRealName());
        assertEquals("张三", result.getDisplayName());
        assertEquals(Command.USER_PROFILE_QUERY, dispatcher.m_sent.get(0).getCommand());
        assertEquals("token-xyz", dispatcher.m_sent.get(0).getToken());
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

        FakeDispatcher() {
            super();
        }

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
