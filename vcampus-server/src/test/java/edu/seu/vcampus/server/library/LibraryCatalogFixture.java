package edu.seu.vcampus.server.library;

import edu.seu.vcampus.common.library.entity.Book;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.server.user.SessionManager;
import edu.seu.vcampus.server.network.ServerMessageDispatcher;
import edu.seu.vcampus.common.message.MessageSender;
import java.sql.Connection;
import javax.sql.DataSource;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 管理消息经过实际分发器和服务，数据库接口使用模拟对象。 */
final class LibraryCatalogFixture {
    static final String ISBN = "9787302423287";
    final DataSource source = mock(DataSource.class);
    final Connection connection = mock(Connection.class);
    final BookDao books = mock(BookDao.class);
    final BorrowDao borrows = mock(BorrowDao.class);
    final SessionManager sessions = new SessionManager();
    final ServerMessageDispatcher dispatcher = new ServerMessageDispatcher();

    LibraryCatalogFixture() throws Exception {
        when(source.getConnection()).thenReturn(connection);
        LibraryMessageHandler.register(dispatcher,
                new LibraryService(source, books, borrows), sessions);
    }

    Message send(int command, Object data, String role) {
        Message request = new Message(command, data);
        request.setToken(sessions.create("001", "login-001", role));
        request.setSender("管理员");
        request.setUid(88L);
        final Message[] result = new Message[1];
        dispatcher.dispatch(request, new MessageSender() {
            @Override
            public void send(Message response) {
                if (result[0] != null) {
                    throw new AssertionError("duplicate response");
                }
                result[0] = response;
            }
        });
        return result[0];
    }

    static Book book(int total, int available) {
        return new Book(ISBN, " Java ", " Author ", " 计算机 ", total, available);
    }
}
