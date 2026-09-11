package edu.seu.vcampus.server.module.library;

import edu.seu.vcampus.common.entity.Book;
import edu.seu.vcampus.common.entity.BorrowRecord;
import edu.seu.vcampus.common.handler.MessageSender;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.message.MessageType;
import edu.seu.vcampus.common.network.MessageStream;
import edu.seu.vcampus.server.auth.SessionManager;
import edu.seu.vcampus.server.dispatch.MessageDispatcher;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
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
    private final Book book = new Book("isbn", "Java", "A", "C", 8, 8);
    private final Connection connection = mock(Connection.class);

    @ParameterizedTest
    @CsvSource({"学生,3", "教师,5"})
    void borrowToLimitReturnAndBorrowAgainOverSocket(String role, final int limit)
            throws Exception {
        SessionManager sessions = new SessionManager();
        final String token = sessions.create("uuid", "001", role);
        final MessageDispatcher dispatcher = new MessageDispatcher();
        LibraryMessageHandler.register(dispatcher, service(), sessions);
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
            assertEquals(8, stock(stream, token));
            for (int index = 0; index < limit; index++) {
                assertEquals(MessageType.SUCCESS, exchange(stream, token,
                        MessageType.LIBRARY_BORROW, "isbn" + index).getStatusCode());
            }
            assertEquals(MessageType.BAD_REQUEST, exchange(stream, token,
                    MessageType.LIBRARY_BORROW, "extra").getStatusCode());
            assertEquals(limit, active(stream, token));
            assertEquals(MessageType.SUCCESS, exchange(stream, token,
                    MessageType.LIBRARY_RETURN, Long.valueOf(1L)).getStatusCode());
            assertEquals(limit - 1, active(stream, token));
            assertEquals(9 - limit, stock(stream, token));
            assertEquals(MessageType.SUCCESS, exchange(stream, token,
                    MessageType.LIBRARY_BORROW, "extra").getStatusCode());
            assertEquals(8 - limit, stock(stream, token));
            peer.get(5, TimeUnit.SECONDS);
            verify(connection, times(limit + 2)).commit();
        } finally {
            listener.close();
            pool.shutdownNow();
        }
    }

    private LibraryService service() throws Exception {
        DataSource source = mock(DataSource.class);
        BookDao books = mock(BookDao.class);
        BorrowDao borrows = mock(BorrowDao.class);
        when(source.getConnection()).thenReturn(connection);
        when(books.search("", "all")).thenReturn(Collections.singletonList(book));
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
        when(borrows.findActiveById(connection, "001", 1L)).thenAnswer(new Answer<BorrowRecord>() {
            @Override
            public BorrowRecord answer(InvocationOnMock call) {
                return records.get(0);
            }
        });
        when(borrows.markReturned(eq(connection), eq(1L), any(Timestamp.class))).thenReturn(true);
        return new LibraryService(source, books, borrows);
    }

    private int stock(MessageStream stream, String token) throws Exception {
        Message response = exchange(stream, token, MessageType.LIBRARY_SEARCH,
                new String[] {"", "all"});
        assertEquals(MessageType.SUCCESS, response.getStatusCode());
        return ((Book) ((List<?>) response.getData()).get(0)).getAvailableCopies();
    }

    private int active(MessageStream stream, String token) throws Exception {
        Message response = exchange(stream, token, MessageType.LIBRARY_LIST_BORROWS, null);
        assertEquals(MessageType.SUCCESS, response.getStatusCode());
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
