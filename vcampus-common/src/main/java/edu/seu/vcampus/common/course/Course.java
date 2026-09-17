package edu.seu.vcampus.common.course;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * 课程实体（值对象）。
 *
 * <p>描述一门可供选修的课程，保留课程自身字段与授课教师引用。授课教师通过
 * {@code m_teacher_uuid} 引用 {@code common.user.User} 的全局唯一标识 uuid，
 * 需要教师详情时凭该 uuid 向用户管理模块查询。学生的选课与成绩关系由
 * {@link Score} 承载，本类不直接持有学生列表。
 */
public class Course implements Serializable {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 课程记录主键（数据库自增分配，插入前为 null）。 */
    private Long m_id;

    /** 课程唯一标识（随机 uuid）。 */
    private String m_uuid;

    /** 课程编号，如 CS101。 */
    private String m_code;

    /** 课程名称。 */
    private String m_name;

    /** 学分。 */
    private int m_credit;

    /** 授课教师账户 uuid（引用 common.user.User 的全局唯一标识）。 */
    private String m_teacher_uuid;

    /** 选课容量，即最多可容纳的学生人数。 */
    private int m_capacity;

    /** 已选人数。 */
    private int m_enrolled;

    /** 学期，如 2026-2027-1。 */
    private String m_semester;

    /** 分配教室 uuid；未安排为 null。 */
    private String m_classroom_uuid;

    /** 上课时间槽（排课网格用一个时间槽表示一个格子）；未安排为 null。 */
    private Timeslot m_timeslot;

    /** 课程标签：教师需具备的研究方向。 */
    private Set<Field> m_required_directions = new HashSet<Field>();

    /** 课程标签：可选专业。 */
    private Set<Field> m_eligible_majors = new HashSet<Field>();

    /** 课程标签：所属学院 uuid。 */
    private String m_college_uuid;

    /**
     * 构造一个空课程对象，供对象流与数据访问层填充字段。
     */
    public Course() {
    }

    /**
     * 构造一门课程，初始已选人数为 0。
     *
     * @param code        课程编号
     * @param name        课程名称
     * @param credit      学分
     * @param teacherUuid 授课教师账户 uuid
     * @param capacity    选课容量
     */
    public Course(String code, String name, int credit, String teacherUuid,
            int capacity) {
        this.m_code = code;
        this.m_name = name;
        this.m_credit = credit;
        this.m_teacher_uuid = teacherUuid;
        this.m_capacity = capacity;
        this.m_enrolled = 0;
    }

    /** @return 课程记录主键 */
    public Long getId() {
        return m_id;
    }

    /** @param id 课程记录主键 */
    public void setId(Long id) {
        this.m_id = id;
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

    /** @return 学分 */
    public int getCredit() {
        return m_credit;
    }

    /** @param credit 学分 */
    public void setCredit(int credit) {
        this.m_credit = credit;
    }

    /** @return 授课教师账户 uuid */
    public String getTeacherUuid() {
        return m_teacher_uuid;
    }

    /** @param teacherUuid 授课教师账户 uuid */
    public void setTeacherUuid(String teacherUuid) {
        this.m_teacher_uuid = teacherUuid;
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

    /** @return 学期 */
    public String getSemester() {
        return m_semester;
    }

    /** @param semester 学期 */
    public void setSemester(String semester) {
        this.m_semester = semester;
    }

    /** @return 分配教室 uuid */
    public String getClassroomUuid() {
        return m_classroom_uuid;
    }

    /** @param classroomUuid 分配教室 uuid */
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
}
