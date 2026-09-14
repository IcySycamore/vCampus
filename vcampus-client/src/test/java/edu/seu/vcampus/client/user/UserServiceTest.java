package edu.seu.vcampus.client.user;

import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.message.PageResponse;
import edu.seu.vcampus.common.user.dto.ChangePasswordRequest;
import edu.seu.vcampus.common.user.dto.LoginChallenge;
import edu.seu.vcampus.common.user.entity.Role;
import edu.seu.vcampus.common.user.entity.SessionEntry;
import edu.seu.vcampus.common.user.entity.User;
import edu.seu.vcampus.common.util.Sha256Util;

import java.io.IOException;
import java.util.ArrayList;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * UserService 测试：挑战-应答登录、改密与会话生命周期（我的轨）。
 *
 * <p>
 * 按 ADR-0009 D10：模块 API 的主战场是同包假分发器单测——断言「方法 → 命令码 + 载荷」与
 * 「状态码 → 异常」，不需要真实连接。管理轨的命令在 {@code UserAdminServiceTest}。
 */
class UserServiceTest {

    /** 请求超时。 */
    private static final long TIMEOUT = 1000L;

    /** 改密：先用挑战校验旧密码（明文不上线），再提交新盐与新哈希。 */
    @Test
    void changePasswordUsesProofAndNewSalt() {
        FakeUserDispatcher dispatcher = new FakeUserDispatcher();
        LoginChallenge challenge = new LoginChallenge();
        challenge.m_salt = "salt-9";
        challenge.m_nonce = "nonce-9";
        dispatcher.reply(Command.USER_LOGIN, response(StatusCode.SUCCESS, challenge));
        dispatcher.reply(Command.USER_CHANGE_PASSWORD, response(StatusCode.SUCCESS, null));
        UserService service = new UserService(dispatcher, TIMEOUT);
        service.session().cache("token-xyz", new SessionEntry("uuid-1", "001", "学生", 0L));

        service.changePassword("old-pw", "new-pw");

        ChangePasswordRequest payload = (ChangePasswordRequest) dispatcher.sent.get(1).getData();
        assertNull(payload.getUserName());// 我的轨：服务端按会话定身份
        assertEquals(Sha256Util.sha256Hex("nonce-9" + Sha256Util.sha256Hex("salt-9" + "old-pw")),
                payload.getProof());
        assertNotNull(payload.getNewSalt());
        assertEquals(Sha256Util.sha256Hex(payload.getNewSalt() + "new-pw"), payload.getNewHash());
        assertEquals("token-xyz", dispatcher.sent.get(1).getToken());
    }

    /** 未登录时改密直接抛 401，不发请求。 */
    @Test
    void changePasswordRequiresSession() {
        FakeUserDispatcher dispatcher = new FakeUserDispatcher();
        final UserService service = new UserService(dispatcher, TIMEOUT);
        try {
            service.changePassword("old", "new");
            fail("expected ApiException");
        } catch (ApiException e) {
            assertEquals(StatusCode.UNAUTHORIZED, e.getStatusCode());
        }
        assertTrue(dispatcher.sent.isEmpty());
    }

    /** 登出后内存会话被清空，不再持有令牌。 */
    @Test
    void logoutClearsSession() {
        FakeUserDispatcher dispatcher = new FakeUserDispatcher();
        dispatcher.reply(Command.USER_LOGOUT, response(StatusCode.SUCCESS, null));
        UserService service = new UserService(dispatcher, TIMEOUT);
        service.session().cache("token-xyz", new SessionEntry("uuid-1", "001", "学生", 0L));

        service.logout();

        assertFalse(service.isLoggedIn());
        assertNull(service.currentToken());
        assertNull(service.currentSession());
        assertEquals("token-xyz", dispatcher.sent.get(0).getToken());
    }

    /** 连接断开即丢弃内存会话（token 不跨连接存活，不落盘）。 */
    @Test
    void connectionClosedClearsSession() {
        UserService service = new UserService(new FakeUserDispatcher(), TIMEOUT);
        service.session().cache("token-xyz", new SessionEntry("uuid-1", "001", "学生", 0L));

        service.connectionClosed(new IOException("peer reset"));

        assertFalse(service.isLoggedIn());
        assertNull(service.currentSession());
    }

    /** 无响应（超时）归一为本地超时码。 */
    @Test
    void timeoutBecomesLocalCode() {
        FakeUserDispatcher dispatcher = new FakeUserDispatcher();
        UserService service = new UserService(dispatcher, TIMEOUT);
        try {
            service.logout();
            fail("expected ApiException");
        } catch (ApiException e) {
            assertTrue(e.isLocal());
            assertEquals("服务器无响应（超时）", e.getMessage());
        }
    }

    /** 分发器为 null 时构造失败。 */
    @Test
    void rejectsNullDispatcher() {
        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() {
                new UserService(null, TIMEOUT);
            }
        });
    }

    /** 管理轨 API 与本人轨共用同一份会话（token 只有一个来源）。 */
    @Test
    void adminSharesSessionWithUserService() {
        FakeUserDispatcher dispatcher = new FakeUserDispatcher();
        PageResponse<User> empty = new PageResponse<User>(new ArrayList<User>(), 0L, 1, 20);
        dispatcher.reply(Command.USER_LIST, response(StatusCode.SUCCESS, empty));
        UserService service = new UserService(dispatcher, TIMEOUT);
        service.session().cache("shared-token", new SessionEntry("uuid-1", "admin", "管理员", 0L));

        assertNotNull(service.admin());
        service.admin().listUsers(null);

        assertEquals("shared-token", dispatcher.sent.get(0).getToken());
    }

    private Message response(String status, Object data) {
        Message message = new Message(0, data);
        message.setStatusCode(status);
        return message;
    }
}
