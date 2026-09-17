package edu.seu.vcampus.client.bank;

import edu.seu.vcampus.client.api.ApiErrors;
import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.client.network.ClientMessageDispatcher;
import edu.seu.vcampus.client.user.UserService;
import edu.seu.vcampus.common.bank.dto.BankAccountResponse;
import edu.seu.vcampus.common.bank.entity.BankAccountStatus;
import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.user.entity.SessionEntry;
import edu.seu.vcampus.common.user.dto.LoginChallenge;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 银行同步 API 的共享会话、错误和协议测试。 */
class BankServiceTest {
    @Test
    void readsCurrentSharedTokenOnEachRequest() {
        FakeDispatcher dispatcher = new FakeDispatcher();
        UserService users = mock(UserService.class);
        when(users.currentToken()).thenReturn("first", "second");
        final BankService bank = new BankService(dispatcher, users);
        assertEquals("account", bank.queryMyAccount().getAccountId());
        assertEquals("first", dispatcher.last.getToken());
        assertEquals(Command.BANK_ACCOUNT_QUERY, dispatcher.last.getCommand());
        assertNull(dispatcher.last.getData());
        bank.openAccount();
        assertEquals("second", dispatcher.last.getToken());
        assertEquals(Command.BANK_ACCOUNT_OPEN, dispatcher.last.getCommand());
    }

    @Test
    void unauthorizedInvalidatesSharedSession() {
        FakeDispatcher dispatcher = new FakeDispatcher();
        dispatcher.code = StatusCode.UNAUTHORIZED;
        UserService users = mock(UserService.class);
        when(users.currentToken()).thenReturn("token");
        final BankService bank = new BankService(dispatcher, users);
        assertEquals(StatusCode.UNAUTHORIZED,
                assertThrows(ApiException.class, new org.junit.jupiter.api.function.Executable() {
                    @Override public void execute() { bank.queryMyAccount(); }
                }).getStatusCode());
        verify(users).connectionClosed(null);
    }

    @Test
    void distinguishesMissingAccountTimeoutAndMalformedResponse() {
        FakeDispatcher dispatcher = new FakeDispatcher();
        UserService users = mock(UserService.class);
        when(users.currentToken()).thenReturn("token");
        final BankService bank = new BankService(dispatcher, users);
        dispatcher.code = Command.BANK_ACCOUNT_NOT_OPENED;
        assertEquals(Command.BANK_ACCOUNT_NOT_OPENED,
                assertThrows(ApiException.class, new org.junit.jupiter.api.function.Executable() {
                    @Override public void execute() { bank.queryMyAccount(); }
                }).getStatusCode());
        dispatcher.timeout = true;
        assertEquals(ApiErrors.LOCAL_TIMEOUT,
                assertThrows(ApiException.class, new org.junit.jupiter.api.function.Executable() {
                    @Override public void execute() { bank.queryMyAccount(); }
                }).getStatusCode());
        dispatcher.timeout = false;
        dispatcher.code = StatusCode.SUCCESS;
        dispatcher.wrongUid = true;
        assertEquals(ApiErrors.LOCAL_MALFORMED,
                assertThrows(ApiException.class, new org.junit.jupiter.api.function.Executable() {
                    @Override public void execute() { bank.queryMyAccount(); }
                }).getStatusCode());
    }

    @Test
    void changesPasswordThroughDedicatedCampusVerificationRequests() {
        FakeDispatcher dispatcher = new FakeDispatcher();
        dispatcher.passwordFlow = true;
        UserService users = mock(UserService.class);
        when(users.currentToken()).thenReturn("primary-token");
        when(users.currentSession()).thenReturn(new SessionEntry("uuid", "student", "学生",
                "学生", Long.MAX_VALUE));
        BankService bank = new BankService(dispatcher, users);
        BankAccountResponse result = bank.changePassword("campus".toCharArray(),
                "bank-old".toCharArray(), "bank-new".toCharArray());
        assertEquals("account", result.getAccountId());
        assertEquals(Command.BANK_PASSWORD_CHANGE, dispatcher.last.getCommand());
        assertEquals("student", ((edu.seu.vcampus.common.bank.dto.BankPasswordChangeRequest)
                dispatcher.last.getData()).getUsername());
    }

    private static final class FakeDispatcher extends ClientMessageDispatcher {
        private Message last;
        private String code = StatusCode.SUCCESS;
        private boolean timeout;
        private boolean wrongUid;
        private boolean passwordFlow;

        @Override
        public Message request(Message request, long timeoutMillis) {
            last = request;
            request.setUid(10L);
            if (timeout) {
                return null;
            }
            Message response = new Message(request.getCommand(), new BankAccountResponse(
                    "account", BigDecimal.ZERO, BankAccountStatus.NORMAL, null, null));
            if (passwordFlow && request.getCommand() == Command.BANK_PASSWORD_VERIFY_CHALLENGE) {
                LoginChallenge challenge = new LoginChallenge();
                challenge.m_salt = "salt";
                challenge.m_nonce = "nonce";
                response.setData(challenge);
            } else if (passwordFlow && request.getCommand() == Command.BANK_PASSWORD_VERIFY) {
                response.setData("verification-token");
            }
            response.setUid(wrongUid ? 11L : 10L);
            response.setStatusCode(code);
            return response;
        }
    }
}
