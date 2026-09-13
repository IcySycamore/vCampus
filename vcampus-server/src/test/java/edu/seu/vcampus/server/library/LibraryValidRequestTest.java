package edu.seu.vcampus.server.library;

import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.library.dto.BookQuery;
import edu.seu.vcampus.common.library.dto.BorrowRequest;
import edu.seu.vcampus.common.library.dto.RecordRef;
import edu.seu.vcampus.server.user.SessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import org.mockito.ArgumentCaptor;

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
    void acceptsSearchAndNormalizesWithoutChangingInput(BookQuery data,
            String keyword, String field)
            throws Exception {
        String originalKeyword = data.getKeyword();
        String originalField = data.getField();
        assertEquals(StatusCode.SUCCESS,
                response(Command.LIBRARY_SEARCH, data).getStatusCode());
        ArgumentCaptor<BookQuery> query = ArgumentCaptor.forClass(BookQuery.class);
        verify(service).search(query.capture());
        assertEquals(keyword, query.getValue().getKeyword());
        assertEquals(field, query.getValue().getField());
        assertEquals(data.getPageNumber(), query.getValue().getPageNumber());
        assertEquals(data.getPageSize(), query.getValue().getPageSize());
        assertEquals(originalKeyword, data.getKeyword());
        assertEquals(originalField, data.getField());
    }

    static Object[][] searches() {
        String boundary = new String(new char[200]).replace('\0', 'a');
        return new Object[][] {
            {new BookQuery(" Java ", null, 1, 20), "Java", "all"},
            {new BookQuery(null, null, 1, 20), "", "all"},
            {new BookQuery("  ", "  ", 1, 20), "", "all"},
            {new BookQuery("书", " title ", 1, 20), "书", "title"},
            {new BookQuery("人", "author", 1, 20), "人", "author"},
            {new BookQuery("978", "isbn", 1, 20), "978", "isbn"},
            {new BookQuery(boundary, "all", 2, 50), boundary, "all"}
        };
    }

    @ParameterizedTest
    @ValueSource(strings = {"9787302423287", "978-7-302-42328-7", "0321356683", "080442957X",
        "0-8044-2957-x", " 9787302423287 ", "9791234567896"})
    void acceptsIsbnFormatsAndPreservesTheDatabaseKey(String isbn) throws Exception {
        assertEquals(StatusCode.SUCCESS,
                response(Command.LIBRARY_BORROW, new BorrowRequest(isbn)).getStatusCode());
        verify(service).borrow("001", isbn.trim());
    }

    @ParameterizedTest
    @ValueSource(longs = {1L, Long.MAX_VALUE})
    void acceptsPositiveRecordIds(long id) throws Exception {
        assertEquals(StatusCode.SUCCESS,
                response(Command.LIBRARY_RETURN, new RecordRef(id)).getStatusCode());
        verify(service).returnBook("001", id);
    }

    private Message response(int command, Object data) {
        Message request = new Message(command, data);
        request.setToken(token);
        return handler.handle(request);
    }
}
