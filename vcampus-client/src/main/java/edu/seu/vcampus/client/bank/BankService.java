package edu.seu.vcampus.client.bank;

import edu.seu.vcampus.client.api.ApiErrors;
import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.client.handler.ConnectionListener;
import edu.seu.vcampus.client.network.ClientMessageDispatcher;
import edu.seu.vcampus.client.user.UserService;
import edu.seu.vcampus.common.bank.dto.BankAccountResponse;
import edu.seu.vcampus.common.bank.dto.BankRechargeRequest;
import edu.seu.vcampus.common.bank.dto.BankRechargeResponse;
import edu.seu.vcampus.common.bank.dto.BankTransactionListResponse;
import edu.seu.vcampus.common.bank.dto.BankTransactionQueryRequest;
import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.NetworkConstant;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.message.Message;
import java.math.BigDecimal;

/** 银行同步客户端 API；请求复用分发器，身份取自共享用户会话。 */
public class BankService implements ConnectionListener {
    private final ClientMessageDispatcher dispatcher;
    private final UserService users;
    private volatile boolean disconnected;

    /**
     * 创建银行 API。相同实例的请求串行，兼容分发器每命令一个等待槽的约定。
     * @param dispatcher 共享分发器
     * @param users 共享用户服务
     */
    public BankService(ClientMessageDispatcher dispatcher, UserService users) {
        if (dispatcher == null || users == null) {
            throw new IllegalArgumentException("dispatcher and users are required");
        }
        this.dispatcher = dispatcher;
        this.users = users;
    }

    /** @return 当前登录用户的账户；未开户抛 B100 */
    public synchronized BankAccountResponse queryMyAccount() {
        return call(Command.BANK_ACCOUNT_QUERY, null, BankAccountResponse.class);
    }

    /** 旧接口仅保留源码兼容；新版服务端拒绝无开户资料的请求。
     * @return 旧服务端开户响应
     * @deprecated 请使用带校园密码和银行密码的重载 */
    @Deprecated
    public synchronized BankAccountResponse openAccount() {
        return call(Command.BANK_ACCOUNT_OPEN, null, BankAccountResponse.class);
    }

    /** @return 当前校园账号，仅用于银行表单预填 */
    public String currentUsername() {
        return users.currentSession() == null ? "" : users.currentSession().getUsername();
    }
    /**
     * @param name 当前校园账号
     * @param login 校园密码
     * @param password 银行密码
     * @return 开户结果；不修改主窗口会话 */
    public synchronized BankAccountResponse openAccount(String name, char[] login,
            char[] password) {
        BankOpening opening = new BankOpening(dispatcher);
        try {
            return call(Command.BANK_ACCOUNT_OPEN, opening.verify(users, name, login, password),
                    BankAccountResponse.class);
        } finally {
            opening.close();
        }
    }

    /**
     * 为本人账户充值。
     * @param amount 金额
     * @return 充值后的账户及流水
     */
    public synchronized BankRechargeResponse recharge(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0
                || amount.stripTrailingZeros().scale() > 2) {
            throw new ApiException(StatusCode.BAD_REQUEST, "请输入大于 0、最多两位小数的金额");
        }
        return call(Command.BANK_RECHARGE, new BankRechargeRequest(amount),
                BankRechargeResponse.class);
    }

    /**
     * 查询本人流水。
     * @param query 类型和分页条件；null 使用默认条件
     * @return 流水分页
     */
    public synchronized BankTransactionListResponse listMyTransactions(
            BankTransactionQueryRequest query) {
        return call(Command.BANK_TRANSACTION_LIST, query == null
                ? new BankTransactionQueryRequest() : query, BankTransactionListResponse.class);
    }

    /**
     * @param cause 连接关闭原因 */
    @Override
    public void connectionClosed(Exception cause) {
        disconnected = true;
    }

    private <T> T call(int command, Object payload, Class<T> resultType) {
        if (disconnected) {
            throw new ApiException(ApiErrors.LOCAL_NETWORK);
        }
        String token = users.currentToken();
        if (token == null) {
            throw new ApiException(StatusCode.UNAUTHORIZED);
        }
        Message request = new Message(command, payload);
        request.setToken(token);
        Message response;
        try {
            response = dispatcher.request(request, NetworkConstant.DEFAULT_REQUEST_TIMEOUT_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(ApiErrors.LOCAL_INTERRUPTED);
        } catch (RuntimeException e) {
            throw new ApiException(ApiErrors.LOCAL_NETWORK);
        }
        if (response == null) {
            throw new ApiException(disconnected || users.currentToken() == null
                    ? ApiErrors.LOCAL_NETWORK : ApiErrors.LOCAL_TIMEOUT);
        }
        if (response.getCommand() != command || request.getUid() == null
                || !request.getUid().equals(response.getUid())
                || response.getStatusCode() == null) {
            throw new ApiException(ApiErrors.LOCAL_MALFORMED);
        }
        if (StatusCode.UNAUTHORIZED.equals(response.getStatusCode())) {
            // 复用用户服务现有的会话清理入口，不修改用户模块或另存 token。
            users.connectionClosed(null);
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
