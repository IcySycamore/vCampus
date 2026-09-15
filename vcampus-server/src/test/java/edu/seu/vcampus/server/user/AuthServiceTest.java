package edu.seu.vcampus.server.user;

import edu.seu.vcampus.common.user.dto.BatchResult;
import edu.seu.vcampus.common.user.dto.RegisterRequest;
import edu.seu.vcampus.common.user.entity.Role;
import edu.seu.vcampus.common.user.entity.SessionEntry;
import edu.seu.vcampus.common.user.dto.LoginChallenge;
import edu.seu.vcampus.common.util.Sha256Util;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AuthService 挑战-应答登录测试。
 */
class AuthServiceTest {

    private AuthService auth;
    private SessionManager sessions;
    private InMemoryUserRepository repository;

    /**
     * 每个用例前构造隔离的服务。
     */
    @BeforeEach
    void setUp() {
        sessions = new SessionManager();
        repository = new InMemoryUserRepository();
        auth = new AuthService(repository, new NonceManager(), sessions);
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

    /** 注册成功后同步建立各模块 1:1 档案（角色作为参数传给钩子）。 */
    @Test
    void registerProvisionsProfiles() {
        final List<String> provisioned = new ArrayList<String>();
        AccountProvisioning provisioning = new AccountProvisioning();
        provisioning.add(new AccountProvisioner() {
            @Override
            public void provision(String userUuid, String userName, Role role) {
                provisioned.add(userName + ":" + role);
            }

            @Override
            public void revoke(String userUuid) {
                provisioned.add("revoke:" + userUuid);
            }
        });
        auth.setProvisioning(provisioning);

        auth.register("001", "张三", "secret", "学生");

        assertEquals(1, provisioned.size());
        assertEquals("张三:STUDENT", provisioned.get(0));
    }

    /** 建档失败时回滚账户：不允许存在「没有档案的账号」。 */
    @Test
    void registerRollsBackAccountWhenProvisioningFails() {
        AccountProvisioning provisioning = new AccountProvisioning();
        provisioning.add(new AccountProvisioner() {
            @Override
            public void provision(String userUuid, String userName, Role role) {
                throw new IllegalStateException("档案建立失败");
            }

            @Override
            public void revoke(String userUuid) {
                // 无需处理
            }
        });
        auth.setProvisioning(provisioning);

        assertThrows(IllegalStateException.class, new Executable() {
            @Override
            public void execute() {
                auth.register("001", "张三", "secret", "学生");
            }
        });
        assertFalse(repository.exists("001"));
        assertTrue(repository.findAll().isEmpty());
    }

    /** 启用位：默认启用，可被关闭。 */
    @Test
    void tracksEnabledFlag() {
        auth.register("001", "secret", "学生");
        assertTrue(auth.isEnabled("001"));
        assertFalse(auth.isEnabled("ghost"));

        repository.setEnabled("001", false);

        assertFalse(auth.isEnabled("001"));
    }

    /** 改密（本人）：proof 校验旧密码，改完新密码可登录、旧密码失效。 */
    @Test
    void changePasswordWithProof() {
        auth.register("001", "secret", "学生");
        LoginChallenge challenge = auth.loginChallenge("001");
        String newSalt = "new-salt";

        assertTrue(auth.changePassword("001", clientProof(challenge, "secret"), newSalt,
                Sha256Util.sha256Hex(newSalt + "new-secret")));

        LoginChallenge after = auth.loginChallenge("001");
        assertNotNull(auth.loginVerify("001", clientProof(after, "new-secret")));
        LoginChallenge again = auth.loginChallenge("001");
        assertNull(auth.loginVerify("001", clientProof(again, "secret")));
    }

    /** 改密（本人）旧密码错误、账号不存在或参数缺失均失败。 */
    @Test
    void changePasswordRejectsBadInput() {
        auth.register("001", "secret", "学生");
        LoginChallenge challenge = auth.loginChallenge("001");

        assertFalse(auth.changePassword("001", clientProof(challenge, "wrong"), "s", "h"));
        assertFalse(auth.changePassword("ghost", null, "s", "h"));
        assertFalse(auth.changePassword("001", null, null, "h"));
    }

    /** 改密（管理员重置）：不带 proof 直接落新凭证。 */
    @Test
    void changePasswordWithoutProofResets() {
        auth.register("001", "secret", "学生");
        String newSalt = "reset-salt";

        assertTrue(
                auth.changePassword("001", null, newSalt, Sha256Util.sha256Hex(newSalt + "reset")));

        LoginChallenge challenge = auth.loginChallenge("001");
        assertNotNull(auth.loginVerify("001", clientProof(challenge, "reset")));
    }

    /** 批量注册：逐条建号并逐条记账，重复登录名不影响其它账号。 */
    @Test
    void registerAllReportsPerItemResult() {
        List<RegisterRequest> requests = new ArrayList<RegisterRequest>();
        requests.add(request("100", "张三", "学生", "pw1"));
        requests.add(request("101", "李四", "教师", "pw2"));
        requests.add(request("100", "重名", "学生", "pw3"));
        requests.add(request(null, null, "学生", "pw4"));

        BatchResult result = auth.registerAll(requests);

        assertEquals(2, result.getSuccessCount());
        assertEquals(2, result.getFailureCount());
        assertFalse(result.isAllSucceeded());
        assertNotNull(repository.findByUsername("100"));
        assertNotNull(repository.findByUsername("101"));
        assertEquals("张三", repository.findByUsername("100").getDisplayName());
    }

    /** 批量注册 null 或空列表不报错。 */
    @Test
    void registerAllToleratesEmptyInput() {
        assertEquals(0, auth.registerAll(null).getSuccessCount());
        assertTrue(auth.registerAll(new ArrayList<RegisterRequest>()).isAllSucceeded());
    }

    private RegisterRequest request(String userName, String displayName, String role,
            String password) {
        RegisterRequest request = new RegisterRequest();
        request.m_user_name = userName;
        request.m_display_name = displayName;
        request.m_role = role;
        request.m_password = password;
        return request;
    }

    /**
     * 注册时登记姓名，登录后会话里带得到——「登录后显示姓名」的服务端源头。
     */
    @Test
    void loginCarriesDisplayName() {
        auth.register("002", "张三", "secret", "学生");
        LoginChallenge ch = auth.loginChallenge("002");
        String token = auth.loginVerify("002", clientProof(ch, "secret"));

        SessionEntry entry = sessions.validate(token);
        assertEquals("张三", entry.getDisplayName());
    }

    /**
     * 管理员不采集姓名：服务端用登录名顶上，保证会话里的姓名非空、可直接显示。
     */
    @Test
    void adminNameFallsBackToUsername() {
        auth.register("003", null, "secret", "管理员");
        LoginChallenge ch = auth.loginChallenge("003");
        String token = auth.loginVerify("003", clientProof(ch, "secret"));

        SessionEntry entry = sessions.validate(token);
        assertEquals("003", entry.getDisplayName());
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