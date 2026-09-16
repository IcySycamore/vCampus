package edu.seu.vcampus.client.user;

import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.client.user.UserServiceTestFixture.FakeDispatcher;
import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.user.dto.ChangePasswordRequest;
import edu.seu.vcampus.common.user.dto.LoginChallenge;
import edu.seu.vcampus.common.user.entity.SessionEntry;
import edu.seu.vcampus.common.util.Sha256Util;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.io.IOException;

import static edu.seu.vcampus.client.user.UserServiceTestFixture.response;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/** Tests password changes, logout, timeout and connection cleanup. */
class UserServiceLifecycleTest {
    private static final long TIMEOUT = 1000L;

    @Test
    void changePasswordUsesProofAndNewSalt() {
        FakeDispatcher dispatcher = new FakeDispatcher();
        LoginChallenge challenge = new LoginChallenge();
        challenge.m_salt = "salt-9";
        challenge.m_nonce = "nonce-9";
        dispatcher.reply(Command.USER_LOGIN, response(StatusCode.SUCCESS, challenge));
        dispatcher.reply(Command.USER_CHANGE_PASSWORD,
                response(StatusCode.SUCCESS, null));
        UserService service = loggedIn(dispatcher);
        service.changePassword("old-pw", "new-pw");
        ChangePasswordRequest payload =
                (ChangePasswordRequest) dispatcher.sent.get(1).getData();
        assertNull(payload.getUserName());
        String expected = Sha256Util.sha256Hex(
                "nonce-9" + Sha256Util.sha256Hex("salt-9old-pw"));
        assertEquals(expected, payload.getProof());
        assertNotNull(payload.getNewSalt());
        assertEquals(Sha256Util.sha256Hex(payload.getNewSalt() + "new-pw"),
                payload.getNewHash());
        assertEquals("token-xyz", dispatcher.sent.get(1).getToken());
    }

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
        assertTrue(dispatcher.sent.isEmpty());
    }

    @Test
    void logoutClearsSession() {
        FakeDispatcher dispatcher = new FakeDispatcher();
        dispatcher.reply(Command.USER_LOGOUT, response(StatusCode.SUCCESS, null));
        UserService service = loggedIn(dispatcher);
        service.logout();
        assertFalse(service.isLoggedIn());
        assertNull(service.currentToken());
        assertNull(service.currentSession());
        assertEquals("token-xyz", dispatcher.sent.get(0).getToken());
    }

    @Test
    void connectionClosedClearsSession() {
        UserService service = loggedIn(new FakeDispatcher());
        service.connectionClosed(new IOException("peer reset"));
        assertFalse(service.isLoggedIn());
        assertNull(service.currentSession());
    }

    @Test
    void timeoutBecomesLocalCode() {
        UserService service = new UserService(new FakeDispatcher(), TIMEOUT);
        try {
            service.logout();
            fail("expected ApiException");
        } catch (ApiException e) {
            assertTrue(e.isLocal());
            assertEquals("服务器无响应（超时）", e.getMessage());
        }
    }

    @Test
    void rejectsNullDispatcher() {
        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() {
                new UserService(null, TIMEOUT);
            }
        });
    }

    private UserService loggedIn(FakeDispatcher dispatcher) {
        UserService service = new UserService(dispatcher, TIMEOUT);
        service.session().cache("token-xyz",
                new SessionEntry("uuid-1", "001", "学生", 0L));
        return service;
    }
}
