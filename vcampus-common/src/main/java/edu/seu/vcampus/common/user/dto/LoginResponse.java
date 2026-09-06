package edu.seu.vcampus.common.user.dto;

import edu.seu.vcampus.common.user.HumanInfo;

import java.io.Serializable;

/**
 * 登录成功回传载荷（挑战-应答第④步，服务器回传）。
 *
 * <p>
 * 不含密码；由个人档案与真实角色组成；会话令牌由 {@code Message.token} 承载。
 */
public class LoginResponse implements Serializable {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 2L;

    /** 个人档案。 */
    public HumanInfo m_human_info;

    /** 真实角色 */
    public String m_role;
}