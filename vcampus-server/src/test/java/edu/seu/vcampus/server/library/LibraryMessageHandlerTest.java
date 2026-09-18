package edu.seu.vcampus.server.library;

import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.message.MessageSender;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.library.dto.BorrowRequest;
import edu.seu.vcampus.common.library.dto.RecordRef;
import edu.seu.vcampus.common.library.dto.ReservationRef;
import edu.seu.vcampus.common.library.dto.BookRef;
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
        handler.createResponse(request(Command.LIBRARY_BORROW,
                new BorrowRequest("978-7-302-42328-7")));
        handler.createResponse(request(Command.LIBRARY_RETURN, new RecordRef(9L)));

        verify(service).borrow("uuid-001", "978-7-302-42328-7");
        verify(service).returnBook("uuid-001", 9L);
    }

    @Test
    void routesRenewReservationAndFineCommands() throws Exception {
        LibraryFinePayment payment = mock(LibraryFinePayment.class);
        handler = new LibraryMessageHandler(service, sessions, payment);

        handler.createResponse(request(Command.LIBRARY_RENEW, new RecordRef(3L)));
        handler.createResponse(request(Command.LIBRARY_RESERVE, new BookRef("9787302423287")));
        handler.createResponse(request(Command.LIBRARY_LIST_RESERVATIONS, null));
        handler.createResponse(request(Command.LIBRARY_ACCOUNT_QUERY, null));
        handler.createResponse(request(Command.LIBRARY_CANCEL_RESERVATION,
                new ReservationRef(4L)));
        handler.createResponse(request(Command.LIBRARY_PAY_FINE, new RecordRef(5L)));

        verify(service).renew("uuid-001", 3L);
        verify(service).reserve("uuid-001", "9787302423287");
        verify(service).listReservations("uuid-001");
        verify(service).queryAccount("uuid-001");
        verify(service).cancelReservation("uuid-001", 4L);
        verify(service).payFine("uuid-001", 5L, payment);
    }

    @Test
    void missingOrInvalidTokenNeverReachesTheService() {
        for (String invalid : new String[] {null, "", "  ", "invalid-token"}) {
            Message request = request(Command.LIBRARY_SEARCH, new String[] {"Java", "all"});
            request.setToken(invalid);
            assertEquals(StatusCode.UNAUTHORIZED,
                    handler.createResponse(request).getStatusCode());
        }
        verifyNoInteractions(service);
    }

    @Test
    void loggedOutTokenCannotQueryBorrowRecords() {
        sessions.invalidate(token);
        Message response = handler.createResponse(
                request(Command.LIBRARY_LIST_BORROWS, null));

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
        assertEquals(StatusCode.UNAUTHORIZED, handler.createResponse(
                request(Command.LIBRARY_LIST_BORROWS, null)).getStatusCode());
        verifyNoInteractions(service);
    }
}
