package edu.seu.vcampus.server.course;

import edu.seu.vcampus.common.course.Classroom;
import edu.seu.vcampus.common.course.College;
import edu.seu.vcampus.common.course.CourseRules;
import edu.seu.vcampus.common.course.CourseSection;
import edu.seu.vcampus.common.course.Teacher;
import edu.seu.vcampus.common.course.Timeslot;
import edu.seu.vcampus.common.course.dto.CourseSaveRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * 课程管理服务：学院开设课程、教师认领课程与手动排课。
 *
 * <p>
 * 方法约定：返回 {@code String}，{@code null} 表示成功，非空为失败原因。校验谓词统一收敛到 {@link CourseRules}。
 */
public class CourseManagementService {

    /** 课程数据访问。 */
    private final CourseDao m_course_dao;

    /** 缺省学期（添加课程未指定时使用）。 */
    private static final String DEFAULT_SEMESTER = "2026-2027-1";

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
     * @param courseUuid  课程
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
     * 
     * @param courseUuid    课程
     * @param classroomUuid 教室
     * @param timeslots     时间槽
     * @return 失败原因，成功为 null
     */
    public String scheduleCourse(String courseUuid, String classroomUuid,
            List<Timeslot> timeslots) {
        CourseSection course = m_course_dao.findCourse(courseUuid);
        if (course == null) {
            return "课程不存在";
        }
        if (timeslots == null || timeslots.isEmpty()) {
            // 空时间槽 = 取消排课：清掉教室与时间，回到「待排」状态。
            // 原实现直接报「请指定上课时间槽」，于是界面上排错课只能撤销本地改动、无法真正退回。
            course.setClassroomUuid(null);
            course.getTimeslots().clear();
            return null;
        }
        Classroom classroom = m_course_dao.findClassroom(classroomUuid);
        if (classroom == null) {
            return "教室不存在";
        }
        if (classroom.getCapacity() < course.getCapacity()) {
            return "教室容量不足";
        }
        if (classroom.getCapacity() < course.getEnrolledCount()) {
            return "教室容量不足（少于已选人数）";
        }
        Teacher teacher = course.getTeacherUuid() == null
                ? null
                : m_course_dao.findTeacher(course.getTeacherUuid());
        for (Timeslot slot : timeslots) {
            if (teacher != null
                    && !CourseRules.coveredByAny(teacher.getAvailableTimeslots(), slot)) {
                return "上课时间不在教师可用时间槽内";
            }
            if (!CourseRules.coveredByAny(classroom.getAvailableTimeslots(), slot)) {
                return "上课时间不在教室可用时间槽内";
            }
        }
        for (CourseSection other : m_course_dao.findAllCourses()) {
            if (other.getUuid().equals(course.getUuid())) {
                continue;
            }
            for (Timeslot otherSlot : other.getTimeslots()) {
                for (Timeslot slot : timeslots) {
                    if (!otherSlot.overlaps(slot)) {
                        continue;
                    }
                    if (teacher != null && teacher.getUuid().equals(other.getTeacherUuid())) {
                        return "该教师此时已有课（" + other.getName() + "）";
                    }
                    if (classroomUuid.equals(other.getClassroomUuid())) {
                        return "该教室此时已被占用（" + other.getName() + "）";
                    }
                }
            }
        }
        course.setClassroomUuid(classroomUuid);
        course.getTimeslots().clear();
        course.getTimeslots().addAll(timeslots);
        return null;
    }

    /**
     * 推荐可用教室（偏好教学楼优先）。
     * 
     * @param courseUuid 课程
     * @param timeslots  时间槽
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
     * @param timeslots   偏好时间槽
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

    /**
     * 设置教师的可用时间槽（覆盖旧值）。
     *
     * @param teacherUuid 教师 uuid
     * @param timeslots   可用时间槽
     * @return 失败原因，成功为 null
     */
    public String setTeacherAvailableTimeslots(String teacherUuid,
            List<Timeslot> timeslots) {
        Teacher teacher = m_course_dao.findTeacher(teacherUuid);
        if (teacher == null) {
            return "教师不存在";
        }
        teacher.getAvailableTimeslots().clear();
        if (timeslots != null) {
            teacher.getAvailableTimeslots().addAll(timeslots);
        }
        return null;
    }

    /**
     * 添加课程（管理员）：编号、名称、学分、容量必填，编号唯一，学院缺省取首个。
     *
     * @param request 课程信息
     * @return 失败原因，成功为 null
     */
    public String addCourse(CourseSaveRequest request) {
        if (request == null) {
            return "课程信息不能为空";
        }
        String code = request.getCode() == null ? null : request.getCode().trim();
        if (code == null || code.length() == 0) {
            return "课程编号不能为空";
        }
        if (request.getName() == null || request.getName().trim().length() == 0) {
            return "课程名称不能为空";
        }
        int credit = request.getCredit() == null ? 3 : request.getCredit().intValue();
        if (credit <= 0) {
            return "学分必须大于 0";
        }
        if (request.getCapacity() == null
                || request.getCapacity().intValue() < 40
                || request.getCapacity().intValue() > 100) {
            return "课程容量必须在 40-100 之间";
        }
        if (m_course_dao.findCourseByCode(code) != null) {
            return "课程编号已存在";
        }
        College college = defaultCollege();
        if (college == null) {
            return "开课学院不存在";
        }
        Teacher teacher = null;
        if (request.getTeacherUuid() != null && request.getTeacherUuid().trim().length() > 0) {
            teacher = m_course_dao.findTeacher(request.getTeacherUuid().trim());
            if (teacher == null) {
                return "授课教师不存在";
            }
        }
        CourseSection course = new CourseSection();
        course.setCode(code);
        course.setName(request.getName().trim());
        course.setCollegeUuid(college.getUuid());
        course.setCredit(credit);
        course.setCapacity(request.getCapacity().intValue());
        course.setSemester(
                request.getSemester() == null || request.getSemester().trim().length() == 0
                        ? DEFAULT_SEMESTER
                        : request.getSemester().trim());
        course.setStartWeek(request.getStartWeek());
        course.setEndWeek(request.getEndWeek());
        if (teacher != null) {
            course.setTeacherUuid(teacher.getUuid());
        }
        m_course_dao.saveCourse(course);
        if (teacher != null) {
            teacher.getClaimedCourseUuids().add(course.getUuid());
        }
        return null;
    }

