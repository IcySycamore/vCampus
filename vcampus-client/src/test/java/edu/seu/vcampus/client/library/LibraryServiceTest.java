package edu.seu.vcampus.client.library;

import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.client.api.ApiErrors;
import edu.seu.vcampus.client.api.ClientApis;
import edu.seu.vcampus.client.network.ClientMessageDispatcher;
import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.message.MessageSender;
import edu.seu.vcampus.common.user.dto.LoginChallenge;
import edu.seu.vcampus.common.user.dto.LoginResponse;
import edu.seu.vcampus.common.user.entity.Role;
import edu.seu.vcampus.common.user.entity.SessionEntry;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** 验证图书馆确实复用用户 API 的会话、uid 分配与统一错误模型。 */
class LibraryServiceTest {
    private final ClientMessageDispatcher dispatcher = new ClientMessageDispatcher();
    private final ClientApis apis = ClientApis.create(dispatcher);
    private Message sent;
    private String token = "token-one";
    private String status = StatusCode.SUCCESS;
    private Object payload = Collections.emptyList();
    private boolean mismatchedUid;
    private boolean silent;

    @BeforeEach
    void loginThroughExistingUserModule() {
        dispatcher.bindSender(new MessageSender() {
            @Override
            public void send(Message request) {
                sent = request;
                if (silent) {
                    return;
                }
                Message reply = new Message(request.getCommand(), payload);
                reply.setUid(mismatchedUid ? request.getUid() + 1 : request.getUid());
                reply.setStatusCode(status);
                if (request.getCommand() == Command.USER_LOGIN) {
                    LoginChallenge challenge = new LoginChallenge();
                    challenge.m_salt = "salt";
                    challenge.m_nonce = "nonce";
                    reply.setData(challenge);
                } else if (request.getCommand() == Command.USER_LOGIN_VERIFY) {
                    LoginResponse result = new LoginResponse();
                    result.m_token = token;
                    result.m_session = new SessionEntry("uuid-001", "001", "学生", Long.MAX_VALUE);
                    reply.setData(result);
                }
                dispatcher.dispatch(reply);
            }
        });
        apis.user().login("001", Role.STUDENT, "secret");
    }

    @Test
    void sharesSessionAndSendsOnlyCurrentTokenWithDispatcherUid() {
        assertSame(apis.user().currentSession(), apis.library().currentSession());
        apis.library().listMyBorrows();
        assertNotNull(sent.getUid());
        assertEquals(token, sent.getToken());
        assertNull(sent.getSender());
        token = "token-two";
        apis.user().login("001", Role.STUDENT, "secret");
        apis.library().listMyBorrows();
        assertEquals("token-two", sent.getToken());
    }

    @Test
    void logoutImmediatelyInvalidatesLibraryWithoutAnotherCache() {
        apis.user().logout();
        assertNull(apis.library().currentSession());
        assertFalse(apis.library().isLoggedIn());
        assertEquals(StatusCode.UNAUTHORIZED, failure(apis.library()).getStatusCode());
        assertEquals(Command.USER_LOGOUT, sent.getCommand());
    }

    @Test
    void unauthorizedReplyInvalidatesTheSharedUserSession() {
        status = StatusCode.UNAUTHORIZED;
        assertEquals(StatusCode.UNAUTHORIZED, failure(apis.library()).getStatusCode());
        assertNull(apis.user().currentSession());
        assertNull(apis.user().currentToken());
    }

    @Test
    void connectionClosurePreventsFurtherLibraryRequests() {
        dispatcher.connectionClosed(null);
        assertEquals(StatusCode.UNAUTHORIZED, failure(apis.library()).getStatusCode());
    }

    @Test
    void rejectsMalformedPayloadAndMismatchedResponseUid() {
        payload = "not a list";
        assertEquals(ApiErrors.LOCAL_MALFORMED, failure(apis.library()).getStatusCode());
        payload = Collections.emptyList();
        mismatchedUid = true;
        assertEquals(ApiErrors.LOCAL_MALFORMED, failure(apis.library()).getStatusCode());
    }

    @Test
    void surfacesTimeoutInsteadOfLeavingThePagePendingForever() {
        silent = true;
        LibraryService shortTimeout = new LibraryService(dispatcher, apis.user(), 20);
        assertEquals(ApiErrors.LOCAL_TIMEOUT, failure(shortTimeout).getStatusCode());
    }

    private ApiException failure(final LibraryService service) {
        return assertThrows(ApiException.class, new Executable() {
            @Override
            public void execute() {
                service.listMyBorrows();
            }
        });
    }
}
