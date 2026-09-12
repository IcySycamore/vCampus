package edu.seu.vcampus.server.user;

import edu.seu.vcampus.common.user.entity.SessionEntry;
import edu.seu.vcampus.common.user.dto.LoginChallenge;
import edu.seu.vcampus.common.user.dto.UserProfile;
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
        auth = new AuthService(new InMemoryUserRepository(), new NonceManager(), sessions);
    }

    /**
     * 注册后正确密码可登录并签发 token，会话含真实身份。
     */
    @Test
    void loginSuccess() {
        auth.register("001", "secret", "学生");
        LoginChallenge ch = auth.loginChallenge("001");
        String token = auth.loginVerify("001", clientProof(ch, "secret"));
        assertNotNull(token);
        SessionEntry entry = sessions.validate(token);
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
        LoginChallenge ch = auth.loginChallenge("001");
        assertNull(auth.loginVerify("001", clientProof(ch, "wrong")));
    }

    /**
     * 用户名不存在时挑战给伪盐，但验证失败。
     */
    @Test
    void unknownUserFails() {
        LoginChallenge ch = auth.loginChallenge("ghost");
        assertNotNull(ch.m_nonce);
        assertNull(auth.loginVerify("ghost", clientProof(ch, "any")));
    }

    /**
     * nonce 一次性：同一 nonce 第二次验证失败。
     */
    @Test
    void nonceOneTime() {
        auth.register("001", "secret", "学生");
        LoginChallenge ch = auth.loginChallenge("001");
        String proof = clientProof(ch, "secret");
        assertNotNull(auth.loginVerify("001", proof));
        assertNull(auth.loginVerify("001", proof));
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
     * 注册时登记姓名，登录后会话里带得到——「登录后显示姓名」的服务端源头。
     */
    @Test
    void loginCarriesRealName() {
        auth.register("002", "secret", "学生", "张三");
        LoginChallenge ch = auth.loginChallenge("002");
        String token = auth.loginVerify("002", clientProof(ch, "secret"));

        SessionEntry entry = sessions.validate(token);
        assertEquals("张三", entry.getRealName());
    }

    /**
     * 管理员不采集姓名：服务端用登录名顶上，保证 realName 非空、可直接显示。
     */
    @Test
    void adminNameFallsBackToUsername() {
        auth.register("003", "secret", "管理员", null);
        LoginChallenge ch = auth.loginChallenge("003");
        String token = auth.loginVerify("003", clientProof(ch, "secret"));

        SessionEntry entry = sessions.validate(token);
        assertEquals("003", entry.getRealName());
    }

    /**
     * 查询个人档案（命令 109）：会话有效时回姓名与角色。
     */
    @Test
    void queryProfileReturnsRealName() {
        auth.register("002", "secret", "教师", "李四");
        LoginChallenge ch = auth.loginChallenge("002");
        String token = auth.loginVerify("002", clientProof(ch, "secret"));

        UserProfile profile = auth.queryProfile(token);

        assertNotNull(profile);
        assertEquals("002", profile.getUserName());
        assertEquals("李四", profile.getRealName());
        assertEquals("教师", profile.getRole());
    }

    /**
     * 会话无效时查不到档案（返回 null，由 handler 转成 401）。
     */
    @Test
    void queryProfileWithBadTokenReturnsNull() {
        assertNull(auth.queryProfile("not-a-token"));
        assertNull(auth.queryProfile(null));
    }

    /**
     * 按客户端视角计算 proof = sha256(nonce + sha256(salt + password))。
     *
     * @param challenge 服务器挑战
     * @param password 明文密码
     * @return proof
     */
    private String clientProof(LoginChallenge challenge, String password) {
        String inner = Sha256Util.sha256Hex(challenge.m_salt + password);
        return Sha256Util.sha256Hex(challenge.m_nonce + inner);
    }
}