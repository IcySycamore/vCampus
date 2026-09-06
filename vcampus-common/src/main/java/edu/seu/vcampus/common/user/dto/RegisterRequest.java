package edu.seu.vcampus.common.user.dto;

import java.io.Serializable;

/**
 * 注册请求载荷（管理员操作）。
 *
 * <p>
 * 携带登录名、角色与明文密码；服务器端生成盐并计算加盐哈希落库。
 */
public class RegisterRequest implements Serializable {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 3L;

    /** 登录名。 */
    public String m_user_name;

    /** 选定角色。 */
    public String m_role;

    /** 明文密码（管理员提供，服务器加盐哈希）。 */
    public String m_password;
}