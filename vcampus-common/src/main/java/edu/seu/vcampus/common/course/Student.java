package edu.seu.vcampus.common.course;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * 学生（值对象）：课程模块视角下的学生，通过 uuid 引用用户账户。
 *
 * <p>学生的「学院 + 专业」决定其可选课程范围；可用/偏好时间槽与已选课程用于选课
 * 冲突与时间校验。
 */
public class Student implements Serializable {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 学生唯一标识（引用用户账户 uuid）。 */
    private String m_uuid;

    /** 所属学院 uuid。 */
    private String m_college_uuid;

    /** 专业（决定可选课程）。 */
    private Field m_major;

    /** 可用时间槽。 */
    private Set<Timeslot> m_available_timeslots = new HashSet<Timeslot>();

    /** 偏好时间槽。 */
    private Set<Timeslot> m_preference_timeslots = new HashSet<Timeslot>();

    /** 已选课程 uuid 集合。 */
    private Set<String> m_selected_course_uuids = new HashSet<String>();

    /** 构造一个空学生对象，供对象流与数据访问层填充字段。 */
    public Student() {
    }

    /**
     * 构造一个学生。
     *
     * @param uuid 学生唯一标识
     * @param collegeUuid 所属学院 uuid
     * @param major 专业
     */
    public Student(String uuid, String collegeUuid, Field major) {
        this.m_uuid = uuid;
        this.m_college_uuid = collegeUuid;
        this.m_major = major;
    }

    /** @return 学生唯一标识 */
    public String getUuid() {
        return m_uuid;
    }

    /** @param uuid 学生唯一标识 */
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

    /** @return 专业 */
    public Field getMajor() {
        return m_major;
    }

    /** @param major 专业 */
    public void setMajor(Field major) {
        this.m_major = major;
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

    /** @return 已选课程 uuid 集合 */
    public Set<String> getSelectedCourseUuids() {
        return m_selected_course_uuids;
    }

    /** @param selectedCourseUuids 已选课程 uuid 集合 */
    public void setSelectedCourseUuids(Set<String> selectedCourseUuids) {
        this.m_selected_course_uuids = selectedCourseUuids == null
                ? new HashSet<String>() : selectedCourseUuids;
    }
}
