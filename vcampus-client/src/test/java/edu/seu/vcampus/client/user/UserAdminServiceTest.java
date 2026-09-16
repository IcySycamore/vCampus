package edu.seu.vcampus.client.user;

import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.message.PageResponse;
import edu.seu.vcampus.common.user.dto.UserEnabledRequest;
import edu.seu.vcampus.common.user.dto.UserQuery;
import edu.seu.vcampus.common.user.dto.UserRefRequest;
import edu.seu.vcampus.common.user.dto.UserUpdateRequest;
import edu.seu.vcampus.common.user.entity.Role;
import edu.seu.vcampus.common.user.entity.SessionEntry;
import edu.seu.vcampus.common.user.entity.User;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * UserAdminService 测试：管理轨的「方法 → 命令码 + 载荷」映射（见 ADR-0009 D10）。
 *
 * <p>
 * 管理轨从 {@code UserService} 拆出后独立成测：它要求显式指定目标账号，并需管理员令牌。
 */
class UserAdminServiceTest {

    /** 请求超时。 */
    private static final long TIMEOUT = 1000L;

    /** 注册请求需携带当前（管理员）令牌，且角色以显示名上线。 */
    @Test
    void registerCarriesAdminToken() {
        FakeUserDispatcher dispatcher = new FakeUserDispatcher();
        dispatcher.reply(Command.USER_REGISTER, response(StatusCode.SUCCESS, null));

        adminOf(dispatcher).register("002", "李四", Role.TEACHER, "pw");

        Message sent = dispatcher.sent.get(0);
        assertEquals(Command.USER_REGISTER, sent.getCommand());
        assertEquals("admin-token", sent.getToken());
    }

    /** 注销发送用户引用载荷。 */
    @Test
    void unregisterSendsUserRef() {
        FakeUserDispatcher dispatcher = new FakeUserDispatcher();
        dispatcher.reply(Command.USER_UNREGISTER, response(StatusCode.SUCCESS, null));

        adminOf(dispatcher).unregister("002");

        Message sent = dispatcher.sent.get(0);
        assertEquals(Command.USER_UNREGISTER, sent.getCommand());
        assertEquals("002", ((UserRefRequest) sent.getData()).getUserName());
    }

    /** 启用/禁用发送状态载荷。 */
    @Test
    void toggleEnabledSendsState() {
        FakeUserDispatcher dispatcher = new FakeUserDispatcher();
        dispatcher.reply(Command.USER_TOGGLE_ENABLED, response(StatusCode.SUCCESS, null));

        adminOf(dispatcher).toggleUserEnabled("002", false);

        UserEnabledRequest payload = (UserEnabledRequest) dispatcher.sent.get(0).getData();
        assertEquals("002", payload.getUserName());
        assertFalse(payload.isEnabled());
    }

    /** 编辑姓名发送更新请求。 */
    @Test
    void updateUserSendsRequest() {
        FakeUserDispatcher dispatcher = new FakeUserDispatcher();
        dispatcher.reply(Command.USER_UPDATE, response(StatusCode.SUCCESS, null));

        adminOf(dispatcher).updateUser(new UserUpdateRequest("002", "李四"));

        assertEquals(Command.USER_UPDATE, dispatcher.sent.get(0).getCommand());
    }

    /** 分页查询返回同一批用户，载荷为查询条件。 */
    @Test
    void listUsersReturnsPage() {
        FakeUserDispatcher dispatcher = new FakeUserDispatcher();
        User user = new User("002", "李四", null, Role.TEACHER);
        user.setUuid("uuid-2");
        PageResponse<User> page = new PageResponse<User>(Arrays.asList(user), 1L, 1, 20);
        dispatcher.reply(Command.USER_LIST, response(StatusCode.SUCCESS, page));

        PageResponse<User> result = adminOf(dispatcher)
                .listUsers(new UserQuery("李", Role.TEACHER, null, 1, 20));

        assertEquals(1, result.getItems().size());
        assertEquals("李四", result.getItems().get(0).getDisplayName());
        assertEquals(1L, result.getTotal());
        assertEquals(Command.USER_LIST, dispatcher.sent.get(0).getCommand());
    }

    /** 非分页载荷视为协议异常。 */
    @Test
    void listUsersRejectsMalformedPayload() {
        FakeUserDispatcher dispatcher = new FakeUserDispatcher();
        dispatcher.reply(Command.USER_LIST, response(StatusCode.SUCCESS, "oops"));
        final UserAdminService admin = adminOf(dispatcher);

        assertThrows(ApiException.class, new Executable() {
            @Override
            public void execute() {
                admin.listUsers(null);
            }
        });
    }

    /** 构造带管理员会话的管理轨 API。 */
    private UserAdminService adminOf(FakeUserDispatcher dispatcher) {
        UserService service = new UserService(dispatcher, TIMEOUT);
        service.session().cache("admin-token",
                new SessionEntry("uuid-admin", "admin", "管理员", 0L));
        return service.admin();
    }

    private Message response(String status, Object data) {
        Message message = new Message(0, data);
        message.setStatusCode(status);
        return message;
    }
}
