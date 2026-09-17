package edu.seu.vcampus.server.bank;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;

/**
 * 银行密码凭据的持久化快照。
 *
 * <p>
 * 与 {@link BankCredential} 的区别：那个是驻留内存的校验对象，带失败计数与退避时间；本类只装
 * 落库需要的四样东西——盐、摘要、最近设置时间、挂失时间。失败计数属于进程内风控状态，重启后归零，
 * 不落库。
 */
public final class BankCredentialRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 盐；未设置密码时为 null。 */
    private final byte[] m_salt;

    /** 摘要 sha256(salt + 密码)；未设置密码时为 null。 */
    private final byte[] m_hash;

    /** 最近一次设置密码的时间。 */
    private final Date m_setAt;

    /** 挂失时间；null 表示未挂失。 */
    private final Date m_frozenAt;

    /**
     * 创建凭据快照。
     *
     * @param salt 盐，可为 null
     * @param hash 摘要，可为 null
     * @param setAt 设置时间，可为 null
     * @param frozenAt 挂失时间，可为 null
     */
    public BankCredentialRecord(byte[] salt, byte[] hash, Date setAt, Date frozenAt) {
        this.m_salt = salt == null ? null : salt.clone();
        this.m_hash = hash == null ? null : hash.clone();
        this.m_setAt = setAt == null ? null : new Date(setAt.getTime());
        this.m_frozenAt = frozenAt == null ? null : new Date(frozenAt.getTime());
    }

    /** @return 盐的副本；未设置时为 null */
    public byte[] getSalt() {
        return m_salt == null ? null : m_salt.clone();
    }

    /** @return 摘要的副本；未设置时为 null */
    public byte[] getHash() {
        return m_hash == null ? null : m_hash.clone();
    }

    /** @return 最近设置密码的时间；未知为 null */
    public Date getSetAt() {
        return m_setAt == null ? null : new Date(m_setAt.getTime());
    }

    /** @return 挂失时间；null 表示未挂失 */
    public Date getFrozenAt() {
        return m_frozenAt == null ? null : new Date(m_frozenAt.getTime());
    }

    /** @return 是否已经设置过银行密码 */
    public boolean hasPassword() {
        return m_salt != null && m_hash != null;
    }

    /**
     * 判断两个快照的密码是否一致（只比盐与摘要）。
     *
     * @param other 另一个快照，可为 null
     * @return 密码相同返回 true
     */
    public boolean samePassword(BankCredentialRecord other) {
        return other != null && Arrays.equals(m_salt, other.m_salt)
                && Arrays.equals(m_hash, other.m_hash);
    }
}
