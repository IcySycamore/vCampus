package edu.seu.vcampus.common.user.dto;

import java.io.Serializable;

/**
 * 启用/禁用用户请求（命令 108）。
 */
public final class UserEnabledRequest implements Serializable {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 目标登录名（必填）。 */
    private final String m_user_name;

    /** 目标状态：true 启用、false 禁用。 */
    private final boolean m_enabled;

    /**
     * 构造启用/禁用请求。
     *
     * @param userName 目标登录名
     * @param enabled 目标状态
     */
    public UserEnabledRequest(String userName, boolean enabled) {
        this.m_user_name = userName;
        this.m_enabled = enabled;
    }

    /** @return 目标登录名 */
    public String getUserName() {
        return m_user_name;
    }

    /** @return 目标状态 */
    public boolean isEnabled() {
        return m_enabled;
    }
}
