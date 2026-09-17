package edu.seu.vcampus.common.user.dto;

import java.io.Serializable;

/**
 * 登录请求载荷（挑战-应答第①步）。
 *
 * <p>
 * 只带登录名，请求服务器返回盐与一次性 nonce。这里<b>不带角色</b>：角色是账户的属性， 由服务端在登录成功时连同会话一起下发；客户端先声称一个角色再让服务端去比对，只会造出
 * 「我选错了身份」这种假故障，而且登录页上那个三选一根本不影响鉴权结果。
 */
public class LoginRequest implements Serializable {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 3L;

    /** 登录名。 */
    public String m_user_name;
}