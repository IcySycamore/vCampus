package edu.seu.vcampus.client.library;

import edu.seu.vcampus.client.api.ApiErrors;
import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.client.network.ClientMessageDispatcher;
import edu.seu.vcampus.client.user.UserService;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.message.Message;

/** 图书馆请求适配；只引用现有用户 API，不拥有另一份身份缓存。 */
final class LibraryTransport {
    private final ClientMessageDispatcher dispatcher;
    private final UserService users;
    private final long timeoutMillis;

    LibraryTransport(ClientMessageDispatcher dispatcher, UserService users, long timeoutMillis) {
        if (dispatcher == null || users == null || timeoutMillis <= 0) {
            throw new IllegalArgumentException("library dependencies must be valid");
        }
        this.dispatcher = dispatcher;
        this.users = users;
        this.timeoutMillis = timeoutMillis;
    }

    // 当前分发器按命令码配对；串行请求避免同命令等待槽被后一个请求顶替。
    synchronized Object call(int command, Object data) {
        String token = users.currentToken();
        if (users.currentSession() == null || token == null) {
            throw new ApiException(StatusCode.UNAUTHORIZED);
        }
        Message request = new Message(command, data);
        request.setToken(token);
        Message response;
        try {
            response = dispatcher.request(request, timeoutMillis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ApiException(ApiErrors.LOCAL_INTERRUPTED);
        } catch (RuntimeException exception) {
            throw new ApiException(ApiErrors.LOCAL_NETWORK);
        }
        if (!token.equals(users.currentToken())) {
            throw new ApiException(StatusCode.UNAUTHORIZED);
        }
        if (response == null) {
            throw new ApiException(ApiErrors.LOCAL_TIMEOUT);
        }
        if (response.getCommand() != command || !request.getUid().equals(response.getUid())) {
            throw new ApiException(ApiErrors.LOCAL_MALFORMED);
        }
        if (StatusCode.UNAUTHORIZED.equals(response.getStatusCode())) {
            dispatcher.connectionClosed(new ApiException(StatusCode.UNAUTHORIZED));
        }
        if (!StatusCode.SUCCESS.equals(response.getStatusCode())) {
            throw new ApiException(response.getStatusCode(), response.getData() instanceof String
                    ? (String) response.getData() : ApiErrors.messageFor(response.getStatusCode()));
        }
        return response.getData();
    }
}
