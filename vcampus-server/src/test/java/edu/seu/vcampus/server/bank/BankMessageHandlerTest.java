package edu.seu.vcampus.server.bank;

import edu.seu.vcampus.common.bank.exception.BankAccountNotOpenedException;
import edu.seu.vcampus.common.bank.dto.BankAccountResponse;
import edu.seu.vcampus.common.bank.dto.BankRechargeRequest;
import edu.seu.vcampus.common.bank.dto.BankRechargeResponse;
import edu.seu.vcampus.common.bank.dto.BankTransactionListResponse;
import edu.seu.vcampus.common.bank.dto.BankTransactionQueryRequest;
import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.handler.MessageSender;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.server.dispatch.MessageDispatcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

/** 真实分发器下的开户、查询、认证边界与协议响应测试。 */
class BankMessageHandlerTest {
    private final BankService bank = new BankService();
    private final BankIdentityResolver identity = mock(BankIdentityResolver.class);
    private final MessageDispatcher dispatcher = new MessageDispatcher();

    BankMessageHandlerTest() {
        when(identity.resolveUserId(any(Message.class))).thenReturn(101L);
        BankModule.register(dispatcher, bank, identity);
    }

    @Test
    void completeLifecycleThroughDispatcher() {
        assertNotOpened(request(Command.BANK_ACCOUNT_QUERY, null));
        assertNotOpened(request(Command.BANK_RECHARGE,
                new BankRechargeRequest(BigDecimal.ONE)));
        assertNotOpened(request(Command.BANK_TRANSACTION_LIST, null));
        BankAccountResponse opened = (BankAccountResponse)
                request(Command.BANK_ACCOUNT_OPEN, null).getData();
        BankRechargeResponse recharge = (BankRechargeResponse) request(Command.BANK_RECHARGE,
                new BankRechargeRequest(BigDecimal.TEN)).getData();
        assertEquals(BigDecimal.TEN, recharge.getAccount().getBalance());
        BankAccountResponse retried = (BankAccountResponse)
                request(Command.BANK_ACCOUNT_OPEN, null).getData();
        assertEquals(opened.getAccountId(), retried.getAccountId());
        assertEquals(opened.getCreatedAt(), retried.getCreatedAt());
        assertEquals(BigDecimal.TEN, retried.getBalance());
        BankTransactionListResponse ledger = (BankTransactionListResponse)
                request(Command.BANK_TRANSACTION_LIST, new BankTransactionQueryRequest()).getData();
        assertEquals(1, ledger.getTotalCount());
    }

    @Test
    void spoofedSenderCannotSelectSomeoneElsesAccount() {
        bank.openAccount(202L);
        bank.recharge(202L, BigDecimal.TEN);
        Message open = new Message(Command.BANK_ACCOUNT_OPEN, null);
        open.setToken("verified-by-resolver");
        open.setSender("202");
        dispatch(open);
        request(Command.BANK_RECHARGE, new BankRechargeRequest(BigDecimal.ONE));
        assertEquals(BigDecimal.ONE, bank.queryAccount(101L).getBalance());
        assertEquals(BigDecimal.TEN, bank.queryAccount(202L).getBalance());
    }

    @ParameterizedTest
    @ValueSource(ints = {601, 602, 603, 604})
    void invalidPayloadDoesNotOpenAccount(int command) {
        Message result = request(command, "client-chosen-user-id");
        assertEquals("400", result.getStatusCode());
        assertNull(result.getData());
        assertNotOpened(request(Command.BANK_ACCOUNT_QUERY, null));
    }

    @Test
    void missingTokenIsRejectedBeforeCallingIdentityProvider() {
        assertEquals("401", dispatch(new Message(Command.BANK_ACCOUNT_OPEN, null)).getStatusCode());
        verify(identity, never()).resolveUserId(any(Message.class));
    }

    @Test
    void blankTokenIsRejectedBeforeCallingIdentityProvider() {
        Message message = new Message(Command.BANK_ACCOUNT_OPEN, null);
        message.setToken("  ");
        assertEquals("401", dispatch(message).getStatusCode());
        verify(identity, never()).resolveUserId(any(Message.class));
    }

    @Test
    void missingOrNonPositiveIdentityIsUnauthorized() {
        for (Long userId : new Long[] {null, 0L, -1L}) {
            when(identity.resolveUserId(any(Message.class))).thenReturn(userId);
            assertEquals("401", request(Command.BANK_ACCOUNT_OPEN, null).getStatusCode());
        }
        when(identity.resolveUserId(any(Message.class))).thenReturn(101L);
        assertNotOpened(request(Command.BANK_ACCOUNT_QUERY, null));
    }

    @Test
    void distinguishesForbiddenAndInternalErrors() {
        when(identity.resolveUserId(any(Message.class)))
                .thenThrow(new IllegalStateException("forbidden"));
        assertEquals("403", request(Command.BANK_ACCOUNT_OPEN, null).getStatusCode());
        when(identity.resolveUserId(any(Message.class)))
                .thenReturn(101L);
        BankService broken = mock(BankService.class);
        when(broken.openAccount(101L)).thenThrow(new RuntimeException("unavailable"));
        BankModule.register(dispatcher, broken, identity);
        assertEquals("500", request(Command.BANK_ACCOUNT_OPEN, null).getStatusCode());
    }

    private void assertNotOpened(Message response) {
        assertEquals(Command.BANK_ACCOUNT_NOT_OPENED, response.getStatusCode());
        BankAccountNotOpenedException error = assertInstanceOf(
                BankAccountNotOpenedException.class, response.getData());
        assertEquals("银行账户未开户，请先开户", error.getMessage());
    }

    private Message request(int command, Object data) {
        Message message = new Message(command, data);
        message.setToken("verified-by-resolver");
        message.setUid(99L);
        return dispatch(message);
    }

    private Message dispatch(Message message) {
        final List<Message> sent = new ArrayList<Message>();
        dispatcher.dispatch(message, new MessageSender() {
            @Override
            public void send(Message response) {
                sent.add(response);
            }
        });
        assertEquals(1, sent.size());
        Message response = sent.get(0);
        assertEquals(message.getUid(), response.getUid());
        assertEquals(message.getCommand(), response.getCommand());
        return response;
    }
}
