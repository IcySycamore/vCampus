package edu.seu.vcampus.server.library;

import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.server.user.SessionManager;
import java.math.BigDecimal;
import java.math.BigInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
            {null, "字符串数组"}, {"Java", "字符串数组"},
            {new Object[] {"Java", "all"}, "字符串数组"},
            {new String[0], "最多两项"}, {new String[] {"a", "all", "extra"}, "最多两项"},
            {new String[] {"Java", "isbn"}, "检索范围仅支持"},
            {new String[] {"Java", "TITLE"}, "检索范围仅支持"},
            {new String[] {new String(new char[201]).replace('\0', 'a')}, "200 个字符"}
        };
    }

    @ParameterizedTest
    @MethodSource("badIsbns")
    void rejectsIsbnBeforeEvenCountingLoans(Object data, String error) {
        assertBad(Command.LIBRARY_BORROW, data, error);
    }

    static Object[][] badIsbns() {
        return new Object[][] {
            {null, "ISBN 必须是字符串"}, {9787302423287L, "ISBN 必须是字符串"},
            {new String[] {"9787302423287"}, "ISBN 必须是字符串"},
            {"", "ISBN 不能为空"}, {" \t\n ", "ISBN 不能为空"},
            {"978-7", "ISBN 格式不正确"}, {"abcdefghij", "ISBN 格式不正确"},
            {"978730242328", "ISBN 格式不正确"}, {"97873024232877", "ISBN 格式不正确"},
            {"1234567890123", "ISBN 格式不正确"}, {"12345678X0", "ISBN 格式不正确"},
            {"978730242328X", "ISBN 格式不正确"}, {"-9787302423287", "ISBN 格式不正确"},
            {"978--7302423287", "ISBN 格式不正确"}, {"9787302423287-", "ISBN 格式不正确"},
            {"978 7302423287", "ISBN 格式不正确"}, {"978730242\n3287", "ISBN 格式不正确"},
            {"９７８７３０２４２３２８７", "ISBN 格式不正确"}
        };
    }

    @ParameterizedTest
    @MethodSource("badRecordIds")
    void rejectsRecordIdsWithoutTruncationOrOverflow(Object data, String error) {
        assertBad(Command.LIBRARY_RETURN, data, error);
    }

    static Object[][] badRecordIds() {
        return new Object[][] {
            {null, "记录号不能为空"}, {"9", "64 位范围内的整数"},
            {"", "64 位范围内的整数"}, {Boolean.TRUE, "64 位范围内的整数"},
            {0L, "必须大于 0"}, {-1L, "必须大于 0"}, {Long.MIN_VALUE, "必须大于 0"},
            {1.5D, "64 位范围内的整数"}, {9.0F, "64 位范围内的整数"},
            {Double.NaN, "64 位范围内的整数"},
            {Double.POSITIVE_INFINITY, "64 位范围内的整数"},
            {new BigInteger("9223372036854775808"), "64 位范围内的整数"},
            {new BigDecimal("9.1"), "64 位范围内的整数"}
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
        when(service.search("Java", "all")).thenThrow(new IllegalStateException("private detail"));
        Message response = handler.handle(request(Command.LIBRARY_SEARCH,
                new String[] {"Java"}));
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
