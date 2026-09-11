package edu.seu.vcampus.common.user.entity;

import java.io.Serializable;

/**
 * 用户账户实体（登录认证主体）。
 *
 * <p>
 * uuid 为账户全局唯一标识：注册时由服务端生成；所有业务模块以 uuid 引用该
 * 用户（各模块不重复存储档案，按需经接口访问）。登录名唯一。
 */
public class User implements Serializable {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 3L;

    /** 账户全局唯一标识（注册时由服务端生成）。 */
    private String m_uuid;

    /** 登录名 */
    private String m_user_name;

    /** 密码 */
    private transient String m_password;

    /** 角色。 */
    private Role m_role;

    /**
     * 构造一个空账户。
     */
    public User() {
    }

    /**
     * 构造并初始化用户账户。
     *
     * @param userName 登录名
     * @param password 密码
     * @param role     角色
     */
    public User(String userName, String password, Role role) {
        this.m_user_name = userName;
        this.m_password = password;
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

    /** @return 密码 */
    public String getPassword() {
        return m_password;
    }

    /** @param password 密码 */
    public void setPassword(String password) {
        this.m_password = password;
    }

    /** @return 角色 */
    public Role getRole() {
        return m_role;
    }

    /** @param role 角色 */
    public void setRole(Role role) {
        this.m_role = role;
    }
}
