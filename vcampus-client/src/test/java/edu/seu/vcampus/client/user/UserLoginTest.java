package edu.seu.vcampus.client.user;

import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.user.dto.LoginChallenge;
import edu.seu.vcampus.common.user.dto.LoginResponse;
import edu.seu.vcampus.common.user.dto.LoginVerify;
import edu.seu.vcampus.common.user.entity.Role;
import edu.seu.vcampus.common.user.entity.SessionEntry;
import edu.seu.vcampus.common.util.Sha256Util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * 登录挑战-应答链路测试（从 {@code UserServiceTest} 拆出，避免单个测试文件超长）。
 */
class UserLoginTest {

    /** 请求超时。 */
    private static final long TIMEOUT = 1000L;

    /** 登录成功：token 只进入内存会话，proof 与客户端公式一致。 */
    @Test
    void loginSuccess() {
        FakeUserDispatcher dispatcher = new FakeUserDispatcher();
        LoginChallenge challenge = new LoginChallenge();
        challenge.m_salt = "salt-1";
        challenge.m_nonce = "nonce-1";
        dispatcher.reply(Command.USER_LOGIN, response(StatusCode.SUCCESS, challenge));
        LoginResponse loginResponse = new LoginResponse();
        loginResponse.m_token = "token-xyz";
        loginResponse.m_session = new SessionEntry("uuid-1", "001", "张三", "学生", 0L);
        dispatcher.reply(Command.USER_LOGIN_VERIFY, response(StatusCode.SUCCESS, loginResponse));

        UserService service = new UserService(dispatcher, TIMEOUT);
        service.login("001", "pw");

        assertTrue(service.isLoggedIn());
        assertEquals("token-xyz", service.currentToken());
        assertNotNull(service.currentSession());
        assertEquals("学生", service.currentSession().getRole());
        assertEquals("张三", service.currentSession().getDisplayName());

        LoginVerify verify = (LoginVerify) dispatcher.sent.get(1).getData();
        String expected = Sha256Util.sha256Hex("nonce-1" + Sha256Util.sha256Hex("salt-1" + "pw"));
        assertEquals(expected, verify.m_proof);
        assertEquals("001", verify.m_user_name);
    }

    /** 服务器拒绝登录时抛出带状态码的 ApiException。 */
    @Test
    void loginRejected() {
        FakeUserDispatcher dispatcher = new FakeUserDispatcher();
        LoginChallenge challenge = new LoginChallenge();
        challenge.m_salt = "s";
        challenge.m_nonce = "n";
        dispatcher.reply(Command.USER_LOGIN, response(StatusCode.SUCCESS, challenge));
        dispatcher.reply(Command.USER_LOGIN_VERIFY, response(StatusCode.UNAUTHORIZED, null));

        UserService service = new UserService(dispatcher, TIMEOUT);
        try {
            service.login("001", "bad");
            fail("expected ApiException");
        } catch (ApiException e) {
            assertEquals(StatusCode.UNAUTHORIZED, e.getStatusCode());
            assertFalse(e.isLocal());
        }
    }

    /** 响应载荷类型不符时抛本地格式异常码。 */
    @Test
    void malformedChallengePayload() {
        FakeUserDispatcher dispatcher = new FakeUserDispatcher();
        dispatcher.reply(Command.USER_LOGIN, response(StatusCode.SUCCESS, "not-a-challenge"));

        UserService service = new UserService(dispatcher, TIMEOUT);
        try {
            service.login("001", "pw");
            fail("expected ApiException");
        } catch (ApiException e) {
            assertTrue(e.isLocal());
        }
    }

    private Message response(String status, Object data) {
        Message message = new Message(0, data);
        message.setStatusCode(status);
        return message;
    }
}
