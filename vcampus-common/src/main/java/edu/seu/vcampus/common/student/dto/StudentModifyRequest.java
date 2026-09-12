package edu.seu.vcampus.common.student.dto;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 学籍修改申请（202 命令的请求体）。
 *
 * <p>
 * 与持久化实体 {@code common.student.entity.StudentModifyRequest} <b>同名但不同包</b>：
 * 本类是学生提交的「动作入参」，只含目标记录、要改的字段与理由，<b>不含状态、审核人、审核时间</b>
 * ——这些由服务端独占维护，客户端无从伪造。
 *
 * <p>
 * 202 的语义是「提交申请」而非直接改学籍：服务端只落一条待审记录，203 通过后才把
 * {@link #m_changes} 应用到学籍上，以体现「学生申请 → 教务审核」的业务流程。
 */
public class StudentModifyRequest implements Serializable {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 目标学籍记录主键。 */
    private Long m_profile_id;

    /** 要修改的字段：字段名 → 新值（只放允许学生申请的字段）。 */
    private Map<String, String> m_changes = new LinkedHashMap<String, String>();

    /** 申请理由。 */
    private String m_reason;

    /**
     * 构造一个空申请。
     */
    public StudentModifyRequest() {
    }

    /**
     * 构造一份申请。
     *
     * @param profileId 目标学籍记录主键
     * @param changes   要修改的字段（字段名 → 新值）
     * @param reason    申请理由
     */
    public StudentModifyRequest(Long profileId, Map<String, String> changes,
            String reason) {
        this.m_profile_id = profileId;
        this.m_reason = reason;
        if (changes != null) {
            this.m_changes = new LinkedHashMap<String, String>(changes);
        }
    }

    /** @return 目标学籍记录主键 */
    public Long getProfileId() {
        return m_profile_id;
    }

    /** @param profileId 目标学籍记录主键 */
    public void setProfileId(Long profileId) {
        this.m_profile_id = profileId;
    }

    /** @return 要修改的字段（字段名 → 新值；非 null） */
    public Map<String, String> getChanges() {
        return m_changes;
    }

    /** @param changes 要修改的字段（字段名 → 新值） */
    public void setChanges(Map<String, String> changes) {
        this.m_changes = changes == null
                ? new LinkedHashMap<String, String>()
                : new LinkedHashMap<String, String>(changes);
    }

    /** @return 申请理由 */
    public String getReason() {
        return m_reason;
    }

    /** @param reason 申请理由 */
    public void setReason(String reason) {
        this.m_reason = reason;
    }
}
