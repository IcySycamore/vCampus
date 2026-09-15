package edu.seu.vcampus.client.user;

import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.client.user.UserServiceTestFixture.FakeDispatcher;
import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.message.PageResponse;
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

import java.util.Arrays;

import static edu.seu.vcampus.client.user.UserServiceTestFixture.response;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/** Tests login and administrator commands exposed by UserService. */
class UserServiceTest {
    private static final long TIMEOUT = 1000L;

    @Test
    void loginSuccess() {
        FakeDispatcher dispatcher = new FakeDispatcher();
        LoginChallenge challenge = new LoginChallenge();
        challenge.m_salt = "salt-1";
        challenge.m_nonce = "nonce-1";
        dispatcher.reply(Command.USER_LOGIN, response(StatusCode.SUCCESS, challenge));
        LoginResponse login = new LoginResponse();
        login.m_token = "token-xyz";
        login.m_session = new SessionEntry("uuid-1", "001", "学生", 0L);
        dispatcher.reply(Command.USER_LOGIN_VERIFY, response(StatusCode.SUCCESS, login));
        final UserService service = new UserService(dispatcher, TIMEOUT);
        service.login("001", Role.STUDENT, "pw");
        assertTrue(service.isLoggedIn());
        assertEquals("token-xyz", service.currentToken());
        assertNotNull(service.currentSession());
        assertEquals("学生", service.currentSession().getRole());
        LoginVerify verify = (LoginVerify) dispatcher.sent.get(1).getData();
        String expected = Sha256Util.sha256Hex(
                "nonce-1" + Sha256Util.sha256Hex("salt-1pw"));
        assertEquals(expected, verify.m_proof);
        assertEquals("001", verify.m_user_name);
    }

    @Test
    void loginRejected() {
        FakeDispatcher dispatcher = new FakeDispatcher();
        LoginChallenge challenge = new LoginChallenge();
        challenge.m_salt = "s";
        challenge.m_nonce = "n";
        dispatcher.reply(Command.USER_LOGIN, response(StatusCode.SUCCESS, challenge));
        dispatcher.reply(Command.USER_LOGIN_VERIFY,
                response(StatusCode.UNAUTHORIZED, null));
        UserService service = new UserService(dispatcher, TIMEOUT);
        try {
            service.login("001", Role.STUDENT, "bad");
            fail("expected ApiException");
        } catch (ApiException e) {
            assertEquals(StatusCode.UNAUTHORIZED, e.getStatusCode());
            assertFalse(e.isLocal());
        }
    }

    @Test
    void malformedChallengePayload() {
        FakeDispatcher dispatcher = new FakeDispatcher();
        dispatcher.reply(Command.USER_LOGIN,
                response(StatusCode.SUCCESS, "not-a-challenge"));
        final UserService service = new UserService(dispatcher, TIMEOUT);
        assertThrows(ApiException.class, new Executable() {
            @Override
            public void execute() {
                service.login("001", Role.STUDENT, "pw");
            }
        });
    }

    @Test
    void registerCarriesAdminToken() {
        FakeDispatcher dispatcher = new FakeDispatcher();
        dispatcher.reply(Command.USER_REGISTER, response(StatusCode.SUCCESS, null));
        UserService service = loggedIn(dispatcher);
        service.register("002", Role.TEACHER, "pw");
        Message sent = dispatcher.sent.get(0);
        assertEquals(Command.USER_REGISTER, sent.getCommand());
        assertEquals("admin-token", sent.getToken());
    }

    @Test
    void unregisterSendsUserRef() {
        FakeDispatcher dispatcher = new FakeDispatcher();
        dispatcher.reply(Command.USER_UNREGISTER, response(StatusCode.SUCCESS, null));
        UserService service = loggedIn(dispatcher);
        service.unregister("002");
        Message sent = dispatcher.sent.get(0);
        assertEquals(Command.USER_UNREGISTER, sent.getCommand());
        assertEquals("002", ((UserRefRequest) sent.getData()).getUserName());
    }

    @Test
    void toggleEnabledSendsState() {
        FakeDispatcher dispatcher = new FakeDispatcher();
        dispatcher.reply(Command.USER_TOGGLE_ENABLED,
                response(StatusCode.SUCCESS, null));
        UserService service = loggedIn(dispatcher);
        service.toggleUserEnabled("002", false);
        UserEnabledRequest payload =
                (UserEnabledRequest) dispatcher.sent.get(0).getData();
        assertEquals("002", payload.getUserName());
        assertFalse(payload.isEnabled());
    }

    @Test
    void listUsersReturnsPage() {
        FakeDispatcher dispatcher = new FakeDispatcher();
        User user = new User("002", "李四", null, Role.TEACHER);
        user.setUuid("uuid-2");
        PageResponse<User> page = new PageResponse<User>(Arrays.asList(user), 1L, 1, 20);
        dispatcher.reply(Command.USER_LIST, response(StatusCode.SUCCESS, page));
        UserService service = loggedIn(dispatcher);
        PageResponse<User> result = service.listUsers(
                new UserQuery("李", Role.TEACHER, null, 1, 20));
        assertEquals(1, result.getItems().size());
        assertEquals("李四", result.getItems().get(0).getDisplayName());
        assertEquals(1L, result.getTotal());
    }

    @Test
    void listUsersRejectsMalformedPayload() {
        FakeDispatcher dispatcher = new FakeDispatcher();
        dispatcher.reply(Command.USER_LIST, response(StatusCode.SUCCESS, "oops"));
        final UserService service = loggedIn(dispatcher);
        assertThrows(ApiException.class, new Executable() {
            @Override
            public void execute() {
                service.listUsers(null);
            }
        });
    }

    private UserService loggedIn(FakeDispatcher dispatcher) {
        UserService service = new UserService(dispatcher, TIMEOUT);
        service.session().cache("admin-token",
                new SessionEntry("uuid-admin", "admin", "管理员", 0L));
        return service;
    }
}
