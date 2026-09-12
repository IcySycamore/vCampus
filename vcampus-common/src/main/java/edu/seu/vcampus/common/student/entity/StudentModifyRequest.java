package edu.seu.vcampus.common.student.entity;

import java.io.Serializable;

/**
 * 学籍修改申请单：学生提交的「要改哪些字段」的请求，等待教务审核。
 *
 * <p>
 * 与 202 命令的请求体 {@code common.student.dto.StudentModifyRequest} <b>同名但不同包</b>：
 * 那一个是「提交动作」的入参（只含 profileId / changes / reason），本类是一条持久化的申请记录，
 * 额外带有申请单主键、状态、审核人与时间戳。两者刻意分开，是因为前者由客户端构造、
 * 后者由服务端维护，混成一个类会让客户端能伪造状态与审核信息。
 *
 * <p>
 * 变更内容以 {@link #m_changes_json}（{@code 字段=新值} 的键值文本，多项以 {@code ;} 分隔）保存，
 * 避免为一条申请单引入 JSON 依赖；审核通过时由服务层解析并应用到 {@link StudentProfile}。
 */
public class StudentModifyRequest implements Serializable {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 多项变更之间的分隔符。 */
    public static final String ENTRY_SEPARATOR = ";";

    /** 单个变更内「字段名」与「新值」之间的分隔符。 */
    public static final String KEY_VALUE_SEPARATOR = "=";

    /** 申请单主键（插入前为 null，由存储分配）。 */
    private Long m_request_id;

    /** 目标学籍记录主键。 */
    private Long m_profile_id;

    /** 申请人（学生）账户 uuid。 */
    private String m_applicant_uuid;

    /** 变更内容（键值文本），例如 {@code enrollYear=2028}。 */
    private String m_changes_json;

    /** 申请理由。 */
    private String m_reason;

    /** 申请单状态。 */
    private ModifyRequestStatus m_status;

    /** 审核意见（驳回时通常写明原因）。 */
    private String m_comment;

    /** 提交时间（毫秒时间戳）。 */
    private long m_applied_at;

    /** 审核人账户 uuid；未审核时为 null。 */
    private String m_audited_by;

    /** 审核时间（毫秒时间戳）；未审核时为 0。 */
    private long m_audited_at;

    /**
     * 构造一条空申请单。
     */
    public StudentModifyRequest() {
    }

    /**
     * 构造一条待审核的申请单。
     *
     * @param profileId     目标学籍记录主键
     * @param applicantUuid 申请人账户 uuid
     * @param changesJson   变更内容（键值文本）
     * @param reason        申请理由
     */
    public StudentModifyRequest(Long profileId, String applicantUuid,
            String changesJson, String reason) {
        this.m_profile_id = profileId;
        this.m_applicant_uuid = applicantUuid;
        this.m_changes_json = changesJson;
        this.m_reason = reason;
        this.m_status = ModifyRequestStatus.PENDING;
    }

    /** @return 申请单主键 */
    public Long getRequestId() {
        return m_request_id;
    }

    /** @param requestId 申请单主键 */
    public void setRequestId(Long requestId) {
        this.m_request_id = requestId;
    }

    /** @return 目标学籍记录主键 */
    public Long getProfileId() {
        return m_profile_id;
    }

    /** @param profileId 目标学籍记录主键 */
    public void setProfileId(Long profileId) {
        this.m_profile_id = profileId;
    }

    /** @return 申请人账户 uuid */
    public String getApplicantUuid() {
        return m_applicant_uuid;
    }

    /** @param applicantUuid 申请人账户 uuid */
    public void setApplicantUuid(String applicantUuid) {
        this.m_applicant_uuid = applicantUuid;
    }

    /** @return 变更内容（键值文本） */
    public String getChangesJson() {
        return m_changes_json;
    }

    /** @param changesJson 变更内容（键值文本） */
    public void setChangesJson(String changesJson) {
        this.m_changes_json = changesJson;
    }

    /** @return 申请理由 */
    public String getReason() {
        return m_reason;
    }

    /** @param reason 申请理由 */
    public void setReason(String reason) {
        this.m_reason = reason;
    }

    /** @return 申请单状态 */
    public ModifyRequestStatus getStatus() {
        return m_status;
    }

    /** @param status 申请单状态 */
    public void setStatus(ModifyRequestStatus status) {
        this.m_status = status;
    }

    /** @return 审核意见 */
    public String getComment() {
        return m_comment;
    }

    /** @param comment 审核意见 */
    public void setComment(String comment) {
        this.m_comment = comment;
    }

    /** @return 提交时间（毫秒） */
    public long getAppliedAt() {
        return m_applied_at;
    }

    /** @param appliedAt 提交时间（毫秒） */
    public void setAppliedAt(long appliedAt) {
        this.m_applied_at = appliedAt;
    }

    /** @return 审核人账户 uuid */
    public String getAuditedBy() {
        return m_audited_by;
    }

    /** @param auditedBy 审核人账户 uuid */
    public void setAuditedBy(String auditedBy) {
        this.m_audited_by = auditedBy;
    }

    /** @return 审核时间（毫秒） */
    public long getAuditedAt() {
        return m_audited_at;
    }

    /** @param auditedAt 审核时间（毫秒） */
    public void setAuditedAt(long auditedAt) {
        this.m_audited_at = auditedAt;
    }
}
