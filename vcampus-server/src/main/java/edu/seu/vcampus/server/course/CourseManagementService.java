package edu.seu.vcampus.server.course;

import edu.seu.vcampus.common.course.Classroom;
import edu.seu.vcampus.common.course.CourseRules;
import edu.seu.vcampus.common.course.CourseSection;
import edu.seu.vcampus.common.course.Teacher;
import edu.seu.vcampus.common.course.Timeslot;

import java.util.ArrayList;
import java.util.List;

/**
 * 课程管理服务：学院开设课程、教师认领课程与手动排课。
 *
 * <p>方法约定：返回 {@code String}，{@code null} 表示成功，非空为失败原因。校验谓词统一收敛到
 * {@link CourseRules}。
 */
public class CourseManagementService {

    /** 课程数据访问。 */
    private final CourseDao m_course_dao;

    /**
     * @param courseDao 课程数据访问
     */
    public CourseManagementService(CourseDao courseDao) {
        if (courseDao == null) {
            throw new IllegalArgumentException("courseDao must not be null");
        }
        this.m_course_dao = courseDao;
    }

    /**
     * @param course 课程
     * @return 失败原因，成功为 null
     */
    public String openCourse(CourseSection course) {
        if (course == null) {
            return "课程不能为空";
        }
        if (course.getCode() == null || course.getCode().trim().length() == 0) {
            return "课程编号不能为空";
        }
        if (course.getCollegeUuid() == null
                || m_course_dao.findCollege(course.getCollegeUuid()) == null) {
            return "开课学院不存在";
        }
        if (course.getCapacity() <= 0) {
            return "课程容量必须大于 0";
        }
        m_course_dao.saveCourse(course);
        return null;
    }

    /**
     * @param teacherUuid 教师
     * @param courseUuid 课程
     * @return 失败原因，成功为 null
     */
    public String claimCourse(String teacherUuid, String courseUuid) {
        Teacher teacher = m_course_dao.findTeacher(teacherUuid);
        if (teacher == null) {
            return "教师不存在";
        }
        CourseSection course = m_course_dao.findCourse(courseUuid);
        if (course == null) {
            return "课程不存在";
        }
        if (course.getTeacherUuid() != null) {
            return "课程已被认领";
        }
        if (!CourseRules.hasRequiredDirection(teacher, course)) {
            return "教师不具备课程要求的研究方向";
        }
        course.setTeacherUuid(teacherUuid);
        teacher.getClaimedCourseUuids().add(courseUuid);
        return null;
    }

    /**
     * 手动排课。
     * @param courseUuid 课程
     * @param classroomUuid 教室
     * @param timeslots 时间槽
     * @return 失败原因，成功为 null
     */
    public String scheduleCourse(String courseUuid, String classroomUuid,
            List<Timeslot> timeslots) {
        CourseSection course = m_course_dao.findCourse(courseUuid);
        if (course == null) {
            return "课程不存在";
        }
        Classroom classroom = m_course_dao.findClassroom(classroomUuid);
        if (classroom == null) {
            return "教室不存在";
        }
        if (timeslots == null || timeslots.isEmpty()) {
            return "请指定上课时间槽";
        }
        if (classroom.getCapacity() < course.getCapacity()) {
            return "教室容量不足";
        }
        if (classroom.getCapacity() < course.getEnrolledCount()) {
            return "教室容量不足（少于已选人数）";
        }
        Teacher teacher = course.getTeacherUuid() == null
                ? null : m_course_dao.findTeacher(course.getTeacherUuid());
        for (Timeslot slot : timeslots) {
            if (teacher != null
                    && !CourseRules.coveredByAny(teacher.getAvailableTimeslots(), slot)) {
                return "上课时间不在教师可用时间槽内";
            }
            if (!CourseRules.coveredByAny(classroom.getAvailableTimeslots(), slot)) {
                return "上课时间不在教室可用时间槽内";
            }
        }
        course.setClassroomUuid(classroomUuid);
        course.getTimeslots().clear();
        course.getTimeslots().addAll(timeslots);
        return null;
    }

    /**
     * 推荐可用教室（偏好教学楼优先）。
     * @param courseUuid 课程
     * @param timeslots 时间槽
     * @return 教室 uuid，无则 null
     */
    public String suggestClassroom(String courseUuid, List<Timeslot> timeslots) {
        CourseSection course = m_course_dao.findCourse(courseUuid);
        if (course == null || timeslots == null || timeslots.isEmpty()) {
            return null;
        }
        String preferred = course.getPreferredLocation();
        for (Classroom classroom : m_course_dao.findAllClassrooms()) {
            if (preferred != null && preferred.equals(classroom.getLocation())
                    && CourseRules.classroomFits(classroom, course, timeslots)) {
                return classroom.getUuid();
            }
        }
        for (Classroom classroom : m_course_dao.findAllClassrooms()) {
            if (CourseRules.classroomFits(classroom, course, timeslots)) {
                return classroom.getUuid();
            }
        }
        return null;
    }

    /** @return 全部课程快照 */
    public List<CourseSection> listCourses() {
        return m_course_dao.findAllCourses();
    }

    /**
     * 查某教师认领的全部课程。
     *
     * @param teacherUuid 教师 uuid
     * @return 课程列表
     */
    public List<CourseSection> listCoursesByTeacher(String teacherUuid) {
        List<CourseSection> result = new ArrayList<CourseSection>();
        if (teacherUuid == null) {
            return result;
        }
        for (CourseSection course : m_course_dao.findAllCourses()) {
            if (teacherUuid.equals(course.getTeacherUuid())) {
                result.add(course);
            }
        }
        return result;
    }

    /**
     * 设置教师的偏好时间槽（覆盖旧值）。
     *
     * @param teacherUuid 教师 uuid
     * @param timeslots 偏好时间槽
     * @return 失败原因，成功为 null
     */
    public String setTeacherPreferenceTimeslots(String teacherUuid,
            List<Timeslot> timeslots) {
        Teacher teacher = m_course_dao.findTeacher(teacherUuid);
        if (teacher == null) {
            return "教师不存在";
        }
        teacher.getPreferenceTimeslots().clear();
        if (timeslots != null) {
            teacher.getPreferenceTimeslots().addAll(timeslots);
        }
        return null;
    }
}
