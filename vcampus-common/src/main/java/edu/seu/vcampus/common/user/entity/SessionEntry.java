package edu.seu.vcampus.common.user.entity;

import java.io.Serializable;

/**
 * 会话记录：登录成功后由服务器签发，客户端本地缓存同一份用于查询身份。
 *
 * <p>字段与服务器 token 池中的记录完全一致，两端共用同一模型，避免各自再造一套身份字段；
 * 随登录响应下发时需可序列化。
 */
public class SessionEntry implements Serializable {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 2L;

    /** 账户全局唯一标识。 */
    private final String m_uuid;

    /** 登录名。 */
    private final String m_username;

    /** 真实姓名（界面显示用）。管理员账号不采集此项，允许为 null。 */
    private final String m_real_name;

    /** 真实角色（以服务端为准）。 */
    private final String m_role;

    /** 会话有效期（绝对时间，毫秒）。 */
    private long m_expiry;

    /**
     * 构造会话记录（不采集姓名）。
     *
     * @param uuid     账户全局唯一标识
     * @param username 登录名
     * @param role     真实角色
     * @param expiry   有效期（绝对时间，毫秒）
     */
    public SessionEntry(String uuid, String username, String role, long expiry) {
        this(uuid, username, null, role, expiry);
    }

    /**
     * 构造会话记录。
     *
     * @param uuid     账户全局唯一标识
     * @param username 登录名
     * @param realName 真实姓名（可为 null）
     * @param role     真实角色
     * @param expiry   有效期（绝对时间，毫秒）
     */
    public SessionEntry(String uuid, String username, String realName, String role,
            long expiry) {
        this.m_uuid = uuid;
        this.m_username = username;
        this.m_real_name = realName;
        this.m_role = role;
        this.m_expiry = expiry;
    }

    /** @return 账户全局唯一标识 */
    public String getUuid() {
        return m_uuid;
    }

    /** @return 登录名 */
    public String getUsername() {
        return m_username;
    }

    /** @return 真实姓名；未采集返回 null */
    public String getRealName() {
        return m_real_name;
    }

    /**
     * 取界面显示用的称呼：优先真实姓名，未采集时回退登录名。
     *
     * @return 显示名；两者都为空时返回空串
     */
    public String getDisplayName() {
        if (m_real_name != null && m_real_name.trim().length() > 0) {
            return m_real_name.trim();
        }
        return m_username == null ? "" : m_username;
    }

    /** @return 真实角色 */
    public String getRole() {
        return m_role;
    }

    /** @return 会话有效期（绝对时间，毫秒） */
    public long getExpiry() {
        return m_expiry;
    }

    /**
     * 判断会话是否已过期。
     *
     * @param nowMillis 当前时间，毫秒
     * @return 已过期返回 true
     */
    public boolean isExpired(long nowMillis) {
        return nowMillis > m_expiry;
    }

    /**
     * 滑动续期（仅服务器在校验通过后调用）。
     *
     * @param expiry 新的有效期（绝对时间，毫秒）
     */
    public void renew(long expiry) {
        this.m_expiry = expiry;
    }
}
