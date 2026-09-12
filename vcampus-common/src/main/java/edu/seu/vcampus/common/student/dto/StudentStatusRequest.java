package edu.seu.vcampus.common.student.dto;

import edu.seu.vcampus.common.student.entity.CampusStatus;

import java.io.Serializable;

/**
 * 修改学籍状态的请求体（206 命令）。
 *
 * <p>
 * 只携带「改哪条记录、改成什么状态」两个字段。用专门的 DTO 而不是复用
 * {@code StudentProfile}，是为了避免客户端顺带把入学年份等字段一并覆盖——
 * 状态变更不应改动其他字段。
 */
public class StudentStatusRequest implements Serializable {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 目标学籍记录主键。 */
    private Long m_profile_id;

    /** 目标状态（在读 / 休学 / 退学 / 毕业）。 */
    private CampusStatus m_status;

    /**
     * 构造一个空请求。
     */
    public StudentStatusRequest() {
    }

    /**
     * 构造改状态请求。
     *
     * @param profileId 目标学籍记录主键
     * @param status    目标状态
     */
    public StudentStatusRequest(Long profileId, CampusStatus status) {
        this.m_profile_id = profileId;
        this.m_status = status;
    }

    /** @return 目标学籍记录主键 */
    public Long getProfileId() {
        return m_profile_id;
    }

    /** @param profileId 目标学籍记录主键 */
    public void setProfileId(Long profileId) {
        this.m_profile_id = profileId;
    }

    /** @return 目标状态 */
    public CampusStatus getStatus() {
        return m_status;
    }

    /** @param status 目标状态 */
    public void setStatus(CampusStatus status) {
        this.m_status = status;
    }
}
