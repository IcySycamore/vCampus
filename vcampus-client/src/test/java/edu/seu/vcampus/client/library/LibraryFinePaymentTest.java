package edu.seu.vcampus.client.library;

import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.client.api.ClientApis;
import edu.seu.vcampus.client.network.ClientMessageDispatcher;
import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.library.dto.FinePaymentRequest;
import edu.seu.vcampus.common.library.entity.BorrowRecord;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.message.MessageSender;
import edu.seu.vcampus.common.user.dto.LoginChallenge;
import edu.seu.vcampus.common.user.dto.LoginResponse;
import edu.seu.vcampus.common.user.entity.Role;
import edu.seu.vcampus.common.user.entity.SessionEntry;
import java.math.BigDecimal;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** 缴纳滞纳金的客户端实例测试：登录后发送 415 并校验请求与响应。 */
class LibraryFinePaymentTest {
    private final ClientMessageDispatcher dispatcher = new ClientMessageDispatcher();
    private final ClientApis apis = ClientApis.create(dispatcher);
    private Message sent;
    private String token = "fine-token";
    private String fineStatus = StatusCode.SUCCESS;
    private Object fineData = paidRecord();

    @BeforeEach
    void loginThroughExistingUserModule() {
        dispatcher.bindSender(new MessageSender() {
            @Override
            public void send(Message request) {
                sent = request;
                Message reply = new Message(request.getCommand(), data(request));
                reply.setUid(request.getUid());
                reply.setStatusCode(status(request));
                dispatcher.dispatch(reply);
            }
        });
        apis.user().login("001", Role.STUDENT, "secret");
    }

    @Test
    void paysFineAndReceivesPaidRecord() {
        BorrowRecord result = apis.library().payFine(12L, "pass123".toCharArray());

        assertSame(fineData, result);
        assertEquals(Command.LIBRARY_PAY_FINE, sent.getCommand());
        assertEquals(12L, ((FinePaymentRequest) sent.getData()).getRecordId());
        assertEquals(token, sent.getToken());
        assertNull(sent.getSender());
    }

    @Test
    void surfacesServerRejectedFine() {
        fineStatus = StatusCode.BAD_REQUEST;
        fineData = "银行账户余额不足，请先充值";

        ApiException error = assertThrows(ApiException.class, new Executable() {
            @Override
            public void execute() {
                apis.library().payFine(12L, "pass123".toCharArray());
            }
        });

        assertEquals(StatusCode.BAD_REQUEST, error.getStatusCode());
        assertEquals("银行账户余额不足，请先充值", error.getMessage());
    }

    private Object data(Message request) {
        if (request.getCommand() == Command.USER_LOGIN) {
            LoginChallenge challenge = new LoginChallenge();
            challenge.m_salt = "salt";
            challenge.m_nonce = "nonce";
            return challenge;
        }
        if (request.getCommand() == Command.USER_LOGIN_VERIFY) {
            LoginResponse result = new LoginResponse();
            result.m_token = token;
            result.m_session = new SessionEntry("uuid-001", "001", "学生", Long.MAX_VALUE);
            return result;
        }
        return fineData;
    }

    private String status(Message request) {
        if (request.getCommand() == Command.USER_LOGIN
                || request.getCommand() == Command.USER_LOGIN_VERIFY) {
            return StatusCode.SUCCESS;
        }
        return fineStatus;
    }

    private BorrowRecord paidRecord() {
        BorrowRecord record = new BorrowRecord("uuid-001", "9787302423287", "Java",
                new Date(0), new Date(1));
        record.setId(12L);
        record.setReturnedAt(new Date(2));
        record.setFineAmount(new BigDecimal("1.50"));
        record.setFinePaid(true);
        record.setFineTransactionId("bank-flow-123");
        return record;
    }
}
