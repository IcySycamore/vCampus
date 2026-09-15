package edu.seu.vcampus.common.course;

import java.io.Serializable;

/**
 * 学生选课成绩实体（值对象）。
 *
 * <p>一条记录表示「某学生在某学期选修某门课程及其成绩」。选课阶段可先只创建
 * 记录（成绩尚未录入，{@code m_score} 为 null），成绩录入后再填充分数。学生
 * 通过 {@code m_student_uuid} 引用 {@code common.user.User} 的全局唯一标识
 * uuid，课程通过 {@code m_course_code} 引用 {@link Course} 的课程编号。
 */
public class Score implements Serializable {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 成绩记录主键（数据库自增分配，插入前为 null）。 */
    private Long m_id;

    /** 学生账户 uuid（引用 common.user.User 的全局唯一标识）。 */
    private String m_student_uuid;

    /** 课程编号（引用 Course 的课程编号）。 */
    private String m_course_code;

    /** 成绩分数；null 表示尚未录入成绩。 */
    private Double m_score;

    /** 学期，如 2026-2027-1。 */
    private String m_semester;

    /**
     * 构造一个空成绩对象，供对象流与数据访问层填充字段。
     */
    public Score() {
    }

    /**
     * 构造一条选课记录，成绩尚未录入。
     *
     * @param studentUuid 学生账户 uuid
     * @param courseCode  课程编号
     * @param semester    学期
     */
    public Score(String studentUuid, String courseCode, String semester) {
        this.m_student_uuid = studentUuid;
        this.m_course_code = courseCode;
        this.m_semester = semester;
        this.m_score = null;
    }

    /** @return 成绩记录主键 */
    public Long getId() {
        return m_id;
    }

    /** @param id 成绩记录主键 */
    public void setId(Long id) {
        this.m_id = id;
    }

    /** @return 学生账户 uuid */
    public String getStudentUuid() {
        return m_student_uuid;
    }

    /** @param studentUuid 学生账户 uuid */
    public void setStudentUuid(String studentUuid) {
        this.m_student_uuid = studentUuid;
    }

    /** @return 课程编号 */
    public String getCourseCode() {
        return m_course_code;
    }

    /** @param courseCode 课程编号 */
    public void setCourseCode(String courseCode) {
        this.m_course_code = courseCode;
    }

    /** @return 成绩分数；null 表示尚未录入 */
    public Double getScore() {
        return m_score;
    }

    /** @param score 成绩分数 */
    public void setScore(Double score) {
        this.m_score = score;
    }

    /** @return 学期 */
    public String getSemester() {
        return m_semester;
    }

    /** @param semester 学期 */
    public void setSemester(String semester) {
        this.m_semester = semester;
    }
}
