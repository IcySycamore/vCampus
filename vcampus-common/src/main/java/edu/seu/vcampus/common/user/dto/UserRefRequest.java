package edu.seu.vcampus.common.user.dto;

import java.io.Serializable;

/**
 * 按登录名定位用户的最小请求载荷（命令 104 注销、105 批量注销用其列表形式）。
 *
 * <p>
 * 单独建类而不是直接用裸 {@code String}：命令的载荷类型必须自描述， 否则处理器只能靠 {@code ClassCastException}
 * 兜底（既有图书馆模块的教训）。
 */
public final class UserRefRequest implements Serializable {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 目标登录名。 */
    private final String m_user_name;

    /**
     * 构造请求。
     *
     * @param userName 目标登录名
     */
    public UserRefRequest(String userName) {
        this.m_user_name = userName;
    }

    /** @return 目标登录名 */
    public String getUserName() {
        return m_user_name;
    }
}
