package edu.seu.vcampus.common.course;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/** 课程（开设的课程/教学班）：学院开设、教师认领、排入教室并由学生选课的对象。 */
public class CourseSection implements Serializable {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 课程唯一标识。 */
    private String m_uuid;
    /** 课程编号，如 CS101。 */
    private String m_code;
    /** 课程名称。 */
    private String m_name;
    /** 开课学院 uuid。 */
    private String m_college_uuid;
    /** 学分。 */
    private int m_credit;
    /** 人数上限。 */
    private int m_capacity;
    /** 偏好教学楼。 */
    private String m_preferred_location;
    /** 学期，如 2026-2027-1。 */
    private String m_semester;
    /** 学生可选专业（空表示仅按同学院判定）。 */
    private Set<Field> m_eligible_majors = new HashSet<Field>();
    /** 教师需具备的研究方向（空表示不限）。 */
    private Set<Field> m_required_directions = new HashSet<Field>();
    /** 认领教师 uuid；未认领为 null。 */
    private String m_teacher_uuid;
    /** 分配教室 uuid；未安排为 null。 */
    private String m_classroom_uuid;
    /** 上课时间槽。 */
    private Set<Timeslot> m_timeslots = new HashSet<Timeslot>();
    /** 已选学生 uuid 集合。 */
    private Set<String> m_student_uuids = new HashSet<String>();

    /** 构造一个空课程对象。 */
    public CourseSection() {
    }

    /**
     * 构造一门课程。
     * @param code 编号
     * @param name 名称
     * @param collegeUuid 学院
     * @param capacity 人数上限
     */
    public CourseSection(String code, String name, String collegeUuid, int capacity) {
        this.m_code = code;
        this.m_name = name;
        this.m_college_uuid = collegeUuid;
        this.m_capacity = capacity;
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
    public String getCode() {
        return m_code;
    }
    /** @param code 课程编号 */
    public void setCode(String code) {
        this.m_code = code;
    }

    /** @return 课程名称 */
    public String getName() {
        return m_name;
    }
    /** @param name 课程名称 */
    public void setName(String name) {
        this.m_name = name;
    }

    /** @return 开课学院 uuid */
    public String getCollegeUuid() {
        return m_college_uuid;
    }
    /** @param collegeUuid 开课学院 uuid */
    public void setCollegeUuid(String collegeUuid) {
        this.m_college_uuid = collegeUuid;
    }

    /** @return 学分 */
    public int getCredit() {
        return m_credit;
    }
    /** @param credit 学分 */
    public void setCredit(int credit) {
        this.m_credit = credit;
    }

    /** @return 人数上限 */
    public int getCapacity() {
        return m_capacity;
    }
    /** @param capacity 人数上限 */
    public void setCapacity(int capacity) {
        this.m_capacity = capacity;
    }

    /** @return 偏好教学楼 */
    public String getPreferredLocation() {
        return m_preferred_location;
    }
    /** @param preferredLocation 偏好教学楼 */
    public void setPreferredLocation(String preferredLocation) {
        this.m_preferred_location = preferredLocation;
    }

    /** @return 学期 */
    public String getSemester() {
        return m_semester;
    }
    /** @param semester 学期 */
    public void setSemester(String semester) {
        this.m_semester = semester;
    }

    /** @return 学生可选专业集合 */
    public Set<Field> getEligibleMajors() {
        return m_eligible_majors;
    }
    /** @param eligibleMajors 学生可选专业集合 */
    public void setEligibleMajors(Set<Field> eligibleMajors) {
        this.m_eligible_majors = eligibleMajors == null
                ? new HashSet<Field>() : eligibleMajors;
    }

    /** @return 教师需具备的研究方向集合 */
    public Set<Field> getRequiredDirections() {
        return m_required_directions;
    }
    /** @param requiredDirections 教师需具备的研究方向集合 */
    public void setRequiredDirections(Set<Field> requiredDirections) {
        this.m_required_directions = requiredDirections == null
                ? new HashSet<Field>() : requiredDirections;
    }

    /** @return 认领教师 uuid */
    public String getTeacherUuid() {
        return m_teacher_uuid;
    }
    /** @param teacherUuid 认领教师 uuid */
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

    /** @return 上课时间槽集合 */
    public Set<Timeslot> getTimeslots() {
        return m_timeslots;
    }
    /** @param timeslots 上课时间槽集合 */
    public void setTimeslots(Set<Timeslot> timeslots) {
        this.m_timeslots = timeslots == null ? new HashSet<Timeslot>() : timeslots;
    }

    /** @return 已选学生 uuid 集合 */
    public Set<String> getStudentUuids() {
        return m_student_uuids;
    }
    /** @param studentUuids 已选学生 uuid 集合 */
    public void setStudentUuids(Set<String> studentUuids) {
        this.m_student_uuids = studentUuids == null ? new HashSet<String>() : studentUuids;
    }

    /** @return 当前已选人数 */
    public int getEnrolledCount() {
        return m_student_uuids.size();
    }
}
