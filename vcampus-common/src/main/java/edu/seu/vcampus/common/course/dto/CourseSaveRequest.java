package edu.seu.vcampus.common.course.dto;

import edu.seu.vcampus.common.course.Field;
import edu.seu.vcampus.common.course.Timeslot;

import java.io.Serializable;
import java.util.Set;

/**
 * 课程保存请求（值对象）：管理员添加或修改课程时使用的统一载荷。
 *
 * <p>添加时使用课程编号、名称、学分、容量、学期（授课教师可选）；修改时以课程编号为
 * 定位键，仅名称、容量、授课教师生效——课程编号与 uuid 不可改，由服务端保证。
 */
public class CourseSaveRequest implements Serializable {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 课程编号（添加时必填且唯一；修改时作为定位键）。 */
    private String m_code;

    /** 课程名称。 */
    private String m_name;

    /** 学分（添加时必填）。 */
    private Integer m_credit;

    /** 容量（添加时必填；修改时只增不减）。 */
    private Integer m_capacity;

    /** 学期，可空（服务端缺省为 2026-2027-1）。 */
    private String m_semester;

    /** 授课教师 uuid，可空（空字符串在修改时表示取消认领）。 */
    private String m_teacher_uuid;

    /** 课程 uuid（修改时作为定位键，不可改）。 */
    private String m_uuid;

    /** 授课教室 uuid（排课，可空）。 */
    private String m_classroom_uuid;

    /** 上课时间槽（开始/结束时间，排课）。 */
    private Timeslot m_timeslot;

    /** 课程标签：教师需具备的研究方向。 */
    private Set<Field> m_required_directions;

    /** 课程标签：可选专业。 */
    private Set<Field> m_eligible_majors;

    /** 课程标签：所属学院 uuid。 */
    private String m_college_uuid;

    /** 构造一个空请求，供对象流填充字段。 */
    public CourseSaveRequest() {
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

    /** @return 学分 */
    public Integer getCredit() {
        return m_credit;
    }

    /** @param credit 学分 */
    public void setCredit(Integer credit) {
        this.m_credit = credit;
    }

    /** @return 容量 */
    public Integer getCapacity() {
        return m_capacity;
    }

    /** @param capacity 容量 */
    public void setCapacity(Integer capacity) {
        this.m_capacity = capacity;
    }

    /** @return 学期 */
    public String getSemester() {
        return m_semester;
    }

    /** @param semester 学期 */
    public void setSemester(String semester) {
        this.m_semester = semester;
    }

    /** @return 授课教师 uuid */
    public String getTeacherUuid() {
        return m_teacher_uuid;
    }

    /** @param teacherUuid 授课教师 uuid */
    public void setTeacherUuid(String teacherUuid) {
        this.m_teacher_uuid = teacherUuid;
    }

    /** @return 课程 uuid */
    public String getUuid() {
        return m_uuid;
    }

    /** @param uuid 课程 uuid */
    public void setUuid(String uuid) {
        this.m_uuid = uuid;
    }

    /** @return 授课教室 uuid */
    public String getClassroomUuid() {
        return m_classroom_uuid;
    }

    /** @param classroomUuid 授课教室 uuid */
    public void setClassroomUuid(String classroomUuid) {
        this.m_classroom_uuid = classroomUuid;
    }

    /** @return 上课时间槽 */
    public Timeslot getTimeslot() {
        return m_timeslot;
    }

    /** @param timeslot 上课时间槽 */
    public void setTimeslot(Timeslot timeslot) {
        this.m_timeslot = timeslot;
    }

    /** @return 课程标签：教师需具备的研究方向 */
    public Set<Field> getRequiredDirections() {
        return m_required_directions;
    }

    /** @param requiredDirections 课程标签：教师需具备的研究方向 */
    public void setRequiredDirections(Set<Field> requiredDirections) {
        this.m_required_directions = requiredDirections;
    }

    /** @return 课程标签：可选专业 */
    public Set<Field> getEligibleMajors() {
        return m_eligible_majors;
    }

    /** @param eligibleMajors 课程标签：可选专业 */
    public void setEligibleMajors(Set<Field> eligibleMajors) {
        this.m_eligible_majors = eligibleMajors;
    }

    /** @return 课程标签：所属学院 uuid */
    public String getCollegeUuid() {
        return m_college_uuid;
    }

    /** @param collegeUuid 课程标签：所属学院 uuid */
    public void setCollegeUuid(String collegeUuid) {
        this.m_college_uuid = collegeUuid;
    }
}
