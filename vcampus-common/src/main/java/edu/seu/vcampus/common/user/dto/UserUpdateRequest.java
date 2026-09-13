package edu.seu.vcampus.common.user.dto;

import java.io.Serializable;

/**
 * 编辑用户请求（命令 107）：只改可编辑属性，<b>不改角色</b>。
 *
 * <p>
 * 角色是授权结果，改动角色应走独立的管理命令（当前未开放），避免「编辑资料」顺带提权。
 */
public final class UserUpdateRequest implements Serializable {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 目标登录名（必填）。 */
    private final String m_user_name;

    /** 新姓名；null 或空表示不改。 */
    private final String m_display_name;

    /**
     * 构造编辑请求。
     *
     * @param userName 目标登录名
     * @param displayName 新姓名；null 不改
     */
    public UserUpdateRequest(String userName, String displayName) {
        this.m_user_name = userName;
        this.m_display_name = displayName;
    }

    /** @return 目标登录名 */
    public String getUserName() {
        return m_user_name;
    }

    /** @return 新姓名；null 表示不改 */
    public String getDisplayName() {
        return m_display_name;
    }
}
