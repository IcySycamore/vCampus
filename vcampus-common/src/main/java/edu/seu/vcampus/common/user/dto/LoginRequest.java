package edu.seu.vcampus.common.user.dto;

import java.io.Serializable;

/**
 * 登录请求载荷（挑战-应答第①步）。
 *
 * <p>
 * 携带登录名与选定角色，请求服务器返回盐与一次性 nonce。
 */
public class LoginRequest implements Serializable {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 3L;

    /** 登录名。 */
    public String m_user_name;

    /** 选定角色。 */
    public String m_role;
}