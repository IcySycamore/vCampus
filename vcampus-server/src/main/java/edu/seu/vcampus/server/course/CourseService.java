package edu.seu.vcampus.server.course;

import edu.seu.vcampus.common.course.CourseRules;
import edu.seu.vcampus.common.course.CourseSection;
import edu.seu.vcampus.common.course.Score;
import edu.seu.vcampus.common.course.Student;
import edu.seu.vcampus.common.course.Timeslot;

import java.util.List;

/**
 * 选课业务服务：学生选课、退课与成绩录入。
 *
 * <p>方法约定：返回 {@code String}，{@code null} 表示成功，非空为失败原因。校验谓词统一收敛到
 * {@link CourseRules}。
 */
public class CourseService {

    /** 课程数据访问。 */
    private final CourseDao m_course_dao;
    /** 成绩数据访问。 */
    private final ScoreDao m_score_dao;

    /**
     * @param courseDao 课程数据访问
     * @param scoreDao 成绩数据访问
     */
    public CourseService(CourseDao courseDao, ScoreDao scoreDao) {
        if (courseDao == null || scoreDao == null) {
            throw new IllegalArgumentException("courseDao and scoreDao must not be null");
        }
        this.m_course_dao = courseDao;
        this.m_score_dao = scoreDao;
    }

    /**
     * @param studentUuid 学生
     * @param courseUuid 课程
     * @return 失败原因，成功为 null
     */
    public String selectCourse(String studentUuid, String courseUuid) {
        Student student = m_course_dao.findStudent(studentUuid);
        if (student == null) {
            return "学生不存在";
        }
        CourseSection course = m_course_dao.findCourse(courseUuid);
        if (course == null) {
            return "课程不存在";
        }
        if (course.getStudentUuids().contains(studentUuid)) {
            return "已选该课程";
        }
        if (!CourseRules.isEligible(student, course)) {
            return "不符合选课条件（需同学院或指定专业）";
        }
        if (course.getEnrolledCount() >= course.getCapacity()) {
            return "课程人数已满";
        }
        if (!course.getTimeslots().isEmpty()) {
            for (String otherUuid : student.getSelectedCourseUuids()) {
                CourseSection other = m_course_dao.findCourse(otherUuid);
                if (other != null
                        && CourseRules.overlapsAny(course.getTimeslots(), other.getTimeslots())) {
                    return "与已选课程时间冲突";
                }
            }
            for (Timeslot slot : course.getTimeslots()) {
                if (!CourseRules.coveredByAny(student.getAvailableTimeslots(), slot)) {
                    return "上课时间不在学生可用时间槽内";
                }
            }
        }
        course.getStudentUuids().add(studentUuid);
        student.getSelectedCourseUuids().add(courseUuid);
        return null;
    }

    /**
     * @param studentUuid 学生
     * @param courseUuid 课程
     * @return 失败原因，成功为 null
     */
    public String dropCourse(String studentUuid, String courseUuid) {
        Student student = m_course_dao.findStudent(studentUuid);
        if (student == null) {
            return "学生不存在";
        }
        CourseSection course = m_course_dao.findCourse(courseUuid);
        if (course == null) {
            return "课程不存在";
        }
        if (!course.getStudentUuids().contains(studentUuid)) {
            return "未选该课程";
        }
        course.getStudentUuids().remove(studentUuid);
        student.getSelectedCourseUuids().remove(courseUuid);
        return null;
    }

    /**
     * 为已选修学生录入成绩。
     * @param courseUuid 课程
     * @param studentUuid 学生
     * @param score 成绩（0~100）
     * @return 失败原因，成功为 null
     */
    public String recordScore(String courseUuid, String studentUuid, double score) {
        CourseSection course = m_course_dao.findCourse(courseUuid);
        if (course == null) {
            return "课程不存在";
        }
        if (!course.getStudentUuids().contains(studentUuid)) {
            return "该学生未选修此课程";
        }
        if (score < 0.0 || score > 100.0) {
            return "成绩必须在 0~100 之间";
        }
        Score record = new Score(studentUuid, course.getCode(), course.getSemester());
        record.setScore(Double.valueOf(score));
        m_score_dao.save(record);
        return null;
    }

    /**
     * @param studentUuid 学生
     * @return 成绩列表
     */
    public List<Score> listScoresByStudent(String studentUuid) {
        return m_score_dao.findByStudent(studentUuid);
    }

    /**
     * @param courseCode 课程编号
     * @return 成绩列表
     */
    public List<Score> listScoresByCourse(String courseCode) {
        return m_score_dao.findByCourse(courseCode);
    }
}
