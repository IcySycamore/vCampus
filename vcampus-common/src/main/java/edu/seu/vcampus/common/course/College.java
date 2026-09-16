package edu.seu.vcampus.common.course;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * 学院（值对象）：开设课程、拥有研究方向与专业，并与教师建立双向索引。
 *
 * <p>研究方向与专业统一为 {@link Field}（见 Field 类说明）。学院只索引教师
 * （{@code m_teacher_uuids}），不索引学生——学生通过「学院 + 专业」字段归属到学院，
 * 由选课规则动态判定，无需在学院侧维护学生列表。
 */
public class College implements Serializable {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 学院唯一标识。 */
    private String m_uuid;

    /** 学院名称。 */
    private String m_name;

    /** 学院官网（extra）。 */
    private String m_website;

    /** 学院介绍（extra）。 */
    private String m_description;

    /** 研究方向（供教师匹配开课）。 */
    private Set<Field> m_research_directions = new HashSet<Field>();

    /** 专业（供学生匹配选课）。 */
    private Set<Field> m_majors = new HashSet<Field>();

    /** 与教师双向索引：本学院教师的 uuid 集合。 */
    private Set<String> m_teacher_uuids = new HashSet<String>();

    /** 构造一个空学院对象，供对象流与数据访问层填充字段。 */
    public College() {
    }

    /**
     * 构造一个学院。
     *
     * @param uuid 学院唯一标识
     * @param name 学院名称
     */
    public College(String uuid, String name) {
        this.m_uuid = uuid;
        this.m_name = name;
    }

    /** @return 学院唯一标识 */
    public String getUuid() {
        return m_uuid;
    }

    /** @param uuid 学院唯一标识 */
    public void setUuid(String uuid) {
        this.m_uuid = uuid;
    }

    /** @return 学院名称 */
    public String getName() {
        return m_name;
    }

    /** @param name 学院名称 */
    public void setName(String name) {
        this.m_name = name;
    }

    /** @return 学院官网 */
    public String getWebsite() {
        return m_website;
    }

    /** @param website 学院官网 */
    public void setWebsite(String website) {
        this.m_website = website;
    }

    /** @return 学院介绍 */
    public String getDescription() {
        return m_description;
    }

    /** @param description 学院介绍 */
    public void setDescription(String description) {
        this.m_description = description;
    }

    /** @return 研究方向集合 */
    public Set<Field> getResearchDirections() {
        return m_research_directions;
    }

    /** @param researchDirections 研究方向集合 */
    public void setResearchDirections(Set<Field> researchDirections) {
        this.m_research_directions = researchDirections == null
                ? new HashSet<Field>() : researchDirections;
    }

    /** @return 专业集合 */
    public Set<Field> getMajors() {
        return m_majors;
    }

    /** @param majors 专业集合 */
    public void setMajors(Set<Field> majors) {
        this.m_majors = majors == null ? new HashSet<Field>() : majors;
    }

    /** @return 教师 uuid 集合（双向索引） */
    public Set<String> getTeacherUuids() {
        return m_teacher_uuids;
    }

    /** @param teacherUuids 教师 uuid 集合 */
    public void setTeacherUuids(Set<String> teacherUuids) {
        this.m_teacher_uuids = teacherUuids == null ? new HashSet<String>() : teacherUuids;
    }
}
