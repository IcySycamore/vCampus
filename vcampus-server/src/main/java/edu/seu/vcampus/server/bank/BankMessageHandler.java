package edu.seu.vcampus.server.bank;

import edu.seu.vcampus.common.bank.exception.BankAccountNotOpenedException;
import edu.seu.vcampus.common.bank.dto.BankRechargeRequest;
import edu.seu.vcampus.common.bank.dto.BankOpenRequest;
import edu.seu.vcampus.common.bank.dto.BankTransactionQueryRequest;
import edu.seu.vcampus.common.bank.dto.BankPasswordRequest;
import edu.seu.vcampus.common.bank.dto.BankPasswordChangeRequest;
import edu.seu.vcampus.common.bank.dto.BankCampusPasswordChallengeRequest;
import edu.seu.vcampus.common.bank.dto.BankCampusPasswordVerifyRequest;
import edu.seu.vcampus.common.user.entity.SessionEntry;
import edu.seu.vcampus.server.user.AuthService;
import edu.seu.vcampus.server.user.SessionManager;
import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.bank.dto.BankAdminAccountView;
import edu.seu.vcampus.common.bank.dto.BankAdminQuery;
import edu.seu.vcampus.common.bank.dto.BankAdminRefRequest;
import edu.seu.vcampus.common.bank.dto.BankAdminResetPasswordRequest;
import edu.seu.vcampus.common.bank.dto.BankAdminSetFrozenRequest;
import edu.seu.vcampus.common.bank.dto.BankAdminTransactionsRequest;
import edu.seu.vcampus.common.bank.dto.BankTransactionListResponse;
import edu.seu.vcampus.common.user.entity.Role;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.message.MessageHandler;
import edu.seu.vcampus.common.message.MessageSender;
import edu.seu.vcampus.common.message.Message;

/**
 * 银行命令处理器：601 查询、602 充值、603 流水、604 独立开户、607 改银行密码，以及 610-614 管理轨命令（仅管理员）。
 *
 * <p>
 * token 的合法性由服务器会话层统一检查，本类通过 {@link BankIdentityResolver} 获取校验后的用户编号；没有可信身份时直接回
 * 401。
 * </p>
 */
public class BankMessageHandler implements MessageHandler {

    private final BankService bankService;
    private final BankIdentityResolver identityResolver;
    private BankAdminService bankAdmin;

    private final AuthService auth;

    /**
     * 创建银行处理器。
     *
     * @param bankService 银行业务服务
     * @param identityResolver 认证身份解析器
     */
    public BankMessageHandler(BankService bankService, BankIdentityResolver identityResolver) {
        this(bankService, identityResolver, AuthService.getInstance());
    }

    /**
     * 创建带银行管理端服务的处理器。
     *
     * @param bankService 银行核心服务
     * @param identityResolver 认证身份解析器
     * @param auth 共享认证服务
     * @param bankAdmin 银行管理端服务；null 表示未装配，管理轨命令将回500
     */
    public BankMessageHandler(BankService bankService,
            BankIdentityResolver identityResolver, AuthService auth,
            BankAdminService bankAdmin) {
        this(bankService, identityResolver, auth);
        this.bankAdmin = bankAdmin;
    }

    /** 创建带共享认证服务的银行处理器。 */
    public BankMessageHandler(BankService bankService, BankIdentityResolver identityResolver,
            AuthService auth) {
        if (bankService == null) {
            throw new IllegalArgumentException("bankService must not be null");
        }
        if (identityResolver == null) {
            throw new IllegalArgumentException("identityResolver must not be null");
        }
        this.bankService = bankService;
        this.identityResolver = identityResolver;
        if (auth == null) {
            throw new IllegalArgumentException("auth must not be null");
        }
        this.auth = auth;
    }

