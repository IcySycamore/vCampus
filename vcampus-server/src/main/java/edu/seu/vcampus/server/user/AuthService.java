package edu.seu.vcampus.server.user;

import edu.seu.vcampus.common.random.RandomGen;
import edu.seu.vcampus.common.user.dto.BatchResult;
import edu.seu.vcampus.common.user.dto.RegisterRequest;
import edu.seu.vcampus.common.user.entity.Role;
import edu.seu.vcampus.common.user.entity.SessionEntry;
import edu.seu.vcampus.common.user.dto.LoginChallenge;
import edu.seu.vcampus.common.util.Sha256Util;
import edu.seu.vcampus.server.user.UserRepository.Credential;

import java.util.ArrayList;
import java.util.List;

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

    /** 开户钩子登记表；未装配时为 null（表示不建立任何业务档案）。 */
    private AccountProvisioning m_provisioning;

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
     * 返回账户库（包内可见）：装配层用它构造 {@code UserAdminService}，保证两者操作同一份数据。
     *
     * @return 账户库
     */
    UserRepository repository() {
        return m_users;
    }

    /**
     * 装配开户钩子登记表：注册成功后据此为账户建立各模块的 1:1 档案。
     *
     * @param provisioning 开户钩子登记表；null 表示不建立业务档案
     */
    public void setProvisioning(AccountProvisioning provisioning) {
        this.m_provisioning = provisioning;
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
        register(username, username, password, role);
    }

    /**
     * 注册（含姓名）：生成账户 uuid 与随机盐并计算加盐哈希落库，随后为账户建立各模块 1:1 档案。
     *
     * <p>
     * 档案建立失败时会回滚已建档案并删除刚写入的账户，保证不留下「半个账户」。
     *
     * @param username 登录名
     * @param displayName 姓名；为空时取登录名（保证账户表里的姓名恒非空）
     * @param password 明文密码
     * @param role 角色显示名
     * @throws IllegalStateException 用户名已存在
     * @throws RuntimeException 某个模块建立档案失败（账户已回滚）
     */
    public void register(String username, String displayName, String password, String role) {
        if (m_users.exists(username)) {
            throw new IllegalStateException("用户名已存在: " + username);
        }
        // 姓名未采集时用登录名顶上。集中在这里归一化，用户列表、学籍联查、会话快照
        // 三处都会拿到非空姓名，不必各自再写一遍兜底。
        String shown = displayName == null || displayName.trim().length() == 0
                ? username
                : displayName.trim();
        String uuid = m_random.getUuid().toString();// 注册时生成账户全局标识
        String salt = m_random.randomHex(16);
        String hash = Sha256Util.sha256Hex(salt + password);
        m_users.save(new Credential(username, uuid, shown, salt, hash, role, true));
        provisionOrRollback(username, uuid, shown, role);
    }

    /** 为新账户建立各模块档案；失败则撤销刚写入的账户并抛出。 */
    private void provisionOrRollback(String username, String uuid, String displayName,
            String role) {
        if (m_provisioning == null) {
            return;
        }
        try {
            m_provisioning.provision(uuid, displayName == null ? username : displayName,
                    Role.fromDisplayName(role));
        } catch (RuntimeException e) {
            m_users.delete(username);// 不允许存在「没有档案的账户」
            throw e;
        }
    }

    /**
     * 判断账号是否已存在（管理员引导导入时用于幂等跳过）。
     *
     * @param username 登录名
     * @return 是否存在
     */
    public boolean exists(String username) {
        return m_users.exists(username);
    }

    /**
     * 批量注册（命令 103）：逐条建号，逐条记账，<b>不做全批回滚</b>。
     *
     * <p>
     * 从文件导入时某个登录名重名很常见，整批失败会让功能不可用；因此把失败原因原样带回给界面。
     *
     * @param requests 注册请求列表；null 或空返回全成功 0 条
     * @return 批量结果
     */
    public BatchResult registerAll(List<RegisterRequest> requests) {
        int success = 0;
        List<BatchResult.Failure> failures = new ArrayList<BatchResult.Failure>();
        if (requests != null) {
            for (RegisterRequest request : requests) {
                if (request == null || request.m_user_name == null
                        || request.m_user_name.trim().length() == 0) {
                    failures.add(new BatchResult.Failure("(未命名)", "缺少登录名"));
                    continue;
                }
                try {
                    register(request.m_user_name.trim(), request.m_display_name, request.m_password,
                            request.m_role);
                    success++;
                } catch (RuntimeException e) {
                    failures.add(new BatchResult.Failure(request.m_user_name, reasonOf(e)));
                }
            }
        }
        return new BatchResult(success, failures);
    }

    private String reasonOf(RuntimeException error) {
        String message = error.getMessage();
        if (message == null) {
            return "注册失败";
        }
        int index = message.indexOf(':');
        return index < 0 ? message : message.substring(0, index);
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
        // 验证通过，签发 token；姓名一并写进会话，客户端登录后首屏即可显示称呼。
        // 未采集姓名时（如管理员账号）用登录名顶上，保证会话里的姓名非空。
        String raw = cred.getDisplayName();
        String shown = raw == null || raw.trim().length() == 0 ? username : raw.trim();
        return m_sessions.create(cred.getUuid(), username, shown, cred.getRole());
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
     * 判断账号是否处于启用状态（禁用账号不能登录，回 P102）。
     *
     * @param username 登录名
     * @return 是否启用；账号不存在返回 false
     */
    public boolean isEnabled(String username) {
        Credential credential = m_users.findByUsername(username);
        return credential != null && credential.isEnabled();
    }

    /**
     * 修改密码：换盐并写入新哈希（客户端只提交 {@code sha256(newSalt + 新密码)}，明文不上线）。
     *
     * <p>
     * {@code proof} 非空表示「本人改密」，先用与登录相同的挑战-应答校验旧密码； {@code proof}
     * 为空表示「管理员重置」，由处理器层校验 USER_MANAGE 能力。
     *
     * @param username 目标登录名
     * @param proof 旧密码证明；null 表示管理员重置
     * @param newSalt 客户端生成的新盐
     * @param newHash 新密码哈希
     * @return 是否修改成功（账号不存在、nonce 失效或旧密码错误返回 false）
     */
    public boolean changePassword(String username, String proof, String newSalt, String newHash) {
        Credential credential = m_users.findByUsername(username);
        if (credential == null || newSalt == null || newHash == null) {
            return false;
        }
        if (proof != null) {
            String nonce = m_nonces.consume(username);
            if (nonce == null) {
                return false;
            }
            if (!Sha256Util.sha256Hex(nonce + credential.getHash()).equals(proof)) {
                return false;
            }
        }
        m_users.updateCredential(username, newSalt, newHash);
        return true;
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
}