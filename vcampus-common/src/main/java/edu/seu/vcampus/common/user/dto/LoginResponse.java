package edu.seu.vcampus.common.user.dto;

import java.io.Serializable;

import edu.seu.vcampus.common.user.entity.SessionEntry;

/**
 * 登录成功回传载荷（挑战-应答第④步，服务器回传）。
 *
 * <p>
 * 不含密码；由会话令牌与服务器签发的 {@link SessionEntry} 组成，客户端缓存后者即可查询身份。
 */
public class LoginResponse implements Serializable {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 3L;

    /** 会话令牌；之后随每条请求的 {@code Message.token} 回传。 */
    public String m_token;

    /** 服务器签发的会话记录（uuid / 用户名 / 角色 / 有效期）。 */
    public SessionEntry m_session;
}