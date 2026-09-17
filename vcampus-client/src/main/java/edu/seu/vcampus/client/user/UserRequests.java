package edu.seu.vcampus.client.user;

import edu.seu.vcampus.client.api.ApiErrors;
import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.client.network.ClientMessageDispatcher;
import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.message.PageResponse;
import edu.seu.vcampus.common.user.dto.BatchResult;
import edu.seu.vcampus.common.user.dto.LoginChallenge;
import edu.seu.vcampus.common.user.dto.LoginRequest;
import edu.seu.vcampus.common.user.entity.Role;
import edu.seu.vcampus.common.user.entity.User;
import edu.seu.vcampus.common.util.Sha256Util;

/**
 * 用户模块的「发请求」细节：带 token、等待响应、状态码归一、载荷转换。
 *
 * <p>
 * 从 {@code UserService} 抽出（原文件 300+ 行，破 200 行上限），也避免「我的轨」与「管理轨」 各写一份
 * call/requireSuccess。包内可见，不对外暴露。
 */
public final class UserRequests {

    /** 消息分发器。 */
    private final ClientMessageDispatcher m_dispatcher;

    /** 内存会话（提供 token）。 */
    private final ClientSession m_session;

    /** 请求超时，毫秒。 */
    private final long m_timeout_millis;

    /**
     * 构造请求工具。
     *
     * @param dispatcher    消息分发器
     * @param session       内存会话
     * @param timeoutMillis 请求超时，毫秒
     */
    UserRequests(ClientMessageDispatcher dispatcher, ClientSession session, long timeoutMillis) {
        this.m_dispatcher = dispatcher;
        this.m_session = session;
        this.m_timeout_millis = timeoutMillis;
    }

    /**
     * 发请求并校验状态码：受检异常归一为本地错误码，非 2xx 抛 {@link ApiException}。
     *
     * @param command 命令码
     * @param payload 载荷；可为 null
     * @return 成功响应
     * @throws ApiException 超时、断线、被中断或服务器返回非成功状态码
     */
    Message call(int command, Object payload) {
        Message request = new Message(command, payload);
        request.setToken(m_session.getToken());
        try {
            return requireSuccess(m_dispatcher.request(request, m_timeout_millis));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(ApiErrors.LOCAL_INTERRUPTED);
        }
    }

    /**
     * 请求登录挑战（挑战-应答第①步）。
     *
     * @param userName 登录名
     * @param role     登录页选定身份
     * @return 挑战（salt + nonce）
     * @throws ApiException 未知用户、身份不符或本地失败
     */
    LoginChallenge requestChallenge(String userName, Role role) {
        LoginRequest request = new LoginRequest();
        request.m_user_name = userName;
        request.m_role = role == null ? null : role.getDisplayName();
        Object data = call(Command.USER_LOGIN, request).getData();
        if (!(data instanceof LoginChallenge)) {
            throw new ApiException(ApiErrors.LOCAL_MALFORMED);
        }
        return (LoginChallenge) data;
    }

    /** 状态码校验。 */
    private Message requireSuccess(Message response) {
        if (response == null) {
            throw new ApiException(ApiErrors.LOCAL_TIMEOUT);
        }
        if (!StatusCode.SUCCESS.equals(response.getStatusCode())) {
            throw new ApiException(response.getStatusCode());
        }
        return response;
    }

    /**
     * 计算应答 proof：{@code sha256(nonce + sha256(salt + password))}。
     *
     * @param challenge 登录挑战
     * @param password  明文密码
     * @return proof 十六进制串
     */
    public static String computeProof(LoginChallenge challenge, String password) {
        String inner = Sha256Util.sha256Hex(challenge.m_salt + password);
        return Sha256Util.sha256Hex(challenge.m_nonce + inner);
    }

    /**
     * 把载荷转成分页结果。
     *
     * @param data 响应载荷
     * @return 分页结果
     * @throws ApiException 载荷类型不符（协议异常）
     */
    @SuppressWarnings("unchecked")
    static PageResponse<User> userPage(Object data) {
        if (!(data instanceof PageResponse)) {
            throw new ApiException(ApiErrors.LOCAL_MALFORMED);
        }
        return (PageResponse<User>) data;
    }

    /**
     * 把载荷转成批量结果。
     *
     * @param data 响应载荷
     * @return 批量结果
     * @throws ApiException 载荷类型不符（协议异常）
     */
    static BatchResult batchResult(Object data) {
        if (!(data instanceof BatchResult)) {
            throw new ApiException(ApiErrors.LOCAL_MALFORMED);
        }
        return (BatchResult) data;
    }
}
