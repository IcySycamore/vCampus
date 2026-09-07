package edu.seu.vcampus.server.thread;

import edu.seu.vcampus.common.random.RandomGen;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务端会话管理器：负责签发、校验、续期和注销登录会话。
 */
public class SessionManager {
    private static final long DEFAULT_SESSION_TIMEOUT_MILLIS = 30L * 60L * 1000L;
    private static final int TOKEN_BYTES = 32;

    private static class Holder {
        private static final SessionManager INSTANCE =
                new SessionManager(DEFAULT_SESSION_TIMEOUT_MILLIS);
    }

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final long sessionTimeoutMillis;
    private final RandomGen randomGen = new RandomGen();

    /**
     * 使用默认过期时间创建会话管理器。
     */
    public SessionManager() {
        this(DEFAULT_SESSION_TIMEOUT_MILLIS);
    }

    /**
     * 创建指定过期时间的会话管理器。
     * @param sessionTimeoutMillis 会话滑动过期时间
     */
    public SessionManager(long sessionTimeoutMillis) {
        if (sessionTimeoutMillis <= 0) {
            throw new IllegalArgumentException("sessionTimeoutMillis must be greater than zero");
        }
        this.sessionTimeoutMillis = sessionTimeoutMillis;
    }

    /**
     * 获取全局会话管理器。
     * @return 全局唯一实例
     */
    public static SessionManager getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * 为登录成功的用户签发新 token。
     * @param userId 用户唯一标识
     * @param role 用户角色
     * @return 新会话 token
     */
    public String createSession(String userId, String role) {
        requireText(userId, "userId");
        requireText(role, "role");
        String token;
        do {
            token = randomGen.randomHex(TOKEN_BYTES);
        } while (sessions.containsKey(token));
        long now = System.currentTimeMillis();
        sessions.put(token, new Session(userId, role, now));
        return token;
    }

    /**
     * 校验 token 并刷新最后访问时间。
     * @param token 客户端提交的 token
     * @return token 是否有效
     */
    public boolean isValid(String token) {
        return getSession(token) != null;
    }

    /**
     * 校验 token，兼容工作线程鉴权调用。
     * @param token 客户端提交的 token
     * @return token 是否有效
     */
    public boolean validateToken(String token) {
        return isValid(token);
    }

    /**
     * 获取有效会话并刷新其过期时间。
     * @param token 客户端提交的 token
     * @return 有效会话；无效或过期时返回 null
     */
    public Session getSession(String token) {
        if (token == null || token.length() == 0) {
            return null;
        }
        Session session = sessions.get(token);
        if (session == null) {
            return null;
        }
        long now = System.currentTimeMillis();
        if (now - session.getLastAccessTime() >= sessionTimeoutMillis) {
            sessions.remove(token, session);
            return null;
        }
        session.touch(now);
        return session;
    }

    /**
     * 注销指定 token，使其立即失效。
     * @param token 要注销的 token
     * @return 是否存在并成功删除会话
     */
    public boolean revoke(String token) {
        return token != null && sessions.remove(token) != null;
    }

    /**
     * 删除所有已经过期的会话。
     * @return 本次清理的会话数量
     */
    public int cleanupExpiredSessions() {
        long now = System.currentTimeMillis();
        int removed = 0;
        for (Map.Entry<String, Session> entry : sessions.entrySet()) {
            Session session = entry.getValue();
            if (now - session.getLastAccessTime() >= sessionTimeoutMillis
                    && sessions.remove(entry.getKey(), session)) {
                removed++;
            }
        }
        return removed;
    }

    /**
     * 返回当前会话数量。
     * @return 会话数量
     */
    public int size() {
        return sessions.size();
    }

    private void requireText(String value, String name) {
        if (value == null || value.length() == 0) {
            throw new IllegalArgumentException(name + " must not be empty");
        }
    }

    /**
     * 单个登录会话的服务端身份快照。
     */
    public static final class Session {
        private final String userId;
        private final String role;
        private volatile long lastAccessTime;

        private Session(String userId, String role, long lastAccessTime) {
            this.userId = userId;
            this.role = role;
            this.lastAccessTime = lastAccessTime;
        }

        /**
         * 获取用户标识。
         * @return 用户唯一标识
         */
        public String getUserId() {
            return userId;
        }

        /**
         * 获取用户角色。
         * @return 用户角色
         */
        public String getRole() {
            return role;
        }

        /**
         * 获取最后访问时间。
         * @return 最后访问时间
         */
        public long getLastAccessTime() {
            return lastAccessTime;
        }

        private void touch(long timestamp) {
            lastAccessTime = timestamp;
        }
    }
}