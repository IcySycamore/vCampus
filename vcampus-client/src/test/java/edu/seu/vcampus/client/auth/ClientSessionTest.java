package edu.seu.vcampus.client.auth;

import edu.seu.vcampus.client.handler.UIUpdateHandler;
import edu.seu.vcampus.client.network.ClientSocket;
import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.message.MessageType;
import edu.seu.vcampus.common.user.dto.LoginChallenge;
import edu.seu.vcampus.common.user.dto.LoginRequest;
import edu.seu.vcampus.common.user.dto.LoginResponse;
import edu.seu.vcampus.common.user.dto.LoginVerify;
import edu.seu.vcampus.common.util.Sha256Util;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 验证挑战应答、真实角色、请求身份以及会话失效。 */
class ClientSessionTest {
    private ClientSocket socket;
    private ClientSession session;

    @BeforeEach
    void setUp() {
        socket = mock(ClientSocket.class);
        when(socket.isConnected()).thenReturn(true);
        session = new ClientSession(socket);
    }

    @Test
    void authenticatesThenAttachesServerTokenAndVerifiedUsername() throws Exception {
        answerLogin(StatusCode.SUCCESS, "server-token", "教师");
        char[] password = "secret".toCharArray();
        session.login("001", password, "管理员");

        assertTrue(session.isAuthenticated());
        assertEquals("001", session.getUsername());
        assertEquals("教师", session.getRole());
        for (char value : password) {
            assertEquals('\0', value);
        }
        Message request = new Message(MessageType.LIBRARY_LIST_BORROWS, null);
        request.setSender("forged-user");
        request.setToken("forged-token");
        session.send(request);
        assertEquals("001", request.getSender());
        assertEquals("server-token", request.getToken());
        verify(socket).connect();
        verify(socket).send(request);
    }

    @Test
    void rejectsWrongPasswordAndClosesTheConnection() throws Exception {
        answerLogin(StatusCode.UNAUTHORIZED, null, null);
        assertThrows(IOException.class, new Executable() {
            @Override
            public void execute() throws Exception {
                session.login("001", "wrong".toCharArray(), "学生");
            }
        });
        assertFalse(session.isAuthenticated());
        verify(socket).close();
    }

    @Test
    void rejectsSuccessWithoutAToken() throws Exception {
        answerLogin(StatusCode.SUCCESS, "", "学生");
        assertThrows(IOException.class, new Executable() {
            @Override
            public void execute() throws Exception {
                session.login("001", "secret".toCharArray(), "学生");
            }
        });
        assertFalse(session.isAuthenticated());
    }

    @Test
    void expiresSessionOnUnauthorizedResponse() throws Exception {
        answerLogin(StatusCode.SUCCESS, "token", "学生");
        session.login("001", "secret".toCharArray(), "学生");
        UIUpdateHandler handler = mock(UIUpdateHandler.class);
        session.setHandler(handler);
        Message response = new Message(MessageType.LIBRARY_SEARCH, null);
        response.setStatusCode(StatusCode.UNAUTHORIZED);
        session.handleMessage(response);

        assertFalse(session.isAuthenticated());
        verify(handler).handleMessage(response);
        assertThrows(IOException.class, new Executable() {
            @Override
            public void execute() throws Exception {
                session.send(new Message(MessageType.LIBRARY_SEARCH, null));
            }
        });
    }

    @Test
    void disconnectInvalidatesIdentityEvenIfTransportReconnects() throws Exception {
        answerLogin(StatusCode.SUCCESS, "token", "学生");
        session.login("001", "secret".toCharArray(), "学生");
        UIUpdateHandler handler = mock(UIUpdateHandler.class);
        session.setHandler(handler);
        IOException failure = new IOException("disconnected");
        session.connectionClosed(failure);

        assertFalse(session.isAuthenticated());
        verify(handler).connectionClosed(failure);
    }

    private void answerLogin(final String status, final String token, final String role)
            throws Exception {
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                Message request = invocation.getArgument(0);
                if (request.getCommand() == Command.USER_LOGIN) {
                    LoginRequest data = (LoginRequest) request.getData();
                    assertEquals("001", data.m_user_name);
                    LoginChallenge challenge = new LoginChallenge();
                    challenge.m_salt = "salt";
                    challenge.m_nonce = "nonce";
                    reply(Command.USER_LOGIN, StatusCode.SUCCESS, challenge);
                } else if (request.getCommand() == Command.USER_LOGIN_VERIFY) {
                    LoginVerify data = (LoginVerify) request.getData();
                    if (StatusCode.SUCCESS.equals(status)) {
                        assertEquals(Sha256Util.sha256Hex("nonce"
                                + Sha256Util.sha256Hex("saltsecret")), data.m_proof);
                    }
                    LoginResponse result = new LoginResponse();
                    result.m_token = token;
                    result.m_role = role;
                    reply(Command.USER_LOGIN_VERIFY, status, result);
                }
                return null;
            }
        }).when(socket).send(any(Message.class));
    }

    private void reply(int command, String status, Object data) {
        Message response = new Message(command, data);
        response.setStatusCode(status);
        session.handleMessage(response);
    }
}
