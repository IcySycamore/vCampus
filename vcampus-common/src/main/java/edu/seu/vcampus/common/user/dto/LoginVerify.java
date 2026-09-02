package edu.seu.vcampus.common.user.dto;

import java.io.Serializable;

/**
 * 登录验证载荷（挑战-应答第③步，客户端回传）。
 *
 * <p>
 * 携带登录名与 proof = sha256(nonce + sha256(salt + 密码))。
 */
public class LoginVerify implements Serializable {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 3L;

    /** 登录名。 */
    public String m_user_name;

    /** 证明值。 */
    public String m_proof;
}