package edu.seu.vcampus.common.user.dto;

import java.io.Serializable;

/**
 * 盐的响应载荷。
 *
 * <p>
 * 服务器返回的盐；客户端用它计算 {@code sha256(盐 + 密码)}。
 */
public class SaltResponse implements Serializable {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 盐值。 */
    public String m_salt;
}