package edu.seu.vcampus.server.library;

import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.library.dto.BookQuery;
import edu.seu.vcampus.common.library.dto.BorrowRequest;
import edu.seu.vcampus.common.library.dto.RecordRef;
import edu.seu.vcampus.server.user.SessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

/** 所有错误参数在访问业务服务前拒绝，响应保留 uid/命令并提供中文说明。 */
class LibraryRequestValidationTest {
    private LibraryService service;
    private LibraryMessageHandler handler;
    private String token;

    @BeforeEach
    void setUp() {
        service = mock(LibraryService.class);
        SessionManager sessions = new SessionManager();
        token = sessions.create("001", "login-001", "学生");
        handler = new LibraryMessageHandler(service, sessions);
    }

    @ParameterizedTest
    @MethodSource("badSearches")
    void rejectsSearchParameters(Object data, String error) {
        assertBad(Command.LIBRARY_SEARCH, data, error);
    }

    static Object[][] badSearches() {
        return new Object[][] {
            {null, "BookQuery"}, {"Java", "BookQuery"},
            {new String[] {"Java", "all"}, "BookQuery"},
            {new BookQuery("Java", "category", 1, 20), "检索范围仅支持"},
            {new BookQuery("Java", "TITLE", 1, 20), "检索范围仅支持"},
            {new BookQuery(new String(new char[201]).replace('\0', 'a'), "all", 1, 20),
                "200 个字符"}
        };
    }

    @ParameterizedTest
    @MethodSource("badIsbns")
    void rejectsIsbnBeforeEvenCountingLoans(Object data, String error) {
        assertBad(Command.LIBRARY_BORROW, data, error);
    }

    static Object[][] badIsbns() {
        return new Object[][] {
            {null, "BorrowRequest"}, {9787302423287L, "BorrowRequest"},
            {"9787302423287", "BorrowRequest"},
            {new String[] {"9787302423287"}, "BorrowRequest"},
            {new BorrowRequest(null), "ISBN 不能为空"},
            {new BorrowRequest(""), "ISBN 不能为空"},
            {new BorrowRequest(" \t\n "), "ISBN 不能为空"},
            {new BorrowRequest("978-7"), "ISBN 格式不正确"},
            {new BorrowRequest("abcdefghij"), "ISBN 格式不正确"},
            {new BorrowRequest("978730242328"), "ISBN 格式不正确"},
            {new BorrowRequest("97873024232877"), "ISBN 格式不正确"},
            {new BorrowRequest("1234567890123"), "ISBN 格式不正确"},
            {new BorrowRequest("12345678X0"), "ISBN 格式不正确"},
            {new BorrowRequest("978730242328X"), "ISBN 格式不正确"},
            {new BorrowRequest("-9787302423287"), "ISBN 格式不正确"},
            {new BorrowRequest("978--7302423287"), "ISBN 格式不正确"},
            {new BorrowRequest("9787302423287-"), "ISBN 格式不正确"},
            {new BorrowRequest("978 7302423287"), "ISBN 格式不正确"},
            {new BorrowRequest("978730242\n3287"), "ISBN 格式不正确"},
            {new BorrowRequest("９７８７３０２４２３２８７"), "ISBN 格式不正确"}
        };
    }

    @ParameterizedTest
    @MethodSource("badRecordIds")
    void rejectsRecordIdsWithoutTruncationOrOverflow(Object data, String error) {
        assertBad(Command.LIBRARY_RETURN, data, error);
    }

    static Object[][] badRecordIds() {
        return new Object[][] {
            {null, "RecordRef"}, {"9", "RecordRef"}, {9L, "RecordRef"},
            {Boolean.TRUE, "RecordRef"}, {Double.NaN, "RecordRef"},
            {new RecordRef(0L), "必须大于 0"},
            {new RecordRef(-1L), "必须大于 0"},
            {new RecordRef(Long.MIN_VALUE), "必须大于 0"}
        };
    }

    @Test
    void authenticationStillComesBeforeParameterValidation() {
        Message request = request(Command.LIBRARY_BORROW, null);
        request.setToken("invalid");
        assertEquals(StatusCode.UNAUTHORIZED, handler.handle(request).getStatusCode());
        verifyNoInteractions(service);
    }

    @Test
    void rejectsNullRequestAndUnknownCommand() {
        Message response = handler.handle(null);
        assertEquals(StatusCode.BAD_REQUEST, response.getStatusCode());
        assertEquals("请求不能为空", response.getData());
        assertBad(-1, null, "未知的图书馆命令");
    }

    @Test
    void unexpectedServiceErrorDoesNotExposeJavaException() throws Exception {
        when(service.search(any(BookQuery.class))).thenThrow(
                new IllegalStateException("private detail"));
        Message response = handler.handle(request(Command.LIBRARY_SEARCH,
                new BookQuery("Java", "all", 1, 20)));
        assertEquals(StatusCode.INTERNAL_ERROR, response.getStatusCode());
        assertEquals("图书馆服务暂时不可用", response.getData());
    }

    private void assertBad(int command, Object data, String error) {
        Message response = handler.handle(request(command, data));
        assertEquals(StatusCode.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getData().toString().contains(error), response.getData().toString());
        assertEquals(Long.valueOf(99L), response.getUid());
        assertEquals(command, response.getCommand());
        verifyNoInteractions(service);
    }

    private Message request(int command, Object data) {
        Message request = new Message(command, data);
        request.setToken(token);
        request.setUid(99L);
        return request;
    }
}
