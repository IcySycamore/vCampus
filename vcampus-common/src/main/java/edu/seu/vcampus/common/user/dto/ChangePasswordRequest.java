package edu.seu.vcampus.common.user.dto;

import java.io.Serializable;

/**
 * 修改密码请求（命令 109），同时覆盖「我的轨」与「管理轨」：
 *
 * <ul>
 * <li><b>本人改密</b>：{@code userName} 留空（服务端取会话登录名），必须携带 {@code proof}
 * 校验旧密码——proof 用与登录相同的挑战-应答算法，旧密码不上线；</li>
 * <li><b>管理员重置</b>：{@code userName} 填目标账号，需 {@code USER_MANAGE} 能力，无需
 * proof。</li>
 * </ul>
 *
 * <p>
 * 新密码由客户端生成新盐并只提交 {@code sha256(newSalt + 新密码)}，服务端直接落库为新的 H， 全程没有明文密码出现在协议里。
 */
public final class ChangePasswordRequest implements Serializable {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 目标登录名；留空表示改本人密码。 */
    private final String m_user_name;

    /** 旧密码证明 {@code sha256(nonce + sha256(salt + 旧密码))}；管理员重置时留空。 */
    private final String m_proof;

    /** 客户端生成的新盐（十六进制）。 */
    private final String m_new_salt;

    /** 新密码哈希 {@code sha256(newSalt + 新密码)}。 */
    private final String m_new_hash;

    /**
     * 构造修改密码请求。
     *
     * @param userName 目标登录名；null 或空表示本人
     * @param proof 旧密码证明；管理员重置时可为 null
     * @param newSalt 客户端生成的新盐
     * @param newHash 新密码哈希
     */
    public ChangePasswordRequest(String userName, String proof, String newSalt, String newHash) {
        this.m_user_name = userName;
        this.m_proof = proof;
        this.m_new_salt = newSalt;
        this.m_new_hash = newHash;
    }

    /** @return 目标登录名；null 表示本人 */
    public String getUserName() {
        return m_user_name;
    }

    /** @return 旧密码证明；管理员重置时为 null */
    public String getProof() {
        return m_proof;
    }

    /** @return 新盐 */
    public String getNewSalt() {
        return m_new_salt;
    }

    /** @return 新密码哈希 */
    public String getNewHash() {
        return m_new_hash;
    }
}
