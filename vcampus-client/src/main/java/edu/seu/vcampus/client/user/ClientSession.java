package edu.seu.vcampus.client.user;

import edu.seu.vcampus.common.user.entity.SessionEntry;

/**
 * 客户端会话视图：只缓存服务端签发的 {@link SessionEntry} 与令牌。
 *
 * <p>
 * 用户名/角色/uuid 不在本类重复暴露，调用方直接读 {@link #getEntry()} 的 getter。
 */
public class ClientSession {

    /** 服务端签发的会话记录（登录响应下发）。 */
    private SessionEntry m_entry;

    /** 会话令牌（随每条请求的 {@code Message.token} 回传）。 */
    private String m_token;

    /**
     * 缓存登录结果。
     *
     * @param token 会话令牌
     * @param entry 服务端会话记录
     */
    public synchronized void cache(String token, SessionEntry entry) {
        this.m_token = token;
        this.m_entry = entry;
    }

    /** @return 会话令牌；未登录返回 null */
    public synchronized String getToken() {
        return m_token;
    }

    /** @return 服务端会话记录；未登录返回 null */
    public synchronized SessionEntry getEntry() {
        return m_entry;
    }

    /**
     * 取当前登录者的姓名。
     *
     * <p>
     * 服务端保证姓名非空（未采集时用登录名顶上），所以界面拿到就能直接显示，不需要回退分支。
     *
     * @return 姓名；未登录返回 null
     */
    public synchronized String getRealName() {
        SessionEntry entry = m_entry;
        return entry == null ? null : entry.getRealName();
    }

    /** @return 角色显示名（学生/教师/管理员）；未登录返回 null */
    public synchronized String getRole() {
        SessionEntry entry = m_entry;
        return entry == null ? null : entry.getRole();
    }

    /** @return 是否已登录（令牌与会话记录齐备） */
    public synchronized boolean isLoggedIn() {
        return m_token != null && m_entry != null;
    }

    /** 清空会话（登出或连接断开）。 */
    public synchronized void clear() {
        m_token = null;
        m_entry = null;
    }
}
