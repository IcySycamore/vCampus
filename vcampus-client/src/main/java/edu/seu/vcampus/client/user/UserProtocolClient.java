package edu.seu.vcampus.client.user;

import edu.seu.vcampus.client.api.ApiErrors;
import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.client.network.ClientMessageDispatcher;
import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.message.PageResponse;
import edu.seu.vcampus.common.random.RandomGen;
import edu.seu.vcampus.common.user.dto.BatchResult;
import edu.seu.vcampus.common.user.dto.ChangePasswordRequest;
import edu.seu.vcampus.common.user.dto.LoginChallenge;
import edu.seu.vcampus.common.user.dto.LoginRequest;
import edu.seu.vcampus.common.user.dto.LoginResponse;
import edu.seu.vcampus.common.user.dto.LoginVerify;
import edu.seu.vcampus.common.user.entity.Role;
import edu.seu.vcampus.common.user.entity.SessionEntry;
import edu.seu.vcampus.common.user.entity.User;
import edu.seu.vcampus.common.util.Sha256Util;

/** Encodes user protocol messages and validates their responses. */
final class UserProtocolClient {
    private final ClientMessageDispatcher dispatcher;
    private final ClientSession session;
    private final long timeoutMillis;
    private final RandomGen random = new RandomGen();

    UserProtocolClient(ClientMessageDispatcher dispatcher, ClientSession session,
            long timeoutMillis) {
        if (dispatcher == null) {
            throw new IllegalArgumentException("dispatcher must not be null");
        }
        this.dispatcher = dispatcher;
        this.session = session;
        this.timeoutMillis = timeoutMillis;
    }

    void login(String userName, Role role, String password) {
        LoginChallenge challenge = requestChallenge(userName, role);
        LoginVerify verify = new LoginVerify();
        verify.m_user_name = userName;
        verify.m_proof = computeProof(challenge, password);
        Object data = call(Command.USER_LOGIN_VERIFY, verify).getData();
        if (!(data instanceof LoginResponse)) {
            throw new ApiException(ApiErrors.LOCAL_MALFORMED);
        }
        LoginResponse response = (LoginResponse) data;
        session.cache(response.m_token, response.m_session);
    }

    void changePassword(String oldPassword, String newPassword) {
        SessionEntry entry = session.getEntry();
        if (entry == null) {
            throw new ApiException(StatusCode.UNAUTHORIZED);
        }
        LoginChallenge challenge = requestChallenge(entry.getUsername(),
                Role.fromDisplayName(entry.getRole()));
        String newSalt = random.randomHex(16);
        call(Command.USER_CHANGE_PASSWORD, new ChangePasswordRequest(null,
                computeProof(challenge, oldPassword), newSalt,
                Sha256Util.sha256Hex(newSalt + newPassword)));
    }

    Message call(int command, Object payload) {
        Message request = new Message(command, payload);
        request.setToken(session.getToken());
        try {
            return requireSuccess(dispatcher.request(request, timeoutMillis));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(ApiErrors.LOCAL_INTERRUPTED);
        }
    }

    @SuppressWarnings("unchecked")
    PageResponse<User> userPage(Object data) {
        if (!(data instanceof PageResponse)) {
            throw new ApiException(ApiErrors.LOCAL_MALFORMED);
        }
        return (PageResponse<User>) data;
    }

    BatchResult batchResult(Object data) {
        if (!(data instanceof BatchResult)) {
            throw new ApiException(ApiErrors.LOCAL_MALFORMED);
        }
        return (BatchResult) data;
    }

    private LoginChallenge requestChallenge(String userName, Role role) {
        LoginRequest request = new LoginRequest();
        request.m_user_name = userName;
        request.m_role = role == null ? null : role.getDisplayName();
        Object data = call(Command.USER_LOGIN, request).getData();
        if (!(data instanceof LoginChallenge)) {
            throw new ApiException(ApiErrors.LOCAL_MALFORMED);
        }
        return (LoginChallenge) data;
    }

    private Message requireSuccess(Message response) {
        if (response == null) {
            throw new ApiException(ApiErrors.LOCAL_TIMEOUT);
        }
        if (!StatusCode.SUCCESS.equals(response.getStatusCode())) {
            throw new ApiException(response.getStatusCode());
        }
        return response;
    }

    private String computeProof(LoginChallenge challenge, String password) {
        String inner = Sha256Util.sha256Hex(challenge.m_salt + password);
        return Sha256Util.sha256Hex(challenge.m_nonce + inner);
    }
}
