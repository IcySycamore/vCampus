package edu.seu.vcampus.server.user;

import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.message.MessageSender;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.user.dto.LoginChallenge;
import edu.seu.vcampus.common.user.dto.LoginRequest;
import edu.seu.vcampus.common.util.Sha256Util;
import edu.seu.vcampus.server.network.ServerMessageDispatcher;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * AuthModule 装配测试：命令登记、演示账号预置、重复装配幂等与参数校验。
 */
class AuthModuleTest {

    /** 演示学生账号。 */
    private static final String DEMO_STUDENT = "001";

    /** 演示账号密码。 */
    private static final String DEMO_PASSWORD = "1";

    /** 登记后 100 命令可被分发，演示账号可直接登录，且返回的会话表即签发表。 */
    @Test
    void registersCommandsAndSeedsDemoAccounts() {
        ServerMessageDispatcher dispatcher = new ServerMessageDispatcher();
        SessionManager sessions = AuthModule.register(dispatcher);
        assertNotNull(sessions);

        LoginRequest request = new LoginRequest();
        request.m_user_name = DEMO_STUDENT;
        CapturingSender sender = new CapturingSender();
        dispatcher.dispatch(new Message(Command.USER_LOGIN, request), sender);

        assertEquals(StatusCode.SUCCESS, sender.m_last.getStatusCode(),
                "USER_LOGIN 应已被 AuthModule 登记");
        LoginChallenge challenge = (LoginChallenge) sender.m_last.getData();
        String token = AuthService.getInstance().loginVerify(DEMO_STUDENT,
                proof(challenge, DEMO_PASSWORD));
        assertNotNull(token, "预置的演示账号应可直接登录");
        assertNotNull(sessions.validate(token), "返回的会话表必须是签发 token 的那一张");
    }

    /** 重复装配幂等：账号已存在不报错。 */
    @Test
    void registerIsIdempotent() {
        assertNotNull(AuthModule.register(new ServerMessageDispatcher()));
        assertNotNull(AuthModule.register(new ServerMessageDispatcher()));
    }

    /** 分发器为 null 时快速失败。 */
    @Test
    void rejectsNullDispatcher() {
        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() {
                AuthModule.register(null);
            }
        });
    }

    private String proof(LoginChallenge challenge, String password) {
        String inner = Sha256Util.sha256Hex(challenge.m_salt + password);
        return Sha256Util.sha256Hex(challenge.m_nonce + inner);
    }

    /** 记录最后一条响应的发送器。 */
    private static final class CapturingSender implements MessageSender {

        /** 最后一条响应。 */
        private Message m_last;

        @Override
        public void send(Message response) {
            m_last = response;
        }
    }
}
