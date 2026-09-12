package edu.seu.vcampus.common.student.dto;

import java.io.Serializable;

/**
 * 注销学籍的请求体（205 命令）。
 *
 * <p>
 * 单独建一个 DTO 而不是直接传主键，是为了给后续扩展留位置（如注销原因、生效学期），
 * 也避免与 201 的「传一个 Long」在类型上混淆——两者 data 都是 Long 时极易接错。
 *
 * <p>
 * 注销是<b>软删除</b>：只置删除标记，记录仍保留，选课 / 成绩 / 借阅对 uuid 的引用不会悬空。
 */
public class StudentDeleteRequest implements Serializable {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 目标学籍记录主键。 */
    private Long m_profile_id;

    /** 注销原因（可选，便于留痕）。 */
    private String m_reason;

    /**
     * 构造一个空请求。
     */
    public StudentDeleteRequest() {
    }

    /**
     * 构造注销请求。
     *
     * @param profileId 目标学籍记录主键
     */
    public StudentDeleteRequest(Long profileId) {
        this.m_profile_id = profileId;
    }

    /** @return 目标学籍记录主键 */
    public Long getProfileId() {
        return m_profile_id;
    }

    /** @param profileId 目标学籍记录主键 */
    public void setProfileId(Long profileId) {
        this.m_profile_id = profileId;
    }

    /** @return 注销原因 */
    public String getReason() {
        return m_reason;
    }

    /** @param reason 注销原因 */
    public void setReason(String reason) {
        this.m_reason = reason;
    }
}
