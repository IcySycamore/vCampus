package edu.seu.vcampus.common.student.dto;

import edu.seu.vcampus.common.student.entity.ModifyRequestStatus;

import java.io.Serializable;

/**
 * 修改申请单的查询条件（207 命令）。
 *
 * <p>
 * 最典型的用法是只筛 {@link ModifyRequestStatus#PENDING} 取「待审列表」；不填状态则查全部，
 * 便于学生查看自己申请的历史结果。
 *
 * <p>
 * 与其它查询一样，<b>身份由服务端从会话解析</b>：教务视角看全部申请，学生视角由服务端收窄到
 * 自己提交的那些（不靠客户端传 uuid，避免伪造）。
 */
public class ModifyRequestQuery implements Serializable {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 申请单状态过滤（null 表示全部状态）。 */
    private ModifyRequestStatus m_status;

    /** 目标学籍记录主键过滤（null 表示不限）。 */
    private Long m_profile_id;

    /**
     * 申请人账户 uuid 过滤（null 表示不限）。
     *
     * <p>
     * <b>服务端专用</b>：在「学生看自己的申请」这条路上，服务端把它无条件<b>覆盖</b>成会话里的
     * uuid，客户端传什么都不作数（否则学生就能靠伪造申请人筛出别人的申请）；只有具备
     * {@code STUDENT_MODIFY_AUDIT} 的角色才能按申请人筛。与 201 查询的「我的轨」同一套做法。
     */
    private String m_applicant_uuid;

    /**
     * 关键词（null 或空白表示不过滤）。
     *
     * <p>
     * 与 208 的学籍关键词同思路：审批人手里只有一个搜索框，不会先声明「我在找单号还是找理由」，
     * 所以这里一次比对多条：申请单号、目标学籍主键、申请人 uuid、变更内容、申请理由。
     */
    private String m_keyword;

    /** 页码，从 1 开始。 */
    private int m_page_number = 1;

    /** 每页条数（服务端会截断到上限）。 */
    private int m_page_size = 20;

    /**
     * 构造一个空查询（查全部状态）。
     */
    public ModifyRequestQuery() {
    }

    /**
     * 构造按状态过滤的查询。
     *
     * @param status 申请单状态
     */
    public ModifyRequestQuery(ModifyRequestStatus status) {
        this.m_status = status;
    }

    /** @return 申请单状态过滤条件 */
    public ModifyRequestStatus getStatus() {
        return m_status;
    }

    /** @param status 申请单状态过滤条件 */
    public void setStatus(ModifyRequestStatus status) {
        this.m_status = status;
    }

    /** @return 目标学籍记录主键过滤条件 */
    public Long getProfileId() {
        return m_profile_id;
    }

    /** @param profileId 目标学籍记录主键过滤条件 */
    public void setProfileId(Long profileId) {
        this.m_profile_id = profileId;
    }

    /** @return 申请人账户 uuid 过滤条件；仅审核视角可自行设置 */
    public String getApplicantUuid() {
        return m_applicant_uuid;
    }

    /** @param applicantUuid 申请人账户 uuid 过滤条件（学生视角由服务端覆盖，传了也不作数） */
    public void setApplicantUuid(String applicantUuid) {
        this.m_applicant_uuid = applicantUuid;
    }

    /** @return 关键词 */
    public String getKeyword() {
        return m_keyword;
    }

    /** @param keyword 关键词（null 或空白表示不过滤） */
    public void setKeyword(String keyword) {
        this.m_keyword = keyword;
    }

    /** @return 页码（从 1 开始） */
    public int getPageNumber() {
        return m_page_number;
    }

    /** @param pageNumber 页码（从 1 开始） */
    public void setPageNumber(int pageNumber) {
        this.m_page_number = pageNumber;
    }

    /** @return 每页条数 */
    public int getPageSize() {
        return m_page_size;
    }

    /** @param pageSize 每页条数 */
    public void setPageSize(int pageSize) {
        this.m_page_size = pageSize;
    }
}
