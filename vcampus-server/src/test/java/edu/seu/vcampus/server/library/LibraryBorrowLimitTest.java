package edu.seu.vcampus.server.library;

import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.library.entity.BorrowRecord;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.server.user.SessionManager;
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
    @CsvSource({"student,3,3", "STUDENT,3,2", "Student,3,4", "学生,3,0", "学生,3,2", "学生,3,3", "学生,3,4",
        "教师,5,4", "教师,5,5", "教师,5,6", "管理员,10,9", "管理员,10,10",
        "teacher,5,4", "teacher,5,5", "teacher,5,6", "TEACHER,5,5", "Teacher,5,4"})
    void enforcesRoleLimitAndExcludesReturnedHistory(String role, int limit, int active)
            throws Exception {
        LibraryService service = mock(LibraryService.class);
        when(service.listBorrows("001")).thenReturn(records(active, 20));
        Message response = handler(service, role).handle(request());

        assertEquals(active < limit ? StatusCode.SUCCESS : StatusCode.BAD_REQUEST,
                response.getStatusCode());
        assertEquals(Long.valueOf(123L), response.getUid());
        assertEquals(Command.LIBRARY_BORROW, response.getCommand());
        verify(service).listBorrows("001");
        verify(service, times(active < limit ? 1 : 0)).borrow("001", "9787302423287");
        if (active >= limit) {
            assertTrue(response.getData().toString().contains(" " + limit + " "));
        }
    }

    @Test
    void failedCountQueryDoesNotBorrow() throws Exception {
        LibraryService service = mock(LibraryService.class);
        when(service.listBorrows("001")).thenThrow(new SQLException("unavailable"));
        assertEquals(StatusCode.INTERNAL_ERROR,
                handler(service, "学生").handle(request()).getStatusCode());
        verify(service, never()).borrow(anyString(), anyString());
    }

    @Test
    void missingCountDataDoesNotBorrow() throws Exception {
        LibraryService service = mock(LibraryService.class);
        when(service.listBorrows("001")).thenReturn(null);
        assertEquals(StatusCode.INTERNAL_ERROR,
                handler(service, "教师").handle(request()).getStatusCode());
        verify(service, never()).borrow(anyString(), anyString());
    }

    private String token;

    private LibraryMessageHandler handler(LibraryService service, String role) {
        SessionManager sessions = new SessionManager();
        token = sessions.create("001", "login-001", role);
        return new LibraryMessageHandler(service, sessions);
    }

    private Message request() {
        Message request = new Message(Command.LIBRARY_BORROW, "9787302423287");
        request.setToken(token);
        request.setSender("教师");
        request.setUid(123L);
        return request;
    }

    static List<BorrowRecord> records(int active, int returned) {
        List<BorrowRecord> records = new ArrayList<BorrowRecord>();
        for (int index = 0; index < active + returned; index++) {
            BorrowRecord record = new BorrowRecord("001", "9787302423287", "Java",
                    new Date(0), new Date(1));
            if (index >= active) {
                record.setReturnedAt(new Date());
            }
            records.add(record);
        }
        return records;
    }
}
