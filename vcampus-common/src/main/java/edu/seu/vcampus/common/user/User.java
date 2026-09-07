package edu.seu.vcampus.common.user;

import java.io.Serializable;

/**
 * 用户账户实体（登录认证主体）。
 *
 * <p>
 * 主键由数据库自增分配（BIGINT），对应 tblUser.user_id；登录名唯一。
 */
public class User implements Serializable {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 3L;

    /** 自增主键（数据库分配，插入前为 null） */
    private Long m_id;

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

    /** @return 自增主键 */
    public Long getUserId() {
        return m_id;
    }

    /** @param id 自增主键 */
    public void setUserId(Long id) {
        this.m_id = id;
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
