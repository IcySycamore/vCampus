package edu.seu.vcampus.server.auth;

import edu.seu.vcampus.common.random.RandomGen;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录会话管理
 *
 * <p>
 * 登录成功后签发 token；之后每条请求携带 token，服务器按 token 解析身份。
 * token 30 分钟滑动过期，每次校验刷新 TTL，登出删除。线程安全。
 */
public class SessionManager {

    /** 过期时长（毫秒）：30 分钟。 */
    private static final long EXPIRY_MILLIS = 30 * 60 * 1000L;

    /** token → 会话记录。 */
    private final Map<String, SessionEntry> sessions = new ConcurrentHashMap<String, SessionEntry>();

    /** 随机源。 */
    private final RandomGen random = new RandomGen();

    /**
     * 签发会话并返回 token。
     *
     * @param username 真实用户名
     * @param role     真实角色
     * @return 新 token
     */
    public String create(String username, String role) {
        String token = random.randomHex(16);
        sessions.put(token, new SessionEntry(username, role,
                System.currentTimeMillis() + EXPIRY_MILLIS));
        return token;
    }

    /**
     * 校验 token：有效则刷新 TTL并返回身份，无效返回 null。
     *
     * @param token 会话令牌
     * @return 会话记录；无效/过期返回 null
     */
    public SessionEntry validate(String token) {
        SessionEntry entry = sessions.get(token);
        if (entry == null) {// 没找到token
            return null;
        }
        if (System.currentTimeMillis() > entry.expiry) {// 移除存在但过期的token
            sessions.remove(token);
            return null;
        }
        entry.expiry = System.currentTimeMillis() + EXPIRY_MILLIS;// 更新有效期
        return entry;
    }

    /**
     * 注销：删除 token 会话。
     *
     * @param token 会话令牌
     */
    public void invalidate(String token) {
        sessions.remove(token);
    }

    /**
     * 会话记录
     */
    public static final class SessionEntry {
        private final String username;
        private final String role;
        private long expiry;// 有效期

        SessionEntry(String username, String role, long expiry) {
            this.username = username;
            this.role = role;
            this.expiry = expiry;
        }

        /** @return 真实用户名 */
        public String getUsername() {
            return username;
        }

        /** @return 真实角色 */
        public String getRole() {
            return role;
        }
    }
}