    /**
     * 按银行命令处理请求并通过 sender 发送一条响应。
     *
     * @param request 银行请求
     * @param sender 响应发送器
     */
    @Override
    public void handle(Message request, MessageSender sender) {
        if (request == null || sender == null) {
            throw new IllegalArgumentException("request and sender are required");
        }
        try {
            if (request.getToken() == null || request.getToken().trim().isEmpty()) {
                send(sender, request, StatusCode.UNAUTHORIZED, null);
                return;
            }
            String ownerUuid = identityResolver.resolveOwnerUuid(request);
            if (ownerUuid == null || ownerUuid.trim().length() == 0) {
                send(sender, request, StatusCode.UNAUTHORIZED, null);
                return;
            }
            switch (request.getCommand()) {
                case Command.BANK_ACCOUNT_OPEN:
                    openAccount(request, sender, ownerUuid);
                    return;
                case Command.BANK_ACCOUNT_QUERY:
                    queryAccount(request, sender, ownerUuid);
                    return;
                case Command.BANK_RECHARGE:
                    recharge(request, sender, ownerUuid);
                    return;
                case Command.BANK_TRANSACTION_LIST:
                    listTransactions(request, sender, ownerUuid);
                    return;
                case Command.BANK_ACCOUNT_FREEZE:
                    freeze(request, sender, ownerUuid, true);
                    return;
                case Command.BANK_ACCOUNT_UNFREEZE:
                    freeze(request, sender, ownerUuid, false);
                    return;
                case Command.BANK_PASSWORD_VERIFY_CHALLENGE:
                    campusPasswordChallenge(request, sender);
                    return;
                case Command.BANK_PASSWORD_VERIFY:
                    campusPasswordVerify(request, sender);
                    return;
                case Command.BANK_PASSWORD_CHANGE:
                    changePassword(request, sender, ownerUuid);
                    return;
                case Command.BANK_ADMIN_LIST_ACCOUNTS:
                    adminListAccounts(request, sender);
                    return;
                case Command.BANK_ADMIN_QUERY_ACCOUNT:
                    adminQueryAccount(request, sender);
                    return;
                case Command.BANK_ADMIN_TRANSACTION_LIST:
                    adminTransactions(request, sender);
                    return;
                case Command.BANK_ADMIN_SET_FROZEN:
                    adminSetFrozen(request, sender);
                    return;
                case Command.BANK_ADMIN_RESET_PASSWORD:
                    adminResetPassword(request, sender);
                    return;
                default:
                    send(sender, request, StatusCode.BAD_REQUEST, null);
            }
        } catch (BankAccountNotOpenedException e) {
            send(sender, request, Command.BANK_ACCOUNT_NOT_OPENED, e);
        } catch (IllegalStateException e) {
            send(sender, request, StatusCode.FORBIDDEN, null);
        } catch (IllegalArgumentException e) {
            send(sender, request, StatusCode.BAD_REQUEST, null);
        } catch (RuntimeException e) {
            send(sender, request, StatusCode.INTERNAL_ERROR, null);
        }
    }

    private void campusPasswordChallenge(Message request, MessageSender sender) {
        if (!(request.getData() instanceof BankCampusPasswordChallengeRequest)) {
            send(sender, request, StatusCode.BAD_REQUEST, null);
            return;
        }
        BankCampusPasswordChallengeRequest payload =
                (BankCampusPasswordChallengeRequest) request.getData();
        SessionEntry current = auth.getSessionManager().validate(request.getToken());
        if (current == null || !current.getUsername().equals(payload.getUsername())) {
            send(sender, request, StatusCode.BANK_CAMPUS_PASSWORD_INVALID, null);
            return;
        }
        send(sender, request, StatusCode.SUCCESS, auth.loginChallenge(payload.getUsername()));
    }

    private void campusPasswordVerify(Message request, MessageSender sender) {
        if (!(request.getData() instanceof BankCampusPasswordVerifyRequest)) {
            send(sender, request, StatusCode.BAD_REQUEST, null);
            return;
        }
        BankCampusPasswordVerifyRequest payload =
                (BankCampusPasswordVerifyRequest) request.getData();
        SessionEntry current = auth.getSessionManager().validate(request.getToken());
        if (current == null || !current.getUsername().equals(payload.getUsername())) {
            send(sender, request, StatusCode.BANK_CAMPUS_PASSWORD_INVALID, null);
            return;
        }
        String token = auth.verifyCampusPassword(payload.getUsername(), payload.getProof());
        if (token == null) {
            send(sender, request, StatusCode.BANK_CAMPUS_PASSWORD_INVALID, null);
            return;
        }
        send(sender, request, StatusCode.SUCCESS, token);
    }

