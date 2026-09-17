package edu.seu.vcampus.server.bank;

import edu.seu.vcampus.common.bank.dto.BankAdminQuery;
import edu.seu.vcampus.common.bank.dto.BankAdminRefRequest;
import edu.seu.vcampus.common.bank.dto.BankAdminResetPasswordRequest;
import edu.seu.vcampus.common.bank.dto.BankAdminSetFrozenRequest;
import edu.seu.vcampus.common.bank.dto.BankAdminTransactionsRequest;
import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.message.MessageSender;
import edu.seu.vcampus.common.user.entity.SessionEntry;
import edu.seu.vcampus.server.network.ServerMessageDispatcher;
import edu.seu.vcampus.server.user.AuthService;
import edu.seu.vcampus.server.user.InMemoryUserRepository;
import edu.seu.vcampus.server.user.NonceManager;
import edu.seu.vcampus.server.user.SessionManager;
import edu.seu.vcampus.server.user.UserRepository;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * 银行管理轨命令（610-614）的权限边界：无 token回401，学生与教师一律403，管理员才能到达业务层。
 *
 * <p>越权必须回403而不是400/404——后两者说明请求已经进了业务层，权限门就没起作用。</p>
 */
class BankAdminPermissionTest {

    private ServerMessageDispatcher dispatcher;
    private UserRepository users;
    private SessionManager sessions;
    private String studentToken;
    private String teacherToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        users = new InMemoryUserRepository();
        sessions = new SessionManager();
        AuthService auth = new AuthService(users, new NonceManager(), sessions);
        auth.register("stu", "学生甲", "campus-pass", "学生");
        auth.register("tea", "教师乙", "campus-pass", "教师");
        auth.register("root", "管理员", "campus-pass", "管理员");
        studentToken = tokenOf("stu", "学生");
        teacherToken = tokenOf("tea", "教师");
        adminToken = tokenOf("root", "管理员");

        dispatcher = new ServerMessageDispatcher();
        BankModule.register(dispatcher, new BankService(), auth, new BankIdentityResolver() {
            @Override
            public String resolveOwnerUuid(Message request) {
                SessionEntry entry = sessions.validate(request.getToken());
                return entry == null ? null : entry.getUuid();
            }
        }, users);
    }

    private String tokenOf(String username, String role) {
        return sessions.create(users.findByUsername(username).getUuid(), username, role);
    }

    /** 每条管理轨命令都对非管理员关闭、对管理员放行到业务层。 */
    @Test
    void adminCommandsRejectNonAdmins() {
        for (Object[] request : adminRequests()) {
            int command = ((Integer) request[0]).intValue();
            Object payload = request[1];
            assertEquals(StatusCode.UNAUTHORIZED,
                    dispatch(command, null, payload).getStatusCode());
            assertEquals(StatusCode.FORBIDDEN,
                    dispatch(command, studentToken, payload).getStatusCode());
            assertEquals(StatusCode.FORBIDDEN,
                    dispatch(command, teacherToken, payload).getStatusCode());
            String adminStatus = dispatch(command, adminToken, payload).getStatusCode();
            assertNotEquals(StatusCode.FORBIDDEN, adminStatus);
            assertNotEquals(StatusCode.UNAUTHORIZED, adminStatus);
        }
    }

    /** 未装配用户仓库时管理轨命令回500，而不是放行。 */
    @Test
    void adminCommandsFailWhenUserRepositoryMissing() {
        ServerMessageDispatcher bare = new ServerMessageDispatcher();
        BankModule.register(bare, new BankService(), AuthService.getInstance(),
                new BankIdentityResolver() {
                    @Override
                    public String resolveOwnerUuid(Message request) {
                        return "uuid-any";
                    }
                });
        final List<Message> sent = new ArrayList<Message>();
        bare.dispatch(request(Command.BANK_ADMIN_LIST_ACCOUNTS, "any-token",
                new BankAdminQuery()), sender(sent));
        assertEquals(1, sent.size());
        assertEquals(StatusCode.INTERNAL_ERROR, sent.get(0).getStatusCode());
    }

    /** 五条管理轨命令及其载荷。 */
    private static List<Object[]> adminRequests() {
        List<Object[]> requests = new ArrayList<Object[]>();
        requests.add(new Object[] {Integer.valueOf(Command.BANK_ADMIN_LIST_ACCOUNTS),
                new BankAdminQuery()});
        requests.add(new Object[] {Integer.valueOf(Command.BANK_ADMIN_QUERY_ACCOUNT),
                new BankAdminRefRequest("stu")});
        requests.add(new Object[] {Integer.valueOf(Command.BANK_ADMIN_TRANSACTION_LIST),
                new BankAdminTransactionsRequest("stu", null)});
        requests.add(new Object[] {Integer.valueOf(Command.BANK_ADMIN_SET_FROZEN),
                new BankAdminSetFrozenRequest("stu", true)});
        requests.add(new Object[] {Integer.valueOf(Command.BANK_ADMIN_RESET_PASSWORD),
                new BankAdminResetPasswordRequest("stu", new byte[16], new byte[32])});
        return requests;
    }

    private Message dispatch(int command, String token, Object payload) {
        final List<Message> sent = new ArrayList<Message>();
        dispatcher.dispatch(request(command, token, payload), sender(sent));
        assertEquals(1, sent.size());
        return sent.get(0);
    }

    private static Message request(int command, String token, Object payload) {
        Message message = new Message(command, payload);
        message.setToken(token);
        message.setUid(7L);
        return message;
    }

    private static MessageSender sender(final List<Message> sink) {
        return new MessageSender() {
            @Override
            public void send(Message response) {
                sink.add(response);
            }
        };
    }
}
