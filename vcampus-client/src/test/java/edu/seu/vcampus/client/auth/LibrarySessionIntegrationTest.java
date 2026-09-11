package edu.seu.vcampus.client.auth;

import edu.seu.vcampus.client.handler.UIUpdateHandler;
import edu.seu.vcampus.client.view.shell.MainContentPanel;
import edu.seu.vcampus.client.view.shell.PageNames;
import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.entity.Book;
import edu.seu.vcampus.common.entity.BorrowRecord;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.message.MessageType;
import edu.seu.vcampus.common.network.MessageStream;
import edu.seu.vcampus.common.user.dto.LoginChallenge;
import edu.seu.vcampus.common.user.dto.LoginResponse;
import edu.seu.vcampus.common.user.dto.LoginVerify;
import edu.seu.vcampus.common.util.Sha256Util;
import java.awt.Component;
import java.awt.Container;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 本机协议对端验证登录连接复用、令牌传递及主窗口图书馆表格更新。 */
class LibrarySessionIntegrationTest {
    @Test
    void logsInAndLoadsLibraryThroughTheSameSocket() throws Exception {
        final ServerSocket server = new ServerSocket(0);
        server.setSoTimeout(5000);
        final CountDownLatch release = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Void> peer = executor.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                try (Socket socket = server.accept()) {
                    socket.setSoTimeout(5000);
                    MessageStream stream = new MessageStream(socket);
                    loginPeer(stream);
                    Set<Integer> commands = new HashSet<Integer>();
                    for (int index = 0; index < 2; index++) {
                        Message request = stream.recvMessage();
                        assertEquals("session-token", request.getToken());
                        assertEquals("001", request.getSender());
                        commands.add(request.getCommand());
                        stream.writeMessage(libraryReply(request));
                    }
                    assertTrue(commands.contains(MessageType.LIBRARY_SEARCH));
                    assertTrue(commands.contains(MessageType.LIBRARY_LIST_BORROWS));
                    release.await(5, TimeUnit.SECONDS);
                }
                return null;
            }
        });
        final ClientSession session = new ClientSession("127.0.0.1", server.getLocalPort());
        final CountDownLatch received = new CountDownLatch(2);
        final MainContentPanel[] content = new MainContentPanel[1];
        try {
            session.login("001", "secret".toCharArray(), "管理员");
            assertEquals("教师", session.getRole());
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    content[0] = new MainContentPanel(
                            session.getUsername(), session.getRole(), session);
                    session.setHandler(new UIUpdateHandler() {
                        @Override
                        public void handleMessage(Message message) {
                            content[0].handleMessage(message);
                            received.countDown();
                        }
                        @Override
                        public void connectionClosed(Exception cause) {
                            content[0].connectionClosed(cause);
                        }
                    });
                    content[0].showPage(PageNames.LIBRARY);
                }
            });
            assertTrue(received.await(5, TimeUnit.SECONDS));
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    List<JTable> tables = new ArrayList<JTable>();
                    collectTables(content[0], tables);
                    assertEquals(2, tables.size());
                    assertEquals(1, tables.get(0).getRowCount());
                    assertEquals("Java", tables.get(0).getValueAt(0, 1));
                    assertEquals(1, tables.get(1).getRowCount());
                    assertEquals("Java", tables.get(1).getValueAt(0, 1));
                }
            });
        } finally {
            release.countDown();
            session.close();
            server.close();
            executor.shutdownNow();
        }
        peer.get(5, TimeUnit.SECONDS);
        assertFalse(session.isAuthenticated());
    }

    private void loginPeer(MessageStream stream) throws Exception {
        Message first = stream.recvMessage();
        assertEquals(Command.USER_LOGIN, first.getCommand());
        assertNull(first.getToken());
        LoginChallenge challenge = new LoginChallenge();
        challenge.m_salt = "salt";
        challenge.m_nonce = "nonce";
        stream.writeMessage(success(Command.USER_LOGIN, challenge));
        Message verify = stream.recvMessage();
        assertEquals(Command.USER_LOGIN_VERIFY, verify.getCommand());
        LoginVerify proof = (LoginVerify) verify.getData();
        assertEquals("001", proof.m_user_name);
        assertEquals(Sha256Util.sha256Hex("nonce" + Sha256Util.sha256Hex("saltsecret")),
                proof.m_proof);
        LoginResponse login = new LoginResponse();
        login.m_role = "教师";
        login.m_token = "session-token";
        stream.writeMessage(success(Command.USER_LOGIN_VERIFY, login));
    }

    private Message libraryReply(Message request) {
        Object data;
        if (request.getCommand() == MessageType.LIBRARY_SEARCH) {
            data = Collections.singletonList(new Book("978-7", "Java", "A", "C", 2, 1));
        } else {
            BorrowRecord record = new BorrowRecord("001", "978-7", "Java", new Date(), new Date());
            record.setId(9L);
            data = Collections.singletonList(record);
        }
        Message response = success(request.getCommand(), data);
        response.setUid(request.getUid());
        return response;
    }

    private Message success(int command, Object data) {
        Message response = new Message(command, data);
        response.setStatusCode(StatusCode.SUCCESS);
        return response;
    }

    private void collectTables(Container container, List<JTable> result) {
        for (Component child : container.getComponents()) {
            if (child instanceof JTable) {
                result.add((JTable) child);
            } else if (child instanceof Container) {
                collectTables((Container) child, result);
            }
        }
    }
}
