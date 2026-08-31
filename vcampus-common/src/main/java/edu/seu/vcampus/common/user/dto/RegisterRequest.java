package edu.seu.vcampus.common.user.dto;

import java.io.Serializable;
import java.util.Map;

/**
 * 注册请求载荷（轻量 DTO）。
 *
 * <p>
 * 携带登录名、角色、密码哈希及各角色额外信息。
 */
public class RegisterRequest implements Serializable {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 登录名。 */
    public String m_user_name;

    /** 选定角色。 */
    public String m_role;

    /** 密码哈希：sha256 */
    public String m_hashed;

    /** 各角色额外信息，如学生 {学号, 院系}。 */
    public Map<String, Object> m_extra_info;
}