    /**
     * 修改课程（管理员）：以课程编号定位，仅名称、容量、授课教师可改。
     *
     * <p>
     * 课程编号与 uuid 不可改；容量限 40-100 且不能小于已选人数；授课教师空字符串表示 取消认领。
     *
     * @param request 修改信息
     * @return 失败原因，成功为 null
     */
    public String updateCourse(CourseSaveRequest request) {
        if (request == null) {
            return "课程信息不能为空";
        }
        CourseSection course = request.getUuid() == null
                ? m_course_dao.findCourseByCode(request.getCode() == null ? null
                        : request.getCode().trim())
                : m_course_dao.findCourse(request.getUuid());
        if (course == null) {
            return "课程不存在";
        }
        if (request.getCode() != null) {
            String code = request.getCode().trim();
            if (code.length() == 0) {
                return "课程编号不能为空";
            }
            CourseSection duplicate = m_course_dao.findCourseByCode(code);
            if (duplicate != null && !duplicate.getUuid().equals(course.getUuid())) {
                return "课程编号已存在";
            }
            course.setCode(code);
        }
        if (request.getName() != null) {
            if (request.getName().trim().length() == 0) {
                return "课程名称不能为空";
            }
            course.setName(request.getName().trim());
        }
        if (request.getCapacity() != null) {
            int capacity = request.getCapacity().intValue();
            if (capacity < 40) {
                return "课程容量不能小于 40";
            }
            if (capacity > 100) {
                return "课程容量不能大于 100";
            }
            if (capacity < course.getEnrolledCount()) {
                return "课程容量不能小于已选人数";
            }
            course.setCapacity(capacity);
        }
        if (request.getTeacherUuid() != null) {
            String teacherUuid = request.getTeacherUuid().trim();
            if (teacherUuid.length() == 0) {
                clearTeacher(course);
            } else {
                Teacher teacher = m_course_dao.findTeacher(teacherUuid);
                if (teacher == null) {
                    return "授课教师不存在";
                }
                changeTeacher(course, teacher);
            }
        }
        if (request.getClassroomUuid() != null) {
            String classroomUuid = request.getClassroomUuid().trim();
            course.setClassroomUuid(classroomUuid.length() == 0 ? null : classroomUuid);
        }
        if (request.getTimeslot() != null) {
            course.getTimeslots().clear();
            course.getTimeslots().add(request.getTimeslot());
        }
        if (request.getRequiredDirections() != null) {
            course.setRequiredDirections(request.getRequiredDirections());
        }
        if (request.getEligibleMajors() != null) {
            course.setEligibleMajors(request.getEligibleMajors());
        }
        if (request.getCollegeUuid() != null) {
            String collegeUuid = request.getCollegeUuid().trim();
            course.setCollegeUuid(collegeUuid.length() == 0 ? null : collegeUuid);
        }
        if (request.getStartWeek() != null) {
            course.setStartWeek(request.getStartWeek());
        }
        if (request.getEndWeek() != null) {
            course.setEndWeek(request.getEndWeek());
        }
        return null;
    }

    /**
     * 删除课程（管理员）：已有学生选修的课程不允许删除。
     *
     * @param code 课程编号
     * @return 失败原因，成功为 null
     */
    public String deleteCourse(String code) {
        CourseSection course = m_course_dao.findCourseByCode(code == null ? null : code.trim());
        if (course == null) {
            return "课程不存在";
        }
        if (course.getEnrolledCount() > 0) {
            return "已有学生选修该课程，不能删除";
        }
        clearTeacher(course);
        m_course_dao.removeCourse(course.getUuid());
        return null;
    }

    /** @return 首个学院，没有则 null */
    private College defaultCollege() {
        List<College> colleges = m_course_dao.findAllColleges();
        return colleges.isEmpty() ? null : colleges.get(0);
    }

    /** 取消课程当前的授课教师。 */
    private void clearTeacher(CourseSection course) {
        String old = course.getTeacherUuid();
        if (old != null) {
            Teacher oldTeacher = m_course_dao.findTeacher(old);
            if (oldTeacher != null) {
                oldTeacher.getClaimedCourseUuids().remove(course.getUuid());
            }
        }
        course.setTeacherUuid(null);
    }

    /** 把课程改交给指定教师（维护教师侧认领索引）。 */
    private void changeTeacher(CourseSection course, Teacher teacher) {
        String old = course.getTeacherUuid();
        if (old != null && !old.equals(teacher.getUuid())) {
            Teacher oldTeacher = m_course_dao.findTeacher(old);
            if (oldTeacher != null) {
                oldTeacher.getClaimedCourseUuids().remove(course.getUuid());
            }
        }
        course.setTeacherUuid(teacher.getUuid());
        teacher.getClaimedCourseUuids().add(course.getUuid());
    }
}
