package edu.seu.vcampus.common.user.dto;

import edu.seu.vcampus.common.user.HumanInfo;

import java.io.Serializable;
import java.util.Map;

/**
 * 登录成功回传载荷。
 *
 * <p>
 * 不含密码；由个人档案、真实角色与角色额外信息组成。
 */
public class LoginResult implements Serializable {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 个人档案。 */
    public HumanInfo m_human_info;

    /** 真实角色 */
    public String m_role;

    /** 各角色额外信息，如学生 {学号, 院系}。 */
    public Map<String, Object> m_extra_info;
}