package edu.seu.vcampus.client.bank;

import edu.seu.vcampus.client.api.ApiErrors;
import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.client.network.ClientMessageDispatcher;
import edu.seu.vcampus.client.user.UserRequests;
import edu.seu.vcampus.client.user.UserService;
import edu.seu.vcampus.common.bank.dto.BankCampusPasswordChallengeRequest;
import edu.seu.vcampus.common.bank.dto.BankCampusPasswordVerifyRequest;
import edu.seu.vcampus.common.bank.dto.BankOpenRequest;
import edu.seu.vcampus.common.bank.security.BankPassword;
import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.NetworkConstant;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.user.dto.LoginChallenge;
import edu.seu.vcampus.common.user.entity.SessionEntry;
import java.util.Arrays;

/**
 * 开户前的一次性校园密码复核。
 *
 * <p>
 * 走的是<b>复核命令</b>（{@code BANK_PASSWORD_VERIFY*}）而不是「再登录一次」：再登录会新签一个登录会话，在
 * 「同账号只保留一个登录会话」的策略下会顶掉用户当前的登录态，而紧接着的银行命令带的仍是那个主会话 token —— 表现就是开户验证一过，界面立刻被判「登录状态已失效」。
 *
 * <p>
 * 复核命令由服务端签一个一次性 token（非独占，不影响当前会话）；密码错时服务端回 {@code BANK_CAMPUS_PASSWORD_INVALID}，不是
 * 401，因此也不会被误判成会话失效。
 */
final class BankOpening {

    /** 消息分发器：复核命令与业务命令共用同一条连接。 */
    private final ClientMessageDispatcher m_dispatcher;

    BankOpening(ClientMessageDispatcher dispatcher) {
        m_dispatcher = dispatcher;
    }

    BankOpenRequest verify(UserService users, String name, char[] login, char[] password) {
        SessionEntry current = users.currentSession();
        if (current == null) {
            throw new ApiException(StatusCode.UNAUTHORIZED);
        }
        if (!current.getUsername().equals(name) || login == null || login.length == 0) {
            throw new ApiException(StatusCode.BAD_REQUEST, "请输入当前校园账号及登录密码");
        }
        byte[] salt = BankPassword.newSalt();
        byte[] hash;
        try {
            hash = BankPassword.derive(password, salt);
        } catch (IllegalArgumentException e) {
            throw new ApiException(StatusCode.BAD_REQUEST, e.getMessage());
        }
        try {
            String token = users.currentToken();
            if (token == null) {
                throw new ApiException(StatusCode.UNAUTHORIZED);
            }
            LoginChallenge challenge = call(token, Command.BANK_PASSWORD_VERIFY_CHALLENGE,
                    new BankCampusPasswordChallengeRequest(name), LoginChallenge.class);
            String proof = UserRequests.computeProof(challenge, new String(login));
            String verificationToken = call(token, Command.BANK_PASSWORD_VERIFY,
                    new BankCampusPasswordVerifyRequest(name, proof), String.class);
            return new BankOpenRequest(name, verificationToken, salt, hash);
        } finally {
            Arrays.fill(hash, (byte) 0);
        }
    }

    /** 复核结束：这次复核不建立登录会话，没有需要收尾的东西。 */
    void close() {
        // 无需清理
    }

    /**
     * 发一条等待响应的请求（调用约定与 {@code BankService} 一致）。
     *
     * @param token      主会话 token
     * @param command    命令码
     * @param payload    载荷
     * @param resultType 响应类型
     * @param <T>        响应类型
     * @return 响应数据
     */
    private <T> T call(String token, int command, Object payload, Class<T> resultType) {
        Message request = new Message(command, payload);
        request.setToken(token);
        Message response;
        try {
            response = m_dispatcher.request(request,
                    NetworkConstant.DEFAULT_REQUEST_TIMEOUT_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(ApiErrors.LOCAL_INTERRUPTED);
        } catch (RuntimeException e) {
            throw new ApiException(ApiErrors.LOCAL_NETWORK);
        }
        if (response == null) {
            throw new ApiException(ApiErrors.LOCAL_TIMEOUT);
        }
        if (response.getStatusCode() == null) {
            throw new ApiException(ApiErrors.LOCAL_MALFORMED);
        }
        if (!StatusCode.SUCCESS.equals(response.getStatusCode())) {
            throw new ApiException(response.getStatusCode());
        }
        if (!resultType.isInstance(response.getData())) {
            throw new ApiException(ApiErrors.LOCAL_MALFORMED);
        }
        return resultType.cast(response.getData());
    }
}
