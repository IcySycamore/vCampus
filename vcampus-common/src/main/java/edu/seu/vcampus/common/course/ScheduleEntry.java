package edu.seu.vcampus.common.course;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * 排课条目（值对象）：一门课程被安排到某教室的某个时间槽。
 *
 * <p>排课网格以「周一到周五 × 每天 5 节」为坐标，每门课占一个时间槽（一个格子）；
 * 未排课时 {@code timeslot} 为 null。
 */
public class ScheduleEntry implements Serializable {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 课程唯一标识（uuid）。 */
    private String m_uuid;

    /** 课程编号。 */
    private String m_course_code;
    /** 课程名称。 */
    private String m_course_name;
    /** 授课教师 uuid。 */
    private String m_teacher_uuid;
    /** 分配教室 uuid；未排为 null。 */
    private String m_classroom_uuid;
    /** 教室位置（冗余展示）。 */
    private String m_classroom_location;
    /** 选课容量。 */
    private int m_capacity;
    /** 已选人数。 */
    private int m_enrolled;
    /** 上课时间槽；未排为 null。 */
    private Timeslot m_timeslot;

    /** 课程标签：教师需具备的研究方向。 */
    private Set<Field> m_required_directions = new HashSet<Field>();

    /** 课程标签：可选专业。 */
    private Set<Field> m_eligible_majors = new HashSet<Field>();

    /** 课程标签：所属学院 uuid。 */
    private String m_college_uuid;

    /** 构造一个空条目，供对象流填充字段。 */
    public ScheduleEntry() {
    }

    /**
     * 构造一门待排课程。
     *
     * @param courseCode 课程编号
     * @param courseName 课程名称
     * @param teacherUuid 授课教师 uuid
     * @param capacity 容量
     * @param enrolled 已选人数
     */
    public ScheduleEntry(String courseCode, String courseName, String teacherUuid,
            int capacity, int enrolled) {
        this.m_course_code = courseCode;
        this.m_course_name = courseName;
        this.m_teacher_uuid = teacherUuid;
        this.m_capacity = capacity;
        this.m_enrolled = enrolled;
    }

    /** @return 深拷贝（撤销/重做与算法都不应改动共享引用）。 */
    public ScheduleEntry copy() {
        ScheduleEntry copy = new ScheduleEntry(m_course_code, m_course_name, m_teacher_uuid,
                m_capacity, m_enrolled);
        copy.m_uuid = m_uuid;
        copy.m_classroom_uuid = m_classroom_uuid;
        copy.m_classroom_location = m_classroom_location;
        copy.m_timeslot = m_timeslot;
        copy.m_required_directions = new HashSet<Field>(m_required_directions);
        copy.m_eligible_majors = new HashSet<Field>(m_eligible_majors);
        copy.m_college_uuid = m_college_uuid;
        return copy;
    }

    /** @return 课程标签：教师需具备的研究方向 */
    public Set<Field> getRequiredDirections() {
        return m_required_directions;
    }

    /** @param requiredDirections 课程标签：教师需具备的研究方向 */
    public void setRequiredDirections(Set<Field> requiredDirections) {
        this.m_required_directions = requiredDirections == null
                ? new HashSet<Field>() : requiredDirections;
    }

    /** @return 课程标签：可选专业 */
    public Set<Field> getEligibleMajors() {
        return m_eligible_majors;
    }

    /** @param eligibleMajors 课程标签：可选专业 */
    public void setEligibleMajors(Set<Field> eligibleMajors) {
        this.m_eligible_majors = eligibleMajors == null ? new HashSet<Field>() : eligibleMajors;
    }

    /** @return 课程标签：所属学院 uuid */
    public String getCollegeUuid() {
        return m_college_uuid;
    }

    /** @param collegeUuid 课程标签：所属学院 uuid */
    public void setCollegeUuid(String collegeUuid) {
        this.m_college_uuid = collegeUuid;
    }

    /** @return 课程唯一标识 */
    public String getUuid() {
        return m_uuid;
    }

    /** @param uuid 课程唯一标识 */
    public void setUuid(String uuid) {
        this.m_uuid = uuid;
    }

    /** @return 课程编号 */
    public String getCourseCode() {
        return m_course_code;
    }

    /** @param courseCode 课程编号 */
    public void setCourseCode(String courseCode) {
        this.m_course_code = courseCode;
    }

    /** @return 课程名称 */
    public String getCourseName() {
        return m_course_name;
    }

    /** @param courseName 课程名称 */
    public void setCourseName(String courseName) {
        this.m_course_name = courseName;
    }

    /** @return 授课教师 uuid */
    public String getTeacherUuid() {
        return m_teacher_uuid;
    }

    /** @param teacherUuid 授课教师 uuid */
    public void setTeacherUuid(String teacherUuid) {
        this.m_teacher_uuid = teacherUuid;
    }

    /** @return 分配教室 uuid */
    public String getClassroomUuid() {
        return m_classroom_uuid;
    }

    /** @param classroomUuid 分配教室 uuid */
    public void setClassroomUuid(String classroomUuid) {
        this.m_classroom_uuid = classroomUuid;
    }

    /** @return 教室位置 */
    public String getClassroomLocation() {
        return m_classroom_location;
    }

    /** @param classroomLocation 教室位置 */
    public void setClassroomLocation(String classroomLocation) {
        this.m_classroom_location = classroomLocation;
    }

    /** @return 选课容量 */
    public int getCapacity() {
        return m_capacity;
    }

    /** @param capacity 选课容量 */
    public void setCapacity(int capacity) {
        this.m_capacity = capacity;
    }

    /** @return 已选人数 */
    public int getEnrolled() {
        return m_enrolled;
    }

    /** @param enrolled 已选人数 */
    public void setEnrolled(int enrolled) {
        this.m_enrolled = enrolled;
    }

    /** @return 上课时间槽 */
    public Timeslot getTimeslot() {
        return m_timeslot;
    }

    /** @param timeslot 上课时间槽 */
    public void setTimeslot(Timeslot timeslot) {
        this.m_timeslot = timeslot;
    }
}
