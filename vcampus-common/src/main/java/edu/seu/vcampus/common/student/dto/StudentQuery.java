package edu.seu.vcampus.common.student.dto;

import edu.seu.vcampus.common.student.entity.EnrollmentStatus;
import edu.seu.vcampus.common.student.entity.PersonCategory;

import java.io.Serializable;

/**
 * 学籍查询条件：同一命令码（201 / 208）承载多种查法，故把条件集中在一个 DTO 里。
 *
 * <p>
 * 用法：
 * <ul>
 * <li><b>201 查单条</b>：填 {@link #m_profile_id}（管理端按主键）或 {@link #m_user_uuid}
 * （按账户），都不填时服务端按会话取「自己的」；</li>
 * <li><b>208 查列表</b>：填 {@link #m_keyword} / {@link #m_status} 作为过滤条件，
 * 再用 {@link #m_page_number} / {@link #m_page_size} 分页。</li>
 * </ul>
 *
 * <p>
 * 注意身份由服务端从会话解析，<b>本对象不携带「我是谁」</b>；管理端要查他人时显式传目标标识，
 * 服务端按能力准入后才会放行（越权返回 403，不会静默降级成「只返回自己的」）。
 */
public class StudentQuery implements Serializable {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 学籍记录主键（201 精确查询；null 表示不使用该条件）。 */
    private Long m_profile_id;

    /** 账户 uuid（201 按用户查询；null 表示不使用该条件）。 */
    private String m_user_uuid;

    /** 关键字（208 列表查询，按 uuid / 入学年份做模糊匹配；null 或空表示不过滤）。 */
    private String m_keyword;

    /** 学籍状态过滤（208；null 表示全部状态）。 */
    private EnrollmentStatus m_status;

    /** 人员类别过滤（208；null 表示师生都查）。 */
    private PersonCategory m_person_category;

    /** 页码，从 1 开始。 */
    private int m_page_number = 1;

    /** 每页条数（服务端会截断到上限）。 */
    private int m_page_size = 20;

    /**
     * 构造一个空查询（201 按会话查自己）。
     */
    public StudentQuery() {
    }

    /**
     * 构造按主键的精确查询（管理端）。
     *
     * @param profileId 学籍记录主键
     * @return 查询条件
     */
    public static StudentQuery byProfileId(Long profileId) {
        StudentQuery query = new StudentQuery();
        query.m_profile_id = profileId;
        return query;
    }

    /**
     * 构造按账户 uuid 的查询。
     *
     * @param userUuid 账户 uuid
     * @return 查询条件
     */
    public static StudentQuery byUserUuid(String userUuid) {
        StudentQuery query = new StudentQuery();
        query.m_user_uuid = userUuid;
        return query;
    }

    /** @return 学籍记录主键 */
    public Long getProfileId() {
        return m_profile_id;
    }

    /** @param profileId 学籍记录主键 */
    public void setProfileId(Long profileId) {
        this.m_profile_id = profileId;
    }

    /** @return 账户 uuid */
    public String getUserUuid() {
        return m_user_uuid;
    }

    /** @param userUuid 账户 uuid */
    public void setUserUuid(String userUuid) {
        this.m_user_uuid = userUuid;
    }

    /** @return 关键字 */
    public String getKeyword() {
        return m_keyword;
    }

    /** @param keyword 关键字 */
    public void setKeyword(String keyword) {
        this.m_keyword = keyword;
    }

    /** @return 学籍状态过滤条件 */
    public EnrollmentStatus getStatus() {
        return m_status;
    }

    /** @param status 学籍状态过滤条件 */
    public void setStatus(EnrollmentStatus status) {
        this.m_status = status;
    }

    /** @return 人员类别过滤条件 */
    public PersonCategory getPersonCategory() {
        return m_person_category;
    }

    /** @param category 人员类别过滤条件（null 表示师生都查） */
    public void setPersonCategory(PersonCategory category) {
        this.m_person_category = category;
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
