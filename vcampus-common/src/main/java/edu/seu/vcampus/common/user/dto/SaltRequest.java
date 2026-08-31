package edu.seu.vcampus.common.user.dto;

import java.io.Serializable;

/**
 * 获取盐的请求载荷。
 *
 * <p>
 * 客户端在登录/注册前先发送本对象，请求服务器返回用于计算密码哈希的盐。
 */
public class SaltRequest implements Serializable {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 登录名。 */
    public String m_user_name;

    /** 动作：LOGIN / REGISTER。 */
    public String m_action;
}
