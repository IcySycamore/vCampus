package edu.seu.vcampus.server.bank;

import edu.seu.vcampus.common.bank.exception.BankAccountNotOpenedException;
import edu.seu.vcampus.common.bank.dto.BankRechargeRequest;
import edu.seu.vcampus.common.bank.dto.BankTransactionQueryRequest;
import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.handler.MessageHandler;
import edu.seu.vcampus.common.handler.MessageSender;
import edu.seu.vcampus.common.message.Message;

/**
 * 银行命令处理器：601 查询、602 充值、603 流水、604 独立开户。
 *
 * <p>token 的合法性由服务器会话层统一检查，本类通过
 * {@link BankIdentityResolver} 获取校验后的用户编号；没有可信身份时直接回 401。</p>
 */
public class BankMessageHandler implements MessageHandler {

    private final BankService bankService;
    private final BankIdentityResolver identityResolver;

    /**
     * 创建银行处理器。
     *
     * @param bankService 银行业务服务
     * @param identityResolver 认证身份解析器
     */
    public BankMessageHandler(BankService bankService,
            BankIdentityResolver identityResolver) {
        if (bankService == null) {
            throw new IllegalArgumentException("bankService must not be null");
        }
        if (identityResolver == null) {
            throw new IllegalArgumentException("identityResolver must not be null");
        }
        this.bankService = bankService;
        this.identityResolver = identityResolver;
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
            Long userId = identityResolver.resolveUserId(request);
            if (userId == null || userId <= 0) {
                send(sender, request, StatusCode.UNAUTHORIZED, null);
                return;
            }
            switch (request.getCommand()) {
                case Command.BANK_ACCOUNT_OPEN:
                    openAccount(request, sender, userId);
                    return;
                case Command.BANK_ACCOUNT_QUERY:
                    queryAccount(request, sender, userId);
                    return;
                case Command.BANK_RECHARGE:
                    recharge(request, sender, userId);
                    return;
                case Command.BANK_TRANSACTION_LIST:
                    listTransactions(request, sender, userId);
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

    private void openAccount(Message request, MessageSender sender, Long userId) {
        if (request.getData() != null) {
            send(sender, request, StatusCode.BAD_REQUEST, null);
            return;
        }
        send(sender, request, StatusCode.SUCCESS, bankService.openAccount(userId));
    }

    private void queryAccount(Message request, MessageSender sender, Long userId) {
        if (request.getData() != null) {
            send(sender, request, StatusCode.BAD_REQUEST, null);
            return;
        }
        send(sender, request, StatusCode.SUCCESS,
                bankService.queryAccount(userId));
    }

    private void recharge(Message request, MessageSender sender, Long userId) {
        if (!(request.getData() instanceof BankRechargeRequest)) {
            send(sender, request, StatusCode.BAD_REQUEST, null);
            return;
        }
        BankRechargeRequest recharge = (BankRechargeRequest) request.getData();
        send(sender, request, StatusCode.SUCCESS,
                bankService.recharge(userId, recharge.getAmount()));
    }

    private void listTransactions(Message request, MessageSender sender,
            Long userId) {
        if (request.getData() != null
                && !(request.getData() instanceof BankTransactionQueryRequest)) {
            send(sender, request, StatusCode.BAD_REQUEST, null);
            return;
        }
        BankTransactionQueryRequest query = request.getData() == null
                ? new BankTransactionQueryRequest()
                : (BankTransactionQueryRequest) request.getData();
        send(sender, request, StatusCode.SUCCESS,
                bankService.listTransactions(userId, query));
    }

    private static void send(MessageSender sender, Message request,
            String statusCode, Object data) {
        Message response = new Message(request.getCommand(), data);
        response.setUid(request.getUid());
        response.setStatusCode(statusCode);
        sender.send(response);
    }
}
