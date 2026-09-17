package edu.seu.vcampus.server.library;

import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.library.entity.Book;
import edu.seu.vcampus.common.library.entity.BookReservation;
import edu.seu.vcampus.common.library.entity.BorrowRecord;
import edu.seu.vcampus.common.library.entity.LibraryAccount;
import edu.seu.vcampus.common.library.dto.BookQuery;
import edu.seu.vcampus.common.library.dto.BorrowRequest;
import edu.seu.vcampus.common.library.dto.RecordRef;
import edu.seu.vcampus.common.message.PageResponse;
import edu.seu.vcampus.common.message.MessageSender;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.network.MessageStream;
import edu.seu.vcampus.server.user.SessionManager;
import edu.seu.vcampus.server.network.ServerMessageDispatcher;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 真正 Socket、分发器和业务服务的借还流程；DAO/连接为测试替身，非真实数据库联调。 */
class LibraryBorrowFlowTest {
    private final List<BorrowRecord> records = new ArrayList<BorrowRecord>();
    private final Book book = new Book("9787302423287", "Java", "A", "C", 40, 40);
    private final Connection connection = mock(Connection.class);

    @ParameterizedTest
    @CsvSource({ "学生,30", "教师,30", "teacher,30" })
    void borrowToLimitReturnAndBorrowAgainOverSocket(String role, final int limit)
            throws Exception {
        SessionManager sessions = new SessionManager();
        final String token = sessions.create("001", "login-001", role);
        final ServerMessageDispatcher dispatcher = new ServerMessageDispatcher();
        LibraryModule.register(dispatcher, sessions, service());
        final ServerSocket listener = new ServerSocket(0);
        listener.setSoTimeout(5000);
        ExecutorService pool = Executors.newSingleThreadExecutor();
        Future<Void> peer = pool.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                try (Socket socket = listener.accept()) {
                    socket.setSoTimeout(5000);
                    final MessageStream stream = new MessageStream(socket);
                    for (int index = 0; index < limit + 8; index++) {
                        dispatcher.dispatch(stream.recvMessage(), new MessageSender() {
                            @Override
                            public void send(Message response) {
                                try {
                                    stream.writeMessage(response);
                                } catch (IOException exception) {
                                    throw new IllegalStateException(exception);
                                }
                            }
                        });
                    }
                }
                return null;
            }
        });
        try (Socket socket = new Socket("127.0.0.1", listener.getLocalPort())) {
            socket.setSoTimeout(5000);
            MessageStream stream = new MessageStream(socket);
            assertEquals(40, stock(stream, token));
            for (int index = 0; index < limit; index++) {
                assertEquals(StatusCode.SUCCESS, exchange(stream, token,
                        Command.LIBRARY_BORROW,
                        new BorrowRequest(String.format("978730000%04d", index)))
                                .getStatusCode());
            }
            assertEquals(StatusCode.BAD_REQUEST, exchange(stream, token,
                    Command.LIBRARY_BORROW, new BorrowRequest("9787302423294")).getStatusCode());
            assertEquals(limit, active(stream, token));
            assertEquals(StatusCode.SUCCESS, exchange(stream, token,
                    Command.LIBRARY_RETURN, new RecordRef(1L)).getStatusCode());
            assertEquals(limit - 1, active(stream, token));
            assertEquals(41 - limit, stock(stream, token));
            assertEquals(StatusCode.SUCCESS, exchange(stream, token,
                    Command.LIBRARY_BORROW, new BorrowRequest("9787302423294")).getStatusCode());
            assertEquals(40 - limit, stock(stream, token));
            peer.get(5, TimeUnit.SECONDS);
            verify(connection, times(limit + 2)).commit();
        } finally {
            listener.close();
            pool.shutdownNow();
        }
    }

    private LibraryService service() throws Exception {
        LibraryConnectionSource source = mock(LibraryConnectionSource.class);
        BookDao books = mock(BookDao.class);
        BorrowDao borrows = mock(BorrowDao.class);
        LibraryAccountDao accounts = mock(LibraryAccountDao.class);
        ReservationDao reservations = mock(ReservationDao.class);
        when(source.getConnection()).thenReturn(connection);
        when(accounts.findByUserUuid("001"))
                .thenReturn(new LibraryAccount("001", 30, new Date()));
        when(reservations.findExpiredReady(eq(connection), anyString(),
                any(Timestamp.class)))
                        .thenReturn(Collections.<BookReservation>emptyList());
        when(books.search(any(BookQuery.class))).thenReturn(new PageResponse<Book>(
                Collections.singletonList(book), 1, 1, 20));
        when(books.findByIsbn(eq(connection), anyString())).thenReturn(book);
        when(books.adjustAvailable(eq(connection), anyString(), anyInt()))
                .thenAnswer(new Answer<Boolean>() {
                    @Override
                    public Boolean answer(InvocationOnMock call) {
                        int change = call.getArgument(2);
                        book.setAvailableCopies(book.getAvailableCopies() + change);
                        return true;
                    }
                });
        when(borrows.findByUser("001")).thenReturn(records);
        when(borrows.insert(eq(connection), any(BorrowRecord.class)))
                .thenAnswer(new Answer<Long>() {
                    @Override
                    public Long answer(InvocationOnMock call) {
                        records.add((BorrowRecord) call.getArgument(1));
                        return (long) records.size();
                    }
                });
        when(borrows.findActiveById(connection, 1L)).thenAnswer(new Answer<BorrowRecord>() {
            @Override
            public BorrowRecord answer(InvocationOnMock call) {
                return records.get(0);
            }
        });
        when(borrows.markReturned(eq(connection), eq(1L), any(Timestamp.class),
                any(java.math.BigDecimal.class), eq(true))).thenReturn(true);
        return new LibraryService(source, accounts, books, borrows, reservations);
    }

    private int stock(MessageStream stream, String token) throws Exception {
        Message response = exchange(stream, token, Command.LIBRARY_SEARCH,
                new BookQuery("", "all", 1, 20));
        assertEquals(StatusCode.SUCCESS, response.getStatusCode());
        return ((Book) ((PageResponse<?>) response.getData()).getItems().get(0))
                .getAvailableCopies();
    }

    private int active(MessageStream stream, String token) throws Exception {
        Message response = exchange(stream, token, Command.LIBRARY_LIST_BORROWS, null);
        assertEquals(StatusCode.SUCCESS, response.getStatusCode());
        int count = 0;
        for (Object value : (List<?>) response.getData()) {
            if (!((BorrowRecord) value).isReturned()) {
                count++;
            }
        }
        return count;
    }

    private Message exchange(MessageStream stream, String token, int command, Object data)
            throws Exception {
        Message request = new Message(command, data);
        request.setToken(token);
        request.setSender("forged-user");
        request.setUid(99L);
        stream.writeMessage(request);
        Message response = stream.recvMessage();
        assertEquals(request.getUid(), response.getUid());
        assertEquals(command, response.getCommand());
        return response;
    }
}
