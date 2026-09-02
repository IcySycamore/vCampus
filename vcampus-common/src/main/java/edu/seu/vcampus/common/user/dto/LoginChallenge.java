package edu.seu.vcampus.common.user.dto;

import java.io.Serializable;

/**
 * 登录挑战载荷（挑战-应答第②步，服务器回传）。
 *
 * <p>
 * 携带盐与一次性 nonce，客户端据此计算 proof。
 */
public class LoginChallenge implements Serializable {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 3L;

    /** 盐值。 */
    public String m_salt;

    /** 一次性随机数。 */
    public String m_nonce;
}