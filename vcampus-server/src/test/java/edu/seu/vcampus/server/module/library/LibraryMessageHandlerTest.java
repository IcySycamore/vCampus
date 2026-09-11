package edu.seu.vcampus.server.module.library;

import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.handler.MessageSender;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.message.MessageType;
import edu.seu.vcampus.server.auth.SessionManager;
import edu.seu.vcampus.server.dispatch.MessageDispatcher;
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
        MessageDispatcher dispatcher = new MessageDispatcher();
        dispatcher.register(MessageType.LIBRARY_LIST_BORROWS, handler);
        Message request = request(MessageType.LIBRARY_LIST_BORROWS, null);
        request.setUid(123L);
        MessageSender sender = mock(MessageSender.class);

        dispatcher.dispatch(request, sender);

        verify(service).listBorrows("001");
        ArgumentCaptor<Message> response = ArgumentCaptor.forClass(Message.class);
        verify(sender).send(response.capture());
        assertEquals(StatusCode.SUCCESS, response.getValue().getStatusCode());
        assertEquals(Long.valueOf(123L), response.getValue().getUid());
    }

    @Test
    void borrowAndReturnUseTheAuthenticatedUser() throws Exception {
        handler.handle(request(MessageType.LIBRARY_BORROW, "978-7"));
        handler.handle(request(MessageType.LIBRARY_RETURN, Long.valueOf(9L)));

        verify(service).borrow("001", "978-7");
        verify(service).returnBook("001", 9L);
    }

    @Test
    void missingOrInvalidTokenNeverReachesTheService() {
        for (String invalid : new String[] {null, "", "  ", "invalid-token"}) {
            Message request = request(MessageType.LIBRARY_SEARCH, new String[] {"Java", "all"});
            request.setToken(invalid);
            assertEquals(StatusCode.UNAUTHORIZED, handler.handle(request).getStatusCode());
        }
        verifyNoInteractions(service);
    }

    @Test
    void loggedOutTokenCannotQueryBorrowRecords() {
        sessions.invalidate(token);
        Message response = handler.handle(request(MessageType.LIBRARY_LIST_BORROWS, null));

        assertEquals(StatusCode.UNAUTHORIZED, response.getStatusCode());
        verifyNoInteractions(service);
    }

    private Message request(int command, Object data) {
        Message request = new Message(command, data);
        request.setToken(token);
        request.setSender("other-user");
        return request;
    }
}
