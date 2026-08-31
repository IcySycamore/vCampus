package edu.seu.vcampus.common.user.dto;

import java.io.Serializable;

/**
 * 登录请求载荷。
 *
 * <p>
 * 携带登录名、选定角色与 sha256(盐 + 密码) 的哈希值。
 */
public class LoginRequest implements Serializable {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 登录名。 */
    public String m_user_name;

    /** 选定角色。 */
    public String m_role;

    /** 密码哈希：sha256(盐 + 密码)。 */
    public String m_hashed;
}