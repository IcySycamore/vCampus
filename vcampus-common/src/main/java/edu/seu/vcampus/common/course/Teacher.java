package edu.seu.vcampus.common.course;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * 教师（值对象）：课程模块视角下的教师，通过 uuid 引用用户账户，并与学院双向索引。
 *
 * <p>教师拥有研究组与研究方向（研究方向决定其可认领哪些课程），以及可用/偏好时间槽
 * 和已认领的课程集合。
 */
public class Teacher implements Serializable {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 教师唯一标识（引用用户账户 uuid）。 */
    private String m_uuid;

    /** 所属学院 uuid（与学院双向索引）。 */
    private String m_college_uuid;

    /** 研究组。 */
    private String m_research_group;

    /** 研究方向（决定可认领的课程）。 */
    private Set<Field> m_research_directions = new HashSet<Field>();

    /** 可用时间槽。 */
    private Set<Timeslot> m_available_timeslots = new HashSet<Timeslot>();

    /** 偏好时间槽。 */
    private Set<Timeslot> m_preference_timeslots = new HashSet<Timeslot>();

    /** 已认领课程的 uuid 集合。 */
    private Set<String> m_claimed_course_uuids = new HashSet<String>();

    /** 构造一个空教师对象，供对象流与数据访问层填充字段。 */
    public Teacher() {
    }

    /**
     * 构造一个教师。
     *
     * @param uuid 教师唯一标识
     * @param collegeUuid 所属学院 uuid
     */
    public Teacher(String uuid, String collegeUuid) {
        this.m_uuid = uuid;
        this.m_college_uuid = collegeUuid;
    }

    /** @return 教师唯一标识 */
    public String getUuid() {
        return m_uuid;
    }

    /** @param uuid 教师唯一标识 */
    public void setUuid(String uuid) {
        this.m_uuid = uuid;
    }

    /** @return 所属学院 uuid */
    public String getCollegeUuid() {
        return m_college_uuid;
    }

    /** @param collegeUuid 所属学院 uuid */
    public void setCollegeUuid(String collegeUuid) {
        this.m_college_uuid = collegeUuid;
    }

    /** @return 研究组 */
    public String getResearchGroup() {
        return m_research_group;
    }

    /** @param researchGroup 研究组 */
    public void setResearchGroup(String researchGroup) {
        this.m_research_group = researchGroup;
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

    /** @return 可用时间槽集合 */
    public Set<Timeslot> getAvailableTimeslots() {
        return m_available_timeslots;
    }

    /** @param availableTimeslots 可用时间槽集合 */
    public void setAvailableTimeslots(Set<Timeslot> availableTimeslots) {
        this.m_available_timeslots = availableTimeslots == null
                ? new HashSet<Timeslot>() : availableTimeslots;
    }

    /** @return 偏好时间槽集合 */
    public Set<Timeslot> getPreferenceTimeslots() {
        return m_preference_timeslots;
    }

    /** @param preferenceTimeslots 偏好时间槽集合 */
    public void setPreferenceTimeslots(Set<Timeslot> preferenceTimeslots) {
        this.m_preference_timeslots = preferenceTimeslots == null
                ? new HashSet<Timeslot>() : preferenceTimeslots;
    }

    /** @return 已认领课程 uuid 集合 */
    public Set<String> getClaimedCourseUuids() {
        return m_claimed_course_uuids;
    }

    /** @param claimedCourseUuids 已认领课程 uuid 集合 */
    public void setClaimedCourseUuids(Set<String> claimedCourseUuids) {
        this.m_claimed_course_uuids = claimedCourseUuids == null
                ? new HashSet<String>() : claimedCourseUuids;
    }
}
