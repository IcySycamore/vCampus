package edu.seu.vcampus.client.user;

import edu.seu.vcampus.client.handler.ConnectionListener;
import edu.seu.vcampus.client.network.ClientMessageDispatcher;
import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.NetworkConstant;
import edu.seu.vcampus.common.message.PageResponse;
import edu.seu.vcampus.common.user.dto.BatchResult;
import edu.seu.vcampus.common.user.dto.RegisterRequest;
import edu.seu.vcampus.common.user.dto.UserEnabledRequest;
import edu.seu.vcampus.common.user.dto.UserQuery;
import edu.seu.vcampus.common.user.dto.UserRefRequest;
import edu.seu.vcampus.common.user.dto.UserUpdateRequest;
import edu.seu.vcampus.common.user.entity.Role;
import edu.seu.vcampus.common.user.entity.SessionEntry;
import edu.seu.vcampus.common.user.entity.User;

import java.util.List;

/**
 * Synchronous user API used by the UI. Authentication state is kept only in
 * the shared in-memory session and is cleared when the connection closes.
 */
public class UserService implements ConnectionListener {
    private final ClientSession m_session = new ClientSession();
    private final UserProtocolClient m_protocol;

    /**
     * Creates the API with the default timeout.
     * @param dispatcher message dispatcher
     */
    public UserService(ClientMessageDispatcher dispatcher) {
        this(dispatcher, NetworkConstant.DEFAULT_REQUEST_TIMEOUT_MILLIS);
    }

    /**
     * Creates the API with a request timeout.
     * @param dispatcher message dispatcher
     * @param timeoutMillis timeout in milliseconds
     */
    public UserService(ClientMessageDispatcher dispatcher, long timeoutMillis) {
        m_protocol = new UserProtocolClient(dispatcher, m_session, timeoutMillis);
    }

    /**
     * Completes challenge-response login and caches the returned session.
     * @param userName login name
     * @param role selected role
     * @param password plain password used only to calculate the proof
     */
    public void login(String userName, Role role, String password) {
        m_protocol.login(userName, role, password);
    }

    /** Logs out and always clears local authentication state. */
    public void logout() {
        try {
            m_protocol.call(Command.USER_LOGOUT, null);
        } finally {
            m_session.clear();
        }
    }

    /**
     * Changes the current user's password with a fresh salt.
     * @param oldPassword current password
     * @param newPassword replacement password
     */
    public void changePassword(String oldPassword, String newPassword) {
        m_protocol.changePassword(oldPassword, newPassword);
    }

    /**
     * Lists users for the administrator UI.
     * @param query filters and page coordinates
     * @return matching page
     */
    public PageResponse<User> listUsers(UserQuery query) {
        return m_protocol.userPage(m_protocol.call(Command.USER_LIST, query).getData());
    }

    /**
     * Updates a user's display name.
     * @param request update request
     */
    public void updateUser(UserUpdateRequest request) {
        m_protocol.call(Command.USER_UPDATE, request);
    }

    /**
     * Enables or disables a user.
     * @param userName target login name
     * @param enabled target state
     */
    public void toggleUserEnabled(String userName, boolean enabled) {
        m_protocol.call(Command.USER_TOGGLE_ENABLED,
                new UserEnabledRequest(userName, enabled));
    }

    /**
     * Registers a user whose display name defaults to the login name.
     * @param userName login name
     * @param role role
     * @param password initial password
     */
    public void register(String userName, Role role, String password) {
        register(userName, null, role, password);
    }

    /**
     * Registers a user and provisions its module profiles.
     * @param userName login name
     * @param displayName display name
     * @param role role
     * @param password initial password
     */
    public void register(String userName, String displayName, Role role, String password) {
        RegisterRequest request = new RegisterRequest();
        request.m_user_name = userName;
        request.m_display_name = displayName;
        request.m_role = role == null ? null : role.getDisplayName();
        request.m_password = password;
        m_protocol.call(Command.USER_REGISTER, request);
    }

    /**
     * Unregisters one user.
     * @param userName target login name
     */
    public void unregister(String userName) {
        m_protocol.call(Command.USER_UNREGISTER, new UserRefRequest(userName));
    }

    /**
     * Registers multiple users and returns individual failures.
     * @param requests registration requests
     * @return batch result
     */
    public BatchResult batchRegister(List<RegisterRequest> requests) {
        return m_protocol.batchResult(
                m_protocol.call(Command.USER_BATCH_REGISTER, requests).getData());
    }

    /**
     * Unregisters multiple users and returns individual failures.
     * @param userNames target login names
     * @return batch result
     */
    public BatchResult batchUnregister(List<String> userNames) {
        return m_protocol.batchResult(
                m_protocol.call(Command.USER_BATCH_UNREGISTER, userNames).getData());
    }

    /** @return true when token and session entry are both cached */
    public boolean isLoggedIn() {
        return m_session.isLoggedIn();
    }

    ClientSession session() {
        return m_session;
    }

    /** @return current server session, or null while logged out */
    public SessionEntry currentSession() {
        return m_session.getEntry();
    }

    /** @return current token, or null while logged out */
    public String currentToken() {
        return m_session.getToken();
    }

    @Override
    public void connectionClosed(Exception cause) {
        m_session.clear();
    }
}
