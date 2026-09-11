package edu.seu.vcampus.common.bank.dto;

import edu.seu.vcampus.common.bank.exception.BankAccountNotOpenedException;
import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.message.Message;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

/** 未开户业务异常跨消息序列化后仍可识别，无需依赖 server 类。 */
class BankNotOpenedResponseTest {
    @Test
    void preservesBusinessExceptionAcrossObjectStream() throws Exception {
        Message response = new Message(Command.BANK_ACCOUNT_QUERY,
                new BankAccountNotOpenedException());
        response.setUid(123L);
        response.setStatusCode(Command.BANK_ACCOUNT_NOT_OPENED);
        Message received = BankDtoTestSupport.roundTrip(response);
        assertEquals("B100", received.getStatusCode());
        assertEquals(response.getUid(), received.getUid());
        assertEquals(response.getCommand(), received.getCommand());
        BankAccountNotOpenedException error = assertInstanceOf(
                BankAccountNotOpenedException.class, received.getData());
        assertEquals("银行账户未开户，请先开户", error.getMessage());
        assertEquals(0, error.getStackTrace().length);
        assertNull(error.getCause());
    }
}
