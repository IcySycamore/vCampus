package edu.seu.vcampus.server.library;

import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.message.MessageSender;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.server.user.SessionManager;
import edu.seu.vcampus.server.network.ServerMessageDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/** 验证图书馆只使用真实会话身份，并能通过统一消息接口返回响应。 */
class LibraryMessageHandlerTest {
    private LibraryService service;
    private SessionManager sessions;
    private LibraryMessageHandler handler;
    private String token;

    @BeforeEach
    void setUp() {
        service = mock(LibraryService.class);
        sessions = new SessionManager();
        token = sessions.create("uuid-001", "001", "学生");
        handler = new LibraryMessageHandler(service, sessions);
    }

    @Test
    void listUsesTokenIdentityAndIgnoresForgedSender() throws Exception {
        ServerMessageDispatcher dispatcher = new ServerMessageDispatcher();
        dispatcher.register(Command.LIBRARY_LIST_BORROWS, handler);
        Message request = request(Command.LIBRARY_LIST_BORROWS, null);
        request.setUid(123L);
        MessageSender sender = mock(MessageSender.class);

        dispatcher.dispatch(request, sender);

        verify(service).listBorrows("uuid-001");
        ArgumentCaptor<Message> response = ArgumentCaptor.forClass(Message.class);
        verify(sender).send(response.capture());
        assertEquals(StatusCode.SUCCESS, response.getValue().getStatusCode());
        assertEquals(Long.valueOf(123L), response.getValue().getUid());
    }

    @Test
    void borrowAndReturnUseTheAuthenticatedUser() throws Exception {
        handler.handle(request(Command.LIBRARY_BORROW, "978-7-302-42328-7"));
        handler.handle(request(Command.LIBRARY_RETURN, Long.valueOf(9L)));

        verify(service).borrow("uuid-001", "978-7-302-42328-7");
        verify(service).returnBook("uuid-001", 9L);
    }

    @Test
    void missingOrInvalidTokenNeverReachesTheService() {
        for (String invalid : new String[] {null, "", "  ", "invalid-token"}) {
            Message request = request(Command.LIBRARY_SEARCH, new String[] {"Java", "all"});
            request.setToken(invalid);
            assertEquals(StatusCode.UNAUTHORIZED, handler.handle(request).getStatusCode());
        }
        verifyNoInteractions(service);
    }

    @Test
    void loggedOutTokenCannotQueryBorrowRecords() {
        sessions.invalidate(token);
        Message response = handler.handle(request(Command.LIBRARY_LIST_BORROWS, null));

        assertEquals(StatusCode.UNAUTHORIZED, response.getStatusCode());
        verifyNoInteractions(service);
    }

    private Message request(int command, Object data) {
        Message request = new Message(command, data);
        request.setToken(token);
        request.setSender("other-user");
        return request;
    }

    @Test
    void missingUuidCannotFallBackToTheLoginName() {
        token = sessions.create(null, "001", "学生");
        assertEquals(StatusCode.UNAUTHORIZED, handler.handle(
                request(Command.LIBRARY_LIST_BORROWS, null)).getStatusCode());
        verifyNoInteractions(service);
    }
}