    private void changePassword(Message request, MessageSender sender, String ownerUuid) {
        if (!(request.getData() instanceof BankPasswordChangeRequest)) {
            send(sender, request, StatusCode.BAD_REQUEST, null);
            return;
        }
        BankPasswordChangeRequest payload = (BankPasswordChangeRequest) request.getData();
        String token = payload.getVerificationToken();
        SessionManager sessions = auth.getSessionManager();
        SessionEntry current = sessions.validate(request.getToken());
        if (current == null || !ownerUuid.equals(current.getUuid())) {
            send(sender, request, StatusCode.UNAUTHORIZED, null);
            return;
        }
        SessionEntry verified = sessions.validate(token);
        if (token == null || token.equals(request.getToken()) || verified == null
                || !ownerUuid.equals(verified.getUuid())
                || !verified.getUsername().equals(payload.getUsername())) {
            if (token != null) {
                sessions.invalidate(token);
            }
            send(sender, request, StatusCode.BANK_CAMPUS_PASSWORD_INVALID, null);
            return;
        }
        char[] old = payload.getCurrentPassword();
        try {
            send(sender, request, StatusCode.SUCCESS,
                    bankService.changePassword(ownerUuid, old, payload.getSalt(), payload.getHash()));
        } catch (IllegalStateException e) {
            String message = e.getMessage();
            send(sender, request,
                    message != null && message.indexOf("错误次数") >= 0
                            ? StatusCode.BANK_PASSWORD_LOCKED : StatusCode.BANK_PASSWORD_INVALID,
                    null);
        } catch (IllegalArgumentException e) {
            String message = e.getMessage();
            send(sender, request,
                    message != null && message.indexOf("银行密码错误") >= 0
                            ? StatusCode.BANK_PASSWORD_INVALID : StatusCode.BANK_PASSWORD_POLICY,
                    null);
        } finally {
            java.util.Arrays.fill(old, '\0');
            sessions.invalidate(token);
        }
    }
    private void freeze(Message request, MessageSender sender, String ownerUuid, boolean freeze) {
        if (!(request.getData() instanceof BankPasswordRequest)) { send(sender, request, StatusCode.BAD_REQUEST, null); return; }
        BankPasswordRequest p = (BankPasswordRequest) request.getData();
        char[] password = p.getPassword();
        try { send(sender, request, StatusCode.SUCCESS, freeze ? bankService.freezeAccount(ownerUuid, password) : bankService.unfreezeAccount(ownerUuid, password)); }
        finally { java.util.Arrays.fill(password, '\0'); }
    }

    private void openAccount(Message request, MessageSender sender, String ownerUuid) {
        if (!(request.getData() instanceof BankOpenRequest)) {
            send(sender, request, StatusCode.BAD_REQUEST, null);
            return;
        }
        send(sender, request, StatusCode.SUCCESS, BankEnrollment.open(bankService,
                ownerUuid, request.getToken(), (BankOpenRequest) request.getData()));
    }

    private void queryAccount(Message request, MessageSender sender, String ownerUuid) {
        if (request.getData() != null) {
            send(sender, request, StatusCode.BAD_REQUEST, null);
            return;
        }
        send(sender, request, StatusCode.SUCCESS, bankService.queryAccount(ownerUuid));
    }

    private void recharge(Message request, MessageSender sender, String ownerUuid) {
        if (!(request.getData() instanceof BankRechargeRequest)) {
            send(sender, request, StatusCode.BAD_REQUEST, null);
            return;
        }
        BankRechargeRequest recharge = (BankRechargeRequest) request.getData();
        send(sender, request, StatusCode.SUCCESS,
                bankService.recharge(ownerUuid, recharge.getAmount()));
    }

    private void listTransactions(Message request, MessageSender sender, String ownerUuid) {
        if (request.getData() != null
                && !(request.getData() instanceof BankTransactionQueryRequest)) {
            send(sender, request, StatusCode.BAD_REQUEST, null);
            return;
        }
        BankTransactionQueryRequest query = request.getData() == null
                ? new BankTransactionQueryRequest()
                : (BankTransactionQueryRequest) request.getData();
        send(sender, request, StatusCode.SUCCESS, bankService.listTransactions(ownerUuid, query));
    }

    private static void send(MessageSender sender, Message request, String statusCode,
            Object data) {
        Message response = new Message(request.getCommand(), data);
        response.setUid(request.getUid());
        response.setStatusCode(statusCode);
        sender.send(response);
    }

