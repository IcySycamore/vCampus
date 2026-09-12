package edu.seu.vcampus.client.bank;

import edu.seu.vcampus.client.network.ClientSocket;
import edu.seu.vcampus.common.bank.dto.BankAccountResponse;
import edu.seu.vcampus.common.bank.entity.BankAccountStatus;
import edu.seu.vcampus.common.bank.entity.BankTransaction;
import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.message.Message;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 错误和连接生命周期相关的 Bank 客户端服务测试。 */
class DefaultBankClientServiceErrorTest {

    private static final String TOKEN = "token-1";

    @Test
    void errorResponseUsesFailureCallback() throws Exception {
        ClientSocket socket = connectedSocket();
        DefaultBankClientService service =
                new DefaultBankClientService(socket, TOKEN);
        Result<BankAccountResponse> result = new Result<BankAccountResponse>();

        service.queryAccount(result);

        Message request = sentRequest(socket);
        Object error = new Object();
        Message response = response(request, error);
        response.setStatusCode(Command.BANK_ACCOUNT_NOT_OPENED);
        service.handleMessage(response);

        assertNull(result.value);
        assertEquals(Command.BANK_ACCOUNT_NOT_OPENED, result.statusCode);
        assertEquals(error, result.errorData);
    }

    @Test
    void unmatchedResponseIsIgnored() throws Exception {
        ClientSocket socket = connectedSocket();
        DefaultBankClientService service =
                new DefaultBankClientService(socket, TOKEN);
        Result<BankAccountResponse> result = new Result<BankAccountResponse>();
        service.queryAccount(result);

        Message response = new Message(
                Command.BANK_ACCOUNT_QUERY,
                new BankAccountResponse(
                        "A-2", BigDecimal.ZERO, BankAccountStatus.NORMAL, null, null));
        response.setUid(999999L);
        response.setStatusCode(StatusCode.SUCCESS);
        service.handleMessage(response);

        assertNull(result.value);
        assertNull(result.statusCode);
    }

    @Test
    void invalidArgumentsAreRejected() {
        ClientSocket socket = mock(ClientSocket.class);
        when(socket.isConnected()).thenReturn(true);
        final DefaultBankClientService service =
                new DefaultBankClientService(socket, TOKEN);

        assertThrows(IllegalArgumentException.class,
                new Executable() {
                    @Override
                    public void execute() {
                        service.recharge(BigDecimal.ZERO,
                                new Result<BankTransaction>());
                    }
                });
        assertThrows(IllegalArgumentException.class,
                new Executable() {
                    @Override
                    public void execute() {
                        service.queryAccount(null);
                    }
                });
    }

    @Test
    void disconnectedSocketReportsFailure() {
        ClientSocket socket = mock(ClientSocket.class);
        when(socket.isConnected()).thenReturn(false);
        DefaultBankClientService service =
                new DefaultBankClientService(socket, TOKEN);
        Result<BankAccountResponse> result = new Result<BankAccountResponse>();

        service.queryAccount(result);

        assertEquals(StatusCode.INTERNAL_ERROR, result.statusCode);
        assertNotNull(result.errorData);
    }

    @Test
    void connectionClosedReportsFailureForPendingRequests() throws Exception {
        ClientSocket socket = connectedSocket();
        DefaultBankClientService service =
                new DefaultBankClientService(socket, TOKEN);
        Result<BankAccountResponse> result = new Result<BankAccountResponse>();
        service.queryAccount(result);

        service.connectionClosed(new Exception("closed"));

        assertEquals(StatusCode.INTERNAL_ERROR, result.statusCode);
        assertNotNull(result.errorData);
    }

    private static ClientSocket connectedSocket() {
        ClientSocket socket = mock(ClientSocket.class);
        when(socket.isConnected()).thenReturn(true);
        return socket;
    }

    private static Message sentRequest(ClientSocket socket) throws Exception {
        org.mockito.ArgumentCaptor<Message> captor =
                org.mockito.ArgumentCaptor.forClass(Message.class);
        org.mockito.Mockito.verify(socket).send(captor.capture());
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
