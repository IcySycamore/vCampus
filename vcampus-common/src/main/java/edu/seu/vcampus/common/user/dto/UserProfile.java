package edu.seu.vcampus.common.user.dto;

import java.io.Serializable;

/**
 * 个人档案（命令 109 的回包）：当前登录者需要显示的身份信息。
 *
 * <p>
 * 只承载「界面要显示什么」——登录名、真实姓名、角色显示名；不含盐与哈希，密码永远不出库。
 *
 * <p>
 * 为什么不直接复用 {@link edu.seu.vcampus.common.user.entity.SessionEntry}：会话记录是<b>鉴权
 * 凭据</b>（带有效期与续期语义），档案是<b>展示数据</b>。两者生命周期不同——档案随时可以再查，
 * 会话一过期就作废；混在一起会让「查个姓名」这种纯只读操作被迫触碰会话有效期。所以登录时
 * 姓名随会话一起下发（首屏不必多一次往返），需要刷新时再走本 DTO 独立查询。
 */
public class UserProfile implements Serializable {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 账户全局唯一标识。 */
    private String m_uuid;

    /** 登录名。 */
    private String m_user_name;

    /** 真实姓名。服务端保证非空：未采集时用登录名顶上。 */
    private String m_real_name;

    /** 角色显示名（学生 / 教师 / 管理员）。 */
    private String m_role;

    /**
     * 构造空档案，供反序列化使用。
     */
    public UserProfile() {
    }

    /**
     * 构造个人档案。
     *
     * @param uuid     账户全局唯一标识
     * @param userName 登录名
     * @param realName 真实姓名（可为 null）
     * @param role     角色显示名
     */
    public UserProfile(String uuid, String userName, String realName, String role) {
        this.m_uuid = uuid;
        this.m_user_name = userName;
        this.m_real_name = realName;
        this.m_role = role;
    }

    /** @return 账户全局唯一标识 */
    public String getUuid() {
        return m_uuid;
    }

    /** @param uuid 账户全局唯一标识 */
    public void setUuid(String uuid) {
        this.m_uuid = uuid;
    }

    /** @return 登录名 */
    public String getUserName() {
        return m_user_name;
    }

    /** @param userName 登录名 */
    public void setUserName(String userName) {
        this.m_user_name = userName;
    }

    /** @return 真实姓名；未采集返回 null */
    public String getRealName() {
        return m_real_name;
    }

    /** @param realName 真实姓名 */
    public void setRealName(String realName) {
        this.m_real_name = realName;
    }

    /** @return 角色显示名 */
    public String getRole() {
        return m_role;
    }

    /** @param role 角色显示名 */
    public void setRole(String role) {
        this.m_role = role;
    }
}
