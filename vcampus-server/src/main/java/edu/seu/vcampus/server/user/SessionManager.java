package edu.seu.vcampus.server.user;

import edu.seu.vcampus.common.random.RandomGen;
import edu.seu.vcampus.common.user.entity.SessionEntry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录会话管理
 *
 * <p>
 * 登录成功后签发 token；之后每条请求携带 token，服务器按 token 解析身份。 token 30 分钟滑动过期，每次校验刷新
 * TTL，登出删除。线程安全。
 */
public class SessionManager {

    /** 过期时长（毫秒）：30 分钟。 */
    private static final long EXPIRY_MILLIS = 30 * 60 * 1000L;

    /** map(token,{uuid,username,role,expiry}) */
    private final Map<String, SessionEntry> sessions;

    /** 随机源。 */
    private final RandomGen random = new RandomGen();

    /** 构造会话管理器。 */
    public SessionManager() {
        sessions = new ConcurrentHashMap<String, SessionEntry>();
    }

    /** 单例实例。 */
    private static SessionManager instance;

    /**
     * 获取全局会话管理器单例。
     *
     * @return 会话管理器
     */
    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    /**
     * 签发会话并返回 token（不采集姓名）。
     *
     * @param uuid 账户全局唯一标识
     * @param username 登录名
     * @param role 真实角色
     * @return 新 token
     */
    public String create(String uuid, String username, String role) {
        return create(uuid, username, null, role);
    }

    /**
     * 签发会话并返回 token。
     *
     * <p>
     * 姓名一并写进会话记录，客户端登录后立刻就有称呼可显示，不必为了一个姓名字段多跑一次
     * 请求；会话失效时姓名随之作废（它属于会话快照，不是权威档案）。
     *
     * @param uuid 账户全局唯一标识
     * @param username 登录名
     * @param displayName 姓名（可为 null）
     * @param role 真实角色
     * @return 新 token
     */
    public String create(String uuid, String username, String displayName, String role) {
        String token = random.randomHex(16);
        sessions.put(token, new SessionEntry(uuid, username, displayName, role,
                System.currentTimeMillis() + EXPIRY_MILLIS));
        return token;
    }

    /**
     * 校验 token：有效则刷新 TTL 并返回身份，无效返回 null。
     *
     * @param token 会话令牌
     * @return 会话记录；无效/过期返回 null
     */
    public SessionEntry validate(String token) {
        if (token == null) {
            // 未携带令牌：直接视为未登录；ConcurrentHashMap 不接受 null 键
            return null;
        }
        SessionEntry entry = sessions.get(token);
        if (entry == null) {// 没找到token
            return null;
        }
        if (entry.isExpired(System.currentTimeMillis())) {// 移除存在但过期的token
            sessions.remove(token);
            return null;
        }
        entry.renew(System.currentTimeMillis() + EXPIRY_MILLIS);// 更新有效期
        return entry;
    }

    /**
     * 注销：删除 token 会话。
     *
     * @param token 会话令牌
     */
    public void invalidate(String token) {
        if (token != null) {
            sessions.remove(token);
        }
    }
}