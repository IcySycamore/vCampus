package edu.seu.vcampus.server.user;

import edu.seu.vcampus.common.random.RandomGen;
import edu.seu.vcampus.common.user.entity.SessionEntry;
import edu.seu.vcampus.common.user.dto.LoginChallenge;
import edu.seu.vcampus.common.user.dto.UserProfile;
import edu.seu.vcampus.common.util.Sha256Util;
import edu.seu.vcampus.server.user.UserRepository.Credential;

/**
 * 用户认证服务：注册 + 挑战-应答登录 + 登出。
 *
 * <p>
 * 注册：服务器生成随机盐并计算 sha256(salt + password) 落库； 登录：请求挑战（salt+nonce）→ 校验 proof →
 * 比对通过则签发并分发 token。
 */
public class AuthService {

    /** 伪盐：用户名不存在时也返回，防止账号枚举。 */
    private static final String FAKE_SALT = "00000000000000000000000000000000";

    /** 全局唯一实例：与消息分发器同级，服务器进程内全线程共用。 */
    private static AuthService instance;

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
     * @param users 凭证存储
     * @param nonces nonce 池
     * @param sessions token 会话池
     */
    public AuthService(UserRepository users, NonceManager nonces, SessionManager sessions) {
        this.m_users = users;
        this.m_nonces = nonces;
        this.m_sessions = sessions;
    }

    /**
     * 获取全局唯一的认证服务实例（懒加载单例）。
     *
     * <p>
     * 服务器进程内只应存在一份认证服务：它是所有连接线程与业务处理器共同的 身份权威入口，等价于全局消息分发器。仓库 / nonce 池 /
     * 会话池均取各自单例， 其中 token 表必须全服唯一，否则登录时签发的 token 在业务处理器中校验不到， 会出现「刚登录就 401」。
     *
     * @return 全局唯一的认证服务
     */
    public static synchronized AuthService getInstance() {
        if (instance == null) {
            instance = new AuthService(InMemoryUserRepository.getInstance(),
                    NonceManager.getInstance(), SessionManager.getInstance());
        }
        return instance;
    }

    /**
     * 返回本服务使用的会话管理器（全服唯一）。
     *
     * <p>
     * 连接线程做连接级鉴权、业务处理器做命令级鉴权都应使用本实例， 以保证与登录签发时是同一张 token 表。
     *
     * @return 会话管理器
     */
    public SessionManager getSessionManager() {
        return m_sessions;
    }

    /**
     * 注册：生成账户 uuid 与随机盐并计算加盐哈希落库。
     *
     * @param username 用户名
     * @param password 明文密码
     * @param role 角色
     * @throws IllegalStateException 用户名已存在
     */
    public void register(String username, String password, String role) {
        register(username, password, role, null);
    }

    /**
     * 注册新账户（带姓名）。
     *
     * <p>
     * 姓名只对师生有意义：管理员是系统运维角色，不建人员档案，调用方传 null 即可（界面也
     * 不会采集）。
     *
     * @param username 用户名
     * @param password 明文密码
     * @param role 角色
     * @param realName 真实姓名（管理员账号可为 null）
     * @throws IllegalStateException 用户名已存在
     */
    public void register(String username, String password, String role, String realName) {
        if (m_users.exists(username)) {
            throw new IllegalStateException("用户名已存在: " + username);
        }
        String uuid = m_random.getUuid().toString();// 注册时生成账户全局标识
        String salt = m_random.randomHex(16);
        String hash = Sha256Util.sha256Hex(salt + password);
        // 姓名空缺时用登录名顶上，保证 realName 永远可直接显示（界面不需要回退分支）
        String shown = realName != null && realName.trim().length() > 0
                ? realName.trim()
                : username;
        m_users.save(username, uuid, salt, hash, role, shown);
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
     * expect from server = sha256(nonce + H)，H 为库中加盐哈希 proof from client =
     * sha256(nonce + sha256(salt + password))
     *
     * @param username 用户名
     * @param proof 客户端 proof
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
        // 验证通过，签发 token（姓名随会话下发，客户端首屏即可显示）
        return m_sessions.create(cred.getUuid(), username, cred.getRealName(),
                cred.getRole());
    }

    /**
     * 登出：使 token 失效。
     *
     * @param token 会话令牌
     */
    public void logout(String token) {
        m_sessions.invalidate(token);
    }

    /**
     * 校验并更新 token 时效，返回对应会话记录。
     *
     * @param token 会话令牌
     * @return 会话记录（含真实 username/role）；无效或过期返回 null
     */
    public SessionEntry validateToken(String token) {
        return m_sessions.validate(token);
    }

    /**
     * 查询当前登录者的个人档案（命令 109）：回姓名等展示信息。
     *
     * <p>
     * 姓名虽然已随会话下发，但会话是登录那一刻的快照，姓名被管理员更正后不会变——需要最新值
     * 时走本方法，日常显示直接用会话里的那份即可（省一次往返）。
     *
     * @param token 会话令牌
     * @return 个人档案；会话无效返回 null
     */
    public UserProfile queryProfile(String token) {
        SessionEntry entry = m_sessions.validate(token);
        if (entry == null) {
            return null;
        }
        Credential cred = m_users.findByUsername(entry.getUsername());
        String realName = cred == null ? entry.getRealName() : cred.getRealName();
        return new UserProfile(entry.getUuid(), entry.getUsername(), realName,
                entry.getRole());
    }
}