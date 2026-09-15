package edu.seu.vcampus.common.student.dto;

import java.io.Serializable;

/**
 * 学籍修改申请的审核请求（命令 203 的请求体）。
 *
 * <p>
 * 审核人是「谁在审核」，由服务端按会话解析，不放在请求体里，避免客户端伪造审核人。
 * 本类只承载「审哪一条、通不通过、意见是什么」。
 */
public class ModifyAuditRequest implements Serializable {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 目标申请单主键。 */
    private Long m_request_id;

    /** 是否通过（true 通过、false 驳回）。 */
    private boolean m_approved;

    /** 审核意见（可为 null）。 */
    private String m_comment;

    /**
     * 构造空请求，供反序列化使用。
     */
    public ModifyAuditRequest() {
    }

    /**
     * 构造审核请求。
     *
     * @param requestId 目标申请单主键
     * @param approved 是否通过
     * @param comment 审核意见
     */
    public ModifyAuditRequest(Long requestId, boolean approved, String comment) {
        this.m_request_id = requestId;
        this.m_approved = approved;
        this.m_comment = comment;
    }

    /**
     * 获取目标申请单主键。
     *
     * @return 申请单主键
     */
    public Long getRequestId() {
        return m_request_id;
    }

    /**
     * 设置目标申请单主键。
     *
     * @param requestId 申请单主键
     */
    public void setRequestId(Long requestId) {
        this.m_request_id = requestId;
    }

    /**
     * 是否通过。
     *
     * @return true 通过、false 驳回
     */
    public boolean isApproved() {
        return m_approved;
    }

    /**
     * 设置是否通过。
     *
     * @param approved 是否通过
     */
    public void setApproved(boolean approved) {
        this.m_approved = approved;
    }

    /**
     * 获取审核意见。
     *
     * @return 审核意见
     */
    public String getComment() {
        return m_comment;
    }

    /**
     * 设置审核意见。
     *
     * @param comment 审核意见
     */
    public void setComment(String comment) {
        this.m_comment = comment;
    }
}
