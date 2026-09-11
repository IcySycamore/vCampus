package edu.seu.vcampus.client.auth;

import edu.seu.vcampus.client.network.ClientSocket;
import edu.seu.vcampus.client.view.library.LibraryPanel;
import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.entity.BorrowRecord;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.message.MessageType;
import edu.seu.vcampus.common.user.dto.LoginChallenge;
import edu.seu.vcampus.common.user.dto.LoginResponse;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import javax.swing.SwingUtilities;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 模拟传输层登录和收发，保留真实 ClientSession 与 Swing 页面。 */
final class LibraryUiFixture implements AutoCloseable {
    final BlockingQueue<Message> sent = new LinkedBlockingQueue<Message>();
    final ClientSession session;
    LibraryPanel panel;
    private final AtomicLong ids = new AtomicLong();

    LibraryUiFixture(final String role) throws Exception {
        ClientSocket socket = mock(ClientSocket.class);
        session = new ClientSession(socket);
        when(socket.isConnected()).thenReturn(true);
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock call) {
                Message request = call.getArgument(0);
                request.setUid(ids.incrementAndGet());
                Message response = new Message(request.getCommand(), null);
                response.setStatusCode(MessageType.SUCCESS);
                if (request.getCommand() == Command.USER_LOGIN) {
                    LoginChallenge challenge = new LoginChallenge();
                    challenge.m_salt = "salt";
                    challenge.m_nonce = "nonce";
                    response.setData(challenge);
                } else if (request.getCommand() == Command.USER_LOGIN_VERIFY) {
                    LoginResponse login = new LoginResponse();
                    login.m_role = role;
                    login.m_token = "token";
                    response.setData(login);
                } else {
                    sent.add(request);
                    return null;
                }
                session.handleMessage(response);
                return null;
            }
        }).when(socket).send(any(Message.class));
        session.login("001", "secret".toCharArray(), "管理员");
        ui(new Runnable() {
            @Override
            public void run() {
                panel = new LibraryPanel();
                panel.attach(session);
                session.setHandler(panel);
            }
        });
    }

    Message query() throws Exception {
        ui(new Runnable() {
            @Override
            public void run() {
                panel.refresh();
            }
        });
        return take(MessageType.LIBRARY_LIST_BORROWS);
    }

    Message take(int command) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (true) {
            Message request = sent.poll(Math.max(0, deadline - System.nanoTime()),
                    TimeUnit.NANOSECONDS);
            assertNotNull(request, "missing command " + command);
            if (request.getCommand() == command) {
                return request;
            }
        }
    }

    void reply(Message request, String status, Object data) throws Exception {
        final Message response = new Message(request.getCommand(), data);
        response.setStatusCode(status);
        response.setUid(request.getUid());
        ui(new Runnable() {
            @Override
            public void run() {
                session.handleMessage(response);
            }
        });
    }

    static List<BorrowRecord> records(int active, int returned) {
        List<BorrowRecord> records = new ArrayList<BorrowRecord>();
        for (int index = 0; index < active + returned; index++) {
            BorrowRecord record = new BorrowRecord("001", "isbn", "Java", new Date(0), new Date(1));
            record.setId((long) index + 1);
            if (index >= active) {
                record.setReturnedAt(new Date());
            }
            records.add(record);
        }
        return records;
    }

    static Component find(Container parent, String name) {
        for (Component child : parent.getComponents()) {
            if (name.equals(child.getName())) {
                return child;
            }
            if (child instanceof Container) {
                Component found = find((Container) child, name);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    static void ui(Runnable action) throws Exception {
        SwingUtilities.invokeAndWait(action);
    }

    @Override
    public void close() throws Exception {
        session.close();
    }
}
