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
