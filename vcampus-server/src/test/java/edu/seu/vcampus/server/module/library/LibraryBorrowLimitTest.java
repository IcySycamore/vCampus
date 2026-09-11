package edu.seu.vcampus.server.module.library;

import edu.seu.vcampus.common.entity.BorrowRecord;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.message.MessageType;
import edu.seu.vcampus.server.auth.SessionManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 正式验证角色上限、历史记录计数、异常时禁止借书及真实会话身份。 */
class LibraryBorrowLimitTest {
    @ParameterizedTest
    @CsvSource({"学生,3,0", "学生,3,2", "学生,3,3", "学生,3,4",
        "教师,5,4", "教师,5,5", "教师,5,6", "管理员,10,9", "管理员,10,10"})
    void enforcesRoleLimitAndExcludesReturnedHistory(String role, int limit, int active)
            throws Exception {
        LibraryService service = mock(LibraryService.class);
        when(service.listBorrows("001")).thenReturn(records(active, 20));
        Message response = handler(service, role).handle(request());

        assertEquals(active < limit ? MessageType.SUCCESS : MessageType.BAD_REQUEST,
                response.getStatusCode());
        assertEquals(Long.valueOf(123L), response.getUid());
        assertEquals(MessageType.LIBRARY_BORROW, response.getCommand());
        verify(service).listBorrows("001");
        verify(service, times(active < limit ? 1 : 0)).borrow("001", "isbn");
        if (active >= limit) {
            assertTrue(response.getData().toString().contains(" " + limit + " "));
        }
    }

    @Test
    void failedCountQueryDoesNotBorrow() throws Exception {
        LibraryService service = mock(LibraryService.class);
        when(service.listBorrows("001")).thenThrow(new SQLException("unavailable"));
        assertEquals(MessageType.SERVER_ERROR,
                handler(service, "学生").handle(request()).getStatusCode());
        verify(service, never()).borrow(anyString(), anyString());
    }

    @Test
    void missingCountDataDoesNotBorrow() throws Exception {
        LibraryService service = mock(LibraryService.class);
        when(service.listBorrows("001")).thenReturn(null);
        assertEquals(MessageType.SERVER_ERROR,
                handler(service, "教师").handle(request()).getStatusCode());
        verify(service, never()).borrow(anyString(), anyString());
    }

    private String token;

    private LibraryMessageHandler handler(LibraryService service, String role) {
        SessionManager sessions = new SessionManager();
        token = sessions.create("uuid", "001", role);
        return new LibraryMessageHandler(service, sessions);
    }

    private Message request() {
        Message request = new Message(MessageType.LIBRARY_BORROW, "isbn");
        request.setToken(token);
        request.setSender("教师");
        request.setUid(123L);
        return request;
    }

    static List<BorrowRecord> records(int active, int returned) {
        List<BorrowRecord> records = new ArrayList<BorrowRecord>();
        for (int index = 0; index < active + returned; index++) {
            BorrowRecord record = new BorrowRecord("001", "isbn", "Java",
                    new Date(0), new Date(1));
            if (index >= active) {
                record.setReturnedAt(new Date());
            }
            records.add(record);
        }
        return records;
    }
}
