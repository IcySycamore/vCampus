package edu.seu.vcampus.client.bank;

import edu.seu.vcampus.client.handler.UIUpdateHandler;
import edu.seu.vcampus.client.network.ClientSocket;
import edu.seu.vcampus.common.bank.dto.BankAccountResponse;
import edu.seu.vcampus.common.bank.dto.BankRechargeRequest;
import edu.seu.vcampus.common.bank.dto.BankTransactionListResponse;
import edu.seu.vcampus.common.bank.dto.BankTransactionQueryRequest;
import edu.seu.vcampus.common.bank.entity.BankTransaction;
import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.message.Message;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 基于现有 {@link ClientSocket} 的银行客户端服务实现。
 *
 * <p>本类只负责 Bank 请求的 Message 封装、发送和响应转换，不包含任何 Swing
 * 控件。ClientSocket 收到响应后通过 {@link #handleMessage(Message)} 回调本类，
 * 本类再按消息 UID 找到对应的业务回调。</p>
 */
public class DefaultBankClientService
        implements BankClientService, UIUpdateHandler {

    /** 客户端生成请求 UID，避免发送后再注册回调产生竞态。 */
    private static final AtomicLong REQUEST_IDS = new AtomicLong();

    /** 底层通用客户端连接。 */
    private final ClientSocket clientSocket;

    /** 当前登录会话 token。 */
    private volatile String token;

    /** 请求 UID 到待处理回调的映射。 */
    private final Map<Long, PendingRequest<?>> pending =
            new ConcurrentHashMap<Long, PendingRequest<?>>();

    /**
     * 创建 Bank 客户端服务。
     *
     * @param clientSocket 已创建的通用客户端连接
     * @param token 当前登录用户 token
     */
    public DefaultBankClientService(ClientSocket clientSocket, String token) {
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

    /** {@inheritDoc} */
    @Override
    public void recharge(BigDecimal amount,
            BankClientCallback<BankTransaction> callback) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }
        sendRequest(Command.BANK_RECHARGE,
                new BankRechargeRequest(amount), callback,
                BankTransaction.class);
    }

    /** {@inheritDoc} */
    @Override
    public void listTransactions(BankTransactionQueryRequest request,
            BankClientCallback<BankTransactionListResponse> callback) {
        sendRequest(Command.BANK_TRANSACTION_LIST, request, callback,
                BankTransactionListResponse.class);
    }

    /** {@inheritDoc} */
    @Override
    public void listTransactions(
            BankClientCallback<BankTransactionListResponse> callback) {
        listTransactions(null, callback);
    }

    /** 接收 ClientSocket 转交的消息。 */
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
