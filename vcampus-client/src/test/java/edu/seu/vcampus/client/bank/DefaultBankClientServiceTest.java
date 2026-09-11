package edu.seu.vcampus.client.bank;

import edu.seu.vcampus.client.network.ClientSocket;
import edu.seu.vcampus.common.bank.dto.BankAccountResponse;
import edu.seu.vcampus.common.bank.dto.BankRechargeRequest;
import edu.seu.vcampus.common.bank.dto.BankTransactionListResponse;
import edu.seu.vcampus.common.bank.dto.BankTransactionQueryRequest;
import edu.seu.vcampus.common.bank.entity.BankTransaction;
import edu.seu.vcampus.common.bank.entity.BankAccountStatus;
import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.message.Message;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link DefaultBankClientService} 的客户端协议适配测试。
 */
class DefaultBankClientServiceTest {

    private static final String TOKEN = "token-1";

    @Test
    void openAccountBuildsRequestAndDeliversResponse() throws Exception {
        ClientSocket socket = connectedSocket();
        final DefaultBankClientService service =
                new DefaultBankClientService(socket, TOKEN);
        Result<BankAccountResponse> result = new Result<BankAccountResponse>();

        service.openAccount(result);

        Message request = sentRequest(socket);
        assertEquals(Command.BANK_ACCOUNT_OPEN, request.getCommand());
        assertEquals(TOKEN, request.getToken());
        assertNull(request.getData());

        BankAccountResponse account = new BankAccountResponse(
                "A-1", BigDecimal.ZERO, BankAccountStatus.NORMAL, null, null);
        Message response = response(request, account);
        service.handleMessage(response);

        assertEquals(account, result.value);
        assertNull(result.statusCode);
    }

    @Test
    void queryAccountBuildsRequest() throws Exception {
        ClientSocket socket = connectedSocket();
        DefaultBankClientService service =
                new DefaultBankClientService(socket, TOKEN);
        Result<BankAccountResponse> result = new Result<BankAccountResponse>();

        service.queryAccount(result);

        Message request = sentRequest(socket);
        assertEquals(Command.BANK_ACCOUNT_QUERY, request.getCommand());
        assertEquals(TOKEN, request.getToken());
        assertNull(request.getData());
    }

    @Test
    void rechargeBuildsRechargeDto() throws Exception {
        ClientSocket socket = connectedSocket();
        DefaultBankClientService service =
                new DefaultBankClientService(socket, TOKEN);
        Result<BankTransaction> result = new Result<BankTransaction>();

        service.recharge(new BigDecimal("12.50"), result);

        Message request = sentRequest(socket);
        assertEquals(Command.BANK_RECHARGE, request.getCommand());
        assertEquals(TOKEN, request.getToken());
        assertNotNull(request.getData());
        assertEquals(new BigDecimal("12.50"),
                ((BankRechargeRequest) request.getData()).getAmount());
    }

    @Test
    void listTransactionsBuildsQueryDto() throws Exception {
        ClientSocket socket = connectedSocket();
        DefaultBankClientService service =
                new DefaultBankClientService(socket, TOKEN);
        Result<BankTransactionListResponse> result =
                new Result<BankTransactionListResponse>();
        BankTransactionQueryRequest query =
                new BankTransactionQueryRequest(2, 10);

        service.listTransactions(query, result);

        Message request = sentRequest(socket);
        assertEquals(Command.BANK_TRANSACTION_LIST, request.getCommand());
        assertEquals(TOKEN, request.getToken());
        assertEquals(query, request.getData());
    }

    private static ClientSocket connectedSocket() {
        ClientSocket socket = mock(ClientSocket.class);
        when(socket.isConnected()).thenReturn(true);
        return socket;
    }

    private static Message sentRequest(ClientSocket socket) throws Exception {
        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(socket).send(captor.capture());
        return captor.getValue();
    }

    private static Message response(Message request, Object data) {
        Message response = new Message(request.getCommand(), data);
        response.setUid(request.getUid());
        response.setStatusCode(StatusCode.SUCCESS);
        return response;
    }

    private static final class Result<T> implements BankClientCallback<T> {
        private T value;
        private String statusCode;
        private Object errorData;

        @Override
        public void onSuccess(T result) {
            value = result;
        }

        @Override
        public void onFailure(String code, Object data) {
            statusCode = code;
            errorData = data;
        }
    }
}
