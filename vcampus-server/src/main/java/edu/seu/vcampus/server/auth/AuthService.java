package edu.seu.vcampus.server.auth;

import edu.seu.vcampus.common.random.RandomGen;
import edu.seu.vcampus.common.user.dto.LoginChallenge;
import edu.seu.vcampus.common.util.Sha256Util;
import edu.seu.vcampus.server.auth.UserRepository.Credential;

/**
 * 用户认证服务：注册 + 挑战-应答登录 + 登出。
 *
 * <p>
 * 注册：服务器生成随机盐并计算 sha256(salt + password) 落库；
 * 登录：请求挑战（salt+nonce）→ 校验 proof → 比对通过则签发并分发 token。
 */
public class AuthService {

    /** 伪盐：用户名不存在时也返回，防止账号枚举。 */
    private static final String FAKE_SALT = "00000000000000000000000000000000";

    /** 用户凭证存储。 */
    private final UserRepository m_users;

    /** 一次性 nonce 池。 */
    private final NonceManager m_nonces;

    /** 会话 token 池。 */
    private final SessionManager m_sessions;

    /** 随机源。 */
    private final RandomGen m_random = new RandomGen();

    /**
     * 构造认证服务。
     *
     * @param users    凭证存储
     * @param nonces   nonce 池
     * @param sessions token 会话池
     */
    public AuthService(UserRepository users, NonceManager nonces,
            SessionManager sessions) {
        this.m_users = users;
        this.m_nonces = nonces;
        this.m_sessions = sessions;
    }

    /**
     * 注册：生成随机盐并计算加盐哈希落库。
     *
     * @param username 用户名
     * @param password 明文密码
     * @param role     角色
     * @throws IllegalStateException 用户名已存在
     */
    public void register(String username, String password, String role) {
        if (m_users.exists(username)) {
            throw new IllegalStateException("用户名已存在: " + username);
        }
        String salt = m_random.randomHex(16);
        String hash = Sha256Util.sha256Hex(salt + password);
        m_users.save(username, salt, hash, role);
    }

    /**
     * 登录第①步：为用户生成挑战
     *
     * @param username 用户名
     * @return 挑战
     */
    public LoginChallenge loginChallenge(String username) {
        Credential cred = m_users.findByUsername(username);
        String salt = cred == null ? FAKE_SALT : cred.getSalt();
        LoginChallenge challenge = new LoginChallenge();
        challenge.m_salt = salt;
        challenge.m_nonce = m_nonces.issue(username);
        return challenge;
    }

    /**
     * 登录第③步：校验 proof 并签发 token。
     *
     * <p>
     * expect from server = sha256(nonce + H)，H 为库中加盐哈希
     * proof from client = sha256(nonce + sha256(salt + password))
     *
     * @param username 用户名
     * @param proof    客户端 proof
     * @return 新 token；校验失败返回 null
     */
    public String loginVerify(String username, String proof) {
        Credential cred = m_users.findByUsername(username);
        if (cred == null) {// 不存在账户
            return null;
        }
        String nonce = m_nonces.consume(username);// 取回并消费该用户名当前 nonce
        if (nonce == null) {// nonce 未分配或已过期
            return null;
        }
        String expect = Sha256Util.sha256Hex(nonce + cred.getHash());
        if (!expect.equals(proof)) {// client 计算的 hash 与预期不等，验证失败
            return null;
        }
        // 验证通过，签发 token
        return m_sessions.create(username, cred.getRole());
    }

    /**
     * 登出：使 token 失效。
     *
     * @param token 会话令牌
     */
    public void logout(String token) {
        m_sessions.invalidate(token);
    }

    public SessionManager.SessionEntry validateToken(String token) {
        return m_sessions.validate(token);
    }
}