    /**
     * 管理轨准入：必须已登录且角色为管理员，并且管理端服务已装配。
     *
     * @param request 请求
     * @param sender 响应发送器
     * @return 管理端服务；被拒时返回 null并已发送响应
     */
    private BankAdminService requireAdmin(Message request, MessageSender sender) {
        if (bankAdmin == null) {
            send(sender, request, StatusCode.INTERNAL_ERROR, null);
            return null;
        }
        SessionEntry current = auth.getSessionManager().validate(request.getToken());
        if (current == null) {
            send(sender, request, StatusCode.UNAUTHORIZED, null);
            return null;
        }
        if (Role.fromDisplayName(current.getRole()) != Role.ADMIN) {
            send(sender, request, StatusCode.FORBIDDEN, null);
            return null;
        }
        return bankAdmin;
    }

    /** 管理轨：分页列出全部账户。 */
    private void adminListAccounts(Message request, MessageSender sender) {
        BankAdminService admin = requireAdmin(request, sender);
        if (admin == null) {
            return;
        }
        Object data = request.getData();
        if (data != null && !(data instanceof BankAdminQuery)) {
            send(sender, request, StatusCode.BAD_REQUEST, null);
            return;
        }
        BankAdminQuery query = data == null ? new BankAdminQuery() : (BankAdminQuery) data;
        send(sender, request, StatusCode.SUCCESS, admin.listAccounts(query));
    }

    /** 管理轨：按用户名查看单个账户。 */
    private void adminQueryAccount(Message request, MessageSender sender) {
        BankAdminService admin = requireAdmin(request, sender);
        if (admin == null) {
            return;
        }
        if (!(request.getData() instanceof BankAdminRefRequest)) {
            send(sender, request, StatusCode.BAD_REQUEST, null);
            return;
        }
        BankAdminAccountView view = admin.viewAccount(
                ((BankAdminRefRequest) request.getData()).getUsername());
        send(sender, request,
                view == null ? StatusCode.NOT_FOUND : StatusCode.SUCCESS, view);
    }

    /** 管理轨：分页查询指定用户的流水。 */
    private void adminTransactions(Message request, MessageSender sender) {
        BankAdminService admin = requireAdmin(request, sender);
        if (admin == null) {
            return;
        }
        if (!(request.getData() instanceof BankAdminTransactionsRequest)) {
            send(sender, request, StatusCode.BAD_REQUEST, null);
            return;
        }
        BankAdminTransactionsRequest payload =
                (BankAdminTransactionsRequest) request.getData();
        BankTransactionListResponse result =
                admin.listTransactions(payload.getUsername(), payload.getQuery());
        send(sender, request,
                result == null ? StatusCode.NOT_FOUND : StatusCode.SUCCESS, result);
    }

    /** 管理轨：冻结或解冻指定账户。 */
    private void adminSetFrozen(Message request, MessageSender sender) {
        BankAdminService admin = requireAdmin(request, sender);
        if (admin == null) {
            return;
        }
        if (!(request.getData() instanceof BankAdminSetFrozenRequest)) {
            send(sender, request, StatusCode.BAD_REQUEST, null);
            return;
        }
        BankAdminSetFrozenRequest payload = (BankAdminSetFrozenRequest) request.getData();
        BankAdminAccountView view =
                admin.setFrozen(payload.getUsername(), payload.isFrozen());
        send(sender, request,
                view == null ? StatusCode.NOT_FOUND : StatusCode.SUCCESS, view);
    }

    /** 管理轨：重置指定用户的银行密码。 */
    private void adminResetPassword(Message request, MessageSender sender) {
        BankAdminService admin = requireAdmin(request, sender);
        if (admin == null) {
            return;
        }
        if (!(request.getData() instanceof BankAdminResetPasswordRequest)) {
            send(sender, request, StatusCode.BAD_REQUEST, null);
            return;
        }
        BankAdminResetPasswordRequest payload =
                (BankAdminResetPasswordRequest) request.getData();
        BankAdminAccountView view = admin.resetPassword(payload.getUsername(),
                payload.getSalt(), payload.getHash());
        send(sender, request,
                view == null ? StatusCode.NOT_FOUND : StatusCode.SUCCESS, view);
    }
}
