package edu.seu.vcampus.client.bank;

import edu.seu.vcampus.client.handler.UIUpdateHandler;
import edu.seu.vcampus.client.network.ClientSocketListener;
import edu.seu.vcampus.common.bank.dto.BankAccountResponse;
import edu.seu.vcampus.common.bank.dto.BankRechargeRequest;
import edu.seu.vcampus.common.bank.dto.BankRechargeResponse;
import edu.seu.vcampus.common.bank.dto.BankTransactionListResponse;
import edu.seu.vcampus.common.bank.dto.BankTransactionQueryRequest;
import edu.seu.vcampus.common.bank.dto.BankPasswordRequest;
import edu.seu.vcampus.common.bank.dto.BankPasswordChangeRequest;
import edu.seu.vcampus.common.bank.dto.BankCampusPasswordChallengeRequest;
import edu.seu.vcampus.common.bank.dto.BankCampusPasswordVerifyRequest;
import edu.seu.vcampus.common.bank.security.BankPassword;
import edu.seu.vcampus.common.user.dto.LoginChallenge;
import edu.seu.vcampus.client.user.UserRequests;
import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.message.Message;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 基于现有 {@link ClientSocketListener} 的银行客户端服务实现。
 *
 * <p>
 * 本类只负责 Bank 请求的 Message 封装、发送和响应转换，不包含任何 Swing 控件。ClientSocket 收到响应后通过
 * {@link #handleMessage(Message)} 回调本类， 本类再按消息 UID 找到对应的业务回调。
 * </p>
 */
public class DefaultBankClientService
        implements BankClientService, UIUpdateHandler {

    /** 客户端生成请求 UID，避免发送后再注册回调产生竞态。 */
    private static final AtomicLong REQUEST_IDS = new AtomicLong();

    /** 底层通用客户端连接。 */
    private final ClientSocketListener clientSocket;

    /** 当前登录会话 token。 */
    private volatile String token;

    /** 请求 UID 到待处理回调的映射。 */
    private final Map<Long, PendingRequest<?>> pending = new ConcurrentHashMap<Long, PendingRequest<?>>();

    /**
     * 创建 Bank 客户端服务。
     *
     * @param clientSocket 已创建的通用客户端连接
     * @param token        当前登录用户 token
     */
    public DefaultBankClientService(ClientSocketListener clientSocket, String token) {
        if (clientSocket == null) {
            throw new IllegalArgumentException("clientSocket must not be null");
        }
        this.clientSocket = clientSocket;
        setToken(token);
    }

    /**
     * 更新当前登录 token，供重新登录或 token 刷新后使用。
     *
     * @param value 新 token
     */
    public final void setToken(String value) {
        if (value == null || value.trim().length() == 0) {
            throw new IllegalArgumentException("token must not be empty");
        }
        token = value;
    }

    /**
     * 清理当前 token 和尚未完成的请求。
     */
    public void clearPendingRequests() {
        pending.clear();
    }

    /** {@inheritDoc} */
    @Override
    public void openAccount(BankClientCallback<BankAccountResponse> callback) {
        sendRequest(Command.BANK_ACCOUNT_OPEN, null, callback,
                BankAccountResponse.class);
    }

    /** {@inheritDoc} */
    @Override
    public void queryAccount(BankClientCallback<BankAccountResponse> callback) {
        sendRequest(Command.BANK_ACCOUNT_QUERY, null, callback,
                BankAccountResponse.class);
    }

    @Override
    public void queryMyAccount(BankClientCallback<BankAccountResponse> callback) {
        queryAccount(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void recharge(BigDecimal amount,
            BankClientCallback<BankRechargeResponse> callback) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }
        sendRequest(Command.BANK_RECHARGE,
                new BankRechargeRequest(amount), callback,
                BankRechargeResponse.class);
    }

    /** {@inheritDoc} */
    @Override
    public void listTransactions(BankTransactionQueryRequest request,
            BankClientCallback<BankTransactionListResponse> callback) {
        sendRequest(Command.BANK_TRANSACTION_LIST, request, callback,
                BankTransactionListResponse.class);
    }

    @Override
    public void listMyTransactions(BankTransactionQueryRequest request,
            BankClientCallback<BankTransactionListResponse> callback) {
        listTransactions(request, callback);
    }

    /** {@inheritDoc} */
    @Override
    public void listTransactions(
            BankClientCallback<BankTransactionListResponse> callback) {
        listTransactions(null, callback);
    }

    @Override
    public void freezeAccount(char[] password, BankClientCallback<BankAccountResponse> callback) {
        sendRequest(Command.BANK_ACCOUNT_FREEZE, new BankPasswordRequest(password), callback, BankAccountResponse.class);
    }

    @Override
    public void unfreezeAccount(char[] password, BankClientCallback<BankAccountResponse> callback) {
        sendRequest(Command.BANK_ACCOUNT_UNFREEZE, new BankPasswordRequest(password), callback, BankAccountResponse.class);
    }
    @Override public void changePassword(char[] currentPassword, char[] newPassword, BankClientCallback<BankAccountResponse> callback) {
        if (callback != null) {
            callback.onFailure(StatusCode.BANK_CAMPUS_PASSWORD_INVALID,
                    "需要当前校园账号和校园系统密码");
        }
    }

    /**
     * 使用当前校园账号和密码完成银行密码修改的异步入口。
     * 三个请求按顺序执行，银行修改请求只携带专用一次性 token。
     *
     * @param username 当前校园账号
     * @param campusPassword 校园系统密码
     * @param currentPassword 当前银行密码
     * @param newPassword 新银行密码
     * @param callback 修改结果回调
     */
    public void changePassword(final String username, final char[] campusPassword,
            final char[] currentPassword, final char[] newPassword,
            final BankClientCallback<BankAccountResponse> callback) {
        if (callback == null) {
            throw new IllegalArgumentException("callback must not be null");
        }
        if (username == null || username.trim().length() == 0
                || campusPassword == null || campusPassword.length == 0
                || currentPassword == null || currentPassword.length == 0
                || newPassword == null || newPassword.length < 8 || newPassword.length > 64) {
            callback.onFailure(StatusCode.BAD_REQUEST, "银行密码修改参数无效");
            return;
        }
        final String name = username.trim();
        sendRequest(Command.BANK_PASSWORD_VERIFY_CHALLENGE,
                new BankCampusPasswordChallengeRequest(name),
                new BankClientCallback<LoginChallenge>() {
                    @Override public void onSuccess(LoginChallenge challenge) {
                        final String proof = UserRequests.computeProof(challenge,
                                new String(campusPassword));
                        sendRequest(Command.BANK_PASSWORD_VERIFY,
                                new BankCampusPasswordVerifyRequest(name, proof),
                                new BankClientCallback<String>() {
                                    @Override public void onSuccess(String verificationToken) {
                                        byte[] salt = BankPassword.newSalt();
                                        byte[] hash;
                                        try {
                                            hash = BankPassword.derive(newPassword, salt);
                                        } catch (RuntimeException error) {
                                            java.util.Arrays.fill(salt, (byte) 0);
                                            callback.onFailure(StatusCode.BANK_PASSWORD_POLICY, error);
                                            return;
                                        }
                                        try {
                                            sendRequest(Command.BANK_PASSWORD_CHANGE,
                                                    new BankPasswordChangeRequest(name,
                                                            verificationToken, currentPassword,
                                                            salt, hash), callback,
                                                    BankAccountResponse.class);
                                        } finally {
                                            java.util.Arrays.fill(salt, (byte) 0);
                                            java.util.Arrays.fill(hash, (byte) 0);
                                        }
                                    }
                                    @Override public void onFailure(String code, Object data) {
                                        callback.onFailure(code, data);
                                    }
                                }, String.class);
                    }
                    @Override public void onFailure(String code, Object data) {
                        callback.onFailure(code, data);
                    }
                }, LoginChallenge.class);
    }

    /** 接收 ClientSocketListener 转交的消息。 */
    @Override
    public void handleMessage(Message message) {
        if (message == null || message.getUid() == null) {
            return;
        }
        PendingRequest<?> request = pending.remove(message.getUid());
        if (request != null) {
            request.complete(message);
        }
    }

    /** 连接断开时将未完成请求通知给 UI。 */
    @Override
    public void connectionClosed(Exception cause) {
        for (PendingRequest<?> request : pending.values()) {
            request.fail(StatusCode.INTERNAL_ERROR, cause);
        }
        pending.clear();
    }

    private <T> void sendRequest(int command, Object data,
            BankClientCallback<T> callback, Class<T> resultType) {
        if (callback == null) {
            throw new IllegalArgumentException("callback must not be null");
        }
        if (!clientSocket.isConnected()) {
            callback.onFailure(StatusCode.INTERNAL_ERROR,
                    new IOException("client is not connected"));
            return;
        }
        Message request = new Message(command, data);
        request.setUid(REQUEST_IDS.incrementAndGet());
        request.setToken(token);
        pending.put(request.getUid(),
                new PendingRequest<T>(callback, resultType));
        try {
            clientSocket.send(request);
        } catch (IOException exception) {
            pending.remove(request.getUid());
            callback.onFailure(StatusCode.INTERNAL_ERROR, exception);
        }
    }

    /** 一个请求及其结果转换器。 */
    private static final class PendingRequest<T> {
        private final BankClientCallback<T> callback;
        private final Class<T> resultType;

        private PendingRequest(BankClientCallback<T> callback,
                Class<T> resultType) {
            this.callback = callback;
            this.resultType = resultType;
        }

        private void complete(Message response) {
            if (!StatusCode.SUCCESS.equals(response.getStatusCode())) {
                callback.onFailure(response.getStatusCode(), response.getData());
                return;
            }
            Object data = response.getData();
            if (data == null || !resultType.isInstance(data)) {
                callback.onFailure(StatusCode.INTERNAL_ERROR, data);
                return;
            }
            callback.onSuccess(resultType.cast(data));
        }

        private void fail(String statusCode, Object data) {
            callback.onFailure(statusCode, data);
        }
    }
}
