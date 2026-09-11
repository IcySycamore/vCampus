package edu.seu.vcampus.common.user.dto;

import java.io.Serializable;

/**
 * 登录成功回传载荷（挑战-应答第④步，服务器回传）。
 *
 * <p>
 * 不含密码；由真实角色与会话令牌组成；令牌由 {@code Message.token} 承载。
 */
public class LoginResponse implements Serializable {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 2L;

    /** 会话令牌。 */
    public String m_token;

    /** 真实角色。 */
    public String m_role;
}