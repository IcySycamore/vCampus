package edu.seu.vcampus.server.auth;

import edu.seu.vcampus.common.user.dto.LoginChallenge;
import edu.seu.vcampus.common.util.Sha256Util;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * AuthService 挑战-应答登录测试。
 */
class AuthServiceTest {

    private AuthService auth;
    private SessionManager sessions;

    /**
     * 每个用例前构造隔离的服务。
     */
    @BeforeEach
    void setUp() {
        sessions = new SessionManager();
        auth = new AuthService(new InMemoryUserRepository(),
                new NonceStore(), sessions);
    }

    /**
     * 注册后正确密码可登录并签发 token，会话含真实身份。
     */
    @Test
    void loginSuccess() {
        auth.register("001", "secret", "学生");
        LoginChallenge ch = auth.challengeLogin("001");
        String token = auth.verifyLogin("001", ch.m_nonce,
                clientProof(ch, "secret"));
        assertNotNull(token);
        SessionManager.SessionEntry entry = sessions.validate(token);
        assertNotNull(entry);
        assertEquals("001", entry.getUsername());
        assertEquals("学生", entry.getRole());
    }

    /**
     * 密码错误时验证失败。
     */
    @Test
    void wrongPasswordFails() {
        auth.register("001", "secret", "学生");
        LoginChallenge ch = auth.challengeLogin("001");
        assertNull(auth.verifyLogin("001", ch.m_nonce,
                clientProof(ch, "wrong")));
    }

    /**
     * 用户名不存在时挑战给伪盐，但验证失败。
     */
    @Test
    void unknownUserFails() {
        LoginChallenge ch = auth.challengeLogin("ghost");
        assertNotNull(ch.m_nonce);
        assertNull(auth.verifyLogin("ghost", ch.m_nonce,
                clientProof(ch, "any")));
    }

    /**
     * nonce 一次性：同一 nonce 第二次验证失败。
     */
    @Test
    void nonceOneTime() {
        auth.register("001", "secret", "学生");
        LoginChallenge ch = auth.challengeLogin("001");
        String proof = clientProof(ch, "secret");
        assertNotNull(auth.verifyLogin("001", ch.m_nonce, proof));
        assertNull(auth.verifyLogin("001", ch.m_nonce, proof));
    }

    /**
     * 重复注册抛异常。
     */
    @Test
    void duplicateRegisterFails() {
        auth.register("001", "secret", "学生");
        assertThrows(IllegalStateException.class, new Executable() {
            @Override
            public void execute() {
                auth.register("001", "other", "学生");
            }
        });
    }

    /**
     * 按客户端视角计算 proof = sha256(nonce + sha256(salt + password))。
     *
     * @param challenge 服务器挑战
     * @param password  明文密码
     * @return proof
     */
    private String clientProof(LoginChallenge challenge, String password) {
        String inner = Sha256Util.sha256Hex(challenge.m_salt + password);
        return Sha256Util.sha256Hex(challenge.m_nonce + inner);
    }
}