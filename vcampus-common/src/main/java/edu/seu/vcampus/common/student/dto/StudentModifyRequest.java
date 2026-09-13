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

    /**
     * 允许申请修改的字段：入校年份（学生入学 / 教师入职），十进制整数字符串。
     *
     * <p>
     * 三个 {@code FIELD_*} 常量是 202 的白名单字段名，<b>客户端与服务端共用这一份</b>：
     * 服务端校验白名单，客户端按同样的名字组装 {@link #m_changes}。名字写歪了服务端会
     * 跳过该字段，申请就变成空单，所以不能各写一份字符串。
     */
    public static final String FIELD_JOIN_YEAR = "joinYear";

    /** 允许申请修改的字段：在校状态（值为 {@code CampusStatus} 的枚举名，如 {@code SUSPENDED}）。 */
    public static final String FIELD_STATUS = "status";

    /** 允许申请修改的字段：学术方向（学生专业 / 教师研究方向）。 */
    public static final String FIELD_FIELD = "field";

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
