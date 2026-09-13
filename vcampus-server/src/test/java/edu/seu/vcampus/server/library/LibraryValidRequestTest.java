package edu.seu.vcampus.server.library;

import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.server.user.SessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/** 合法参数的边界、默认检索范围及 ISBN 原始馆藏键保留。 */
class LibraryValidRequestTest {
    private LibraryService service;
    private LibraryMessageHandler handler;
    private String token;

    @BeforeEach
    void setUp() {
        service = mock(LibraryService.class);
        SessionManager sessions = new SessionManager();
        token = sessions.create("001", "login-001", "教师");
        handler = new LibraryMessageHandler(service, sessions);
    }

    @ParameterizedTest
    @MethodSource("searches")
    void acceptsSearchAndNormalizesWithoutChangingInput(String[] data, String keyword, String field)
            throws Exception {
        String[] original = data.clone();
        assertEquals(StatusCode.SUCCESS,
                response(Command.LIBRARY_SEARCH, data).getStatusCode());
        verify(service).search(keyword, field);
        org.junit.jupiter.api.Assertions.assertArrayEquals(original, data);
    }

    static Object[][] searches() {
        String boundary = new String(new char[200]).replace('\0', 'a');
        return new Object[][] {
            {new String[] {" Java "}, "Java", "all"},
            {new String[] {null, null}, "", "all"},
            {new String[] {"  ", "  "}, "", "all"},
            {new String[] {"书", " title "}, "书", "title"},
            {new String[] {"人", "author"}, "人", "author"},
            {new String[] {"计算机", "category"}, "计算机", "category"},
            {new String[] {boundary, "all"}, boundary, "all"}
        };
    }

    @ParameterizedTest
    @ValueSource(strings = {"9787302423287", "978-7-302-42328-7", "0321356683", "080442957X",
        "0-8044-2957-x", " 9787302423287 ", "9791234567896"})
    void acceptsIsbnFormatsAndPreservesTheDatabaseKey(String isbn) throws Exception {
        assertEquals(StatusCode.SUCCESS,
                response(Command.LIBRARY_BORROW, isbn).getStatusCode());
        verify(service).borrow("001", isbn.trim());
    }

    @ParameterizedTest
    @MethodSource("recordIds")
    void acceptsPositiveIntegralTypes(Number id) throws Exception {
        assertEquals(StatusCode.SUCCESS, response(Command.LIBRARY_RETURN, id).getStatusCode());
        verify(service).returnBook("001", id.longValue());
    }

    static Object[] recordIds() {
        return new Object[] {Byte.valueOf((byte) 1), Short.valueOf((short) 2),
            Integer.valueOf(3), Long.valueOf(4), Long.valueOf(Long.MAX_VALUE)};
    }

    private Message response(int command, Object data) {
        Message request = new Message(command, data);
        request.setToken(token);
        return handler.handle(request);
    }
}
