package edu.seu.vcampus.client.library;

import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.client.api.ApiErrors;
import edu.seu.vcampus.client.api.ClientApis;
import edu.seu.vcampus.client.network.ClientMessageDispatcher;
import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.message.MessageSender;
import edu.seu.vcampus.common.message.PageResponse;
import edu.seu.vcampus.common.library.dto.BookQuery;
import edu.seu.vcampus.common.library.dto.BookRef;
import edu.seu.vcampus.common.library.dto.BorrowRequest;
import edu.seu.vcampus.common.library.dto.RecordRef;
import edu.seu.vcampus.common.library.dto.ReservationRef;
import edu.seu.vcampus.common.library.entity.Book;
import edu.seu.vcampus.common.library.entity.BookReservation;
import edu.seu.vcampus.common.library.entity.BorrowRecord;
import edu.seu.vcampus.common.library.entity.LibraryAccount;
import edu.seu.vcampus.common.user.dto.LoginChallenge;
import edu.seu.vcampus.common.user.dto.LoginResponse;
import edu.seu.vcampus.common.user.entity.Role;
import edu.seu.vcampus.common.user.entity.SessionEntry;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** 验证图书馆确实复用用户 API 的会话、uid 分配与统一错误模型。 */
class LibraryServiceTest {
    private final ClientMessageDispatcher dispatcher = new ClientMessageDispatcher();
    private final ClientApis apis = ClientApis.create(dispatcher);
    private Message sent;
    private String token = "token-one";
    private String status = StatusCode.SUCCESS;
    private Object payload = Collections.emptyList();
    private boolean mismatchedUid;
    private boolean silent;

    @BeforeEach
    void loginThroughExistingUserModule() {
        dispatcher.bindSender(new MessageSender() {
            @Override
            public void send(Message request) {
                sent = request;
                if (silent) {
                    return;
                }
                Message reply = new Message(request.getCommand(), payload);
                reply.setUid(mismatchedUid ? request.getUid() + 1 : request.getUid());
                reply.setStatusCode(status);
                if (request.getCommand() == Command.USER_LOGIN) {
                    LoginChallenge challenge = new LoginChallenge();
                    challenge.m_salt = "salt";
                    challenge.m_nonce = "nonce";
                    reply.setData(challenge);
                } else if (request.getCommand() == Command.USER_LOGIN_VERIFY) {
                    LoginResponse result = new LoginResponse();
                    result.m_token = token;
                    result.m_session = new SessionEntry("uuid-001", "001", "学生", Long.MAX_VALUE);
                    reply.setData(result);
                }
                dispatcher.dispatch(reply);
            }
        });
        apis.user().login("001", Role.STUDENT, "secret");
    }

    @Test
    void sharesSessionAndSendsOnlyCurrentTokenWithDispatcherUid() {
        assertSame(apis.user().currentSession(), apis.library().currentSession());
        apis.library().listMyBorrows();
        assertNotNull(sent.getUid());
        assertEquals(token, sent.getToken());
        assertNull(sent.getSender());
        token = "token-two";
        apis.user().login("001", Role.STUDENT, "secret");
        apis.library().listMyBorrows();
        assertEquals("token-two", sent.getToken());
    }

    @Test
    void searchUsesQueryDtoAndValidatesPagedBooks() {
        Book book = new Book("9787302423287", "Java", "A", "C", 2, 1);
        payload = new PageResponse<Book>(Collections.singletonList(book), 21, 2, 20);
        BookQuery query = new BookQuery("Java", "title", 2, 20);

        PageResponse<Book> result = apis.library().searchBooks(query);

        assertSame(query, sent.getData());
        assertEquals(21, result.getTotal());
        assertEquals(2, result.getPageNumber());
        assertSame(book, result.getItems().get(0));
    }

    @Test
    void queriesLibraryAccountWithSharedSession() {
        LibraryAccount account = new LibraryAccount("uuid-001", 30, new java.util.Date());
        payload = account;

        assertSame(account, apis.library().queryMyAccount());
        assertEquals(Command.LIBRARY_ACCOUNT_QUERY, sent.getCommand());
        assertNull(sent.getData());
    }

    @Test
    void mutationRequestsUseExplicitDtos() {
        payload = new BorrowRecord();
        apis.library().borrowBook("9787302423287");
        assertEquals("9787302423287", ((BorrowRequest) sent.getData()).getIsbn());

        payload = new BorrowRecord();
        apis.library().returnBook(9L);
        assertEquals(9L, ((RecordRef) sent.getData()).getRecordId());

        payload = new BorrowRecord();
        apis.library().renewBook(10L);
        assertEquals(Command.LIBRARY_RENEW, sent.getCommand());
        assertEquals(10L, ((RecordRef) sent.getData()).getRecordId());

        payload = new BookReservation();
        apis.library().reserveBook("0321356683");
        assertEquals(Command.LIBRARY_RESERVE, sent.getCommand());
        assertEquals("0321356683", ((BookRef) sent.getData()).getIsbn());

        payload = new BookReservation();
        apis.library().cancelReservation(11L);
        assertEquals(Command.LIBRARY_CANCEL_RESERVATION, sent.getCommand());
        assertEquals(11L, ((ReservationRef) sent.getData()).getReservationId());

        payload = new BorrowRecord();
        apis.library().payFine(12L);
        assertEquals(Command.LIBRARY_PAY_FINE, sent.getCommand());
        assertEquals(12L, ((RecordRef) sent.getData()).getRecordId());

        payload = new Book("0321356683", "Java", "A", "C", 1, 1);
        apis.library().withdrawBook("0321356683");
        assertEquals("0321356683", ((BookRef) sent.getData()).getIsbn());
    }

    @Test
    void logoutImmediatelyInvalidatesLibraryWithoutAnotherCache() {
        apis.user().logout();
        assertNull(apis.library().currentSession());
        assertFalse(apis.library().isLoggedIn());
        assertEquals(StatusCode.UNAUTHORIZED, failure(apis.library()).getStatusCode());
        assertEquals(Command.USER_LOGOUT, sent.getCommand());
    }

    @Test
    void unauthorizedReplyInvalidatesTheSharedUserSession() {
        status = StatusCode.UNAUTHORIZED;
        assertEquals(StatusCode.UNAUTHORIZED, failure(apis.library()).getStatusCode());
        assertNull(apis.user().currentSession());
        assertNull(apis.user().currentToken());
    }

    @Test
    void connectionClosurePreventsFurtherLibraryRequests() {
        dispatcher.connectionClosed(null);
        assertEquals(StatusCode.UNAUTHORIZED, failure(apis.library()).getStatusCode());
    }

    @Test
    void rejectsMalformedPayloadAndMismatchedResponseUid() {
        payload = "not a list";
        assertEquals(ApiErrors.LOCAL_MALFORMED, failure(apis.library()).getStatusCode());
        payload = Collections.emptyList();
        mismatchedUid = true;
        assertEquals(ApiErrors.LOCAL_MALFORMED, failure(apis.library()).getStatusCode());
    }

    @Test
    void surfacesTimeoutInsteadOfLeavingThePagePendingForever() {
        silent = true;
        LibraryService shortTimeout = new LibraryService(dispatcher, apis.user(), 20);
        assertEquals(ApiErrors.LOCAL_TIMEOUT, failure(shortTimeout).getStatusCode());
    }

    private ApiException failure(final LibraryService service) {
        return assertThrows(ApiException.class, new Executable() {
            @Override
            public void execute() {
                service.listMyBorrows();
            }
        });
    }
}
