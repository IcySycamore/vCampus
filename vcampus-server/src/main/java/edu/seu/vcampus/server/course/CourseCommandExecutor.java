package edu.seu.vcampus.server.course;

import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.course.Course;
import edu.seu.vcampus.common.course.CourseSection;
import edu.seu.vcampus.common.course.Student;
import edu.seu.vcampus.common.course.Teacher;
import edu.seu.vcampus.common.course.Timeslot;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.user.entity.Role;
import edu.seu.vcampus.common.user.entity.SessionEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * 选课「读」命令执行器：课程列表 / 我的授课 / 我的成绩 / 我的偏好时间槽 / 教室列表。
 *
 * <p>
 * 从 {@link CourseMessageHandler} 拆出：handler 只管 token → 角色 → 能力判定，本类只负责 拿到已通过鉴权的请求后落业务。写命令统一委托给
 * {@link CourseWriteExecutor}。
 */
final class CourseCommandExecutor {

    private final CourseDao m_dao;
    private final CourseManagementService m_management;
    private final CourseService m_service;
    private final CourseWriteExecutor m_writes;

    /**
     * 构造读命令执行器。
     *
     * @param dao        课程数据访问
     * @param management 课程管理服务
     * @param service    选课业务服务
     * @param users      用户凭证存储（写命令把登录名解析成 uuid 用）
     */
    CourseCommandExecutor(CourseDao dao, CourseManagementService management,
            CourseService service, edu.seu.vcampus.server.user.UserRepository users) {
        this.m_dao = dao;
        this.m_management = management;
        this.m_service = service;
        this.m_writes = new CourseWriteExecutor(dao, management, service, users);
    }

    /**
     * 执行一条已经通过鉴权的选课命令。
     *
     * @param request  请求消息
     * @param response 待填充的响应
     * @param actor    已解析出的会话条目
     */
    void execute(Message request, Message response, SessionEntry actor) {
        int command = request.getCommand();
        if (command == Command.COURSE_LIST) {
            listCourses(response);
        } else if (command == Command.SCORE_QUERY) {
            listMyScores(response, actor);
        } else if (command == Command.COURSE_TEACHING_LIST) {
            listTeaching(response, actor);
        } else if (command == Command.COURSE_PREFERENCE_GET) {
            getPreference(response, actor);
        } else if (command == Command.COURSE_TEACHER_LIST) {
            listTeachers(response);
        } else if (command == Command.COURSE_MY_SELECTIONS) {
            listMySelections(response, actor);
        } else if (command == Command.COURSE_AVAILABLE_GET) {
            getAvailable(response, actor);
        } else if (command == Command.COURSE_CLASSROOM_LIST) {
            listClassrooms(response);
        } else if (command == Command.COURSE_COLLEGE_LIST) {
            listColleges(response);
        } else if (!m_writes.execute(command, request, response, actor)) {
            response.setStatusCode(StatusCode.BAD_REQUEST);
            response.setData("未知的选课命令");
        }
    }

    private void listCourses(Message response) {
        response.setStatusCode(StatusCode.SUCCESS);
        response.setData(toCourses(m_management.listCourses()));
    }

    private void listMyScores(Message response, SessionEntry actor) {
        response.setStatusCode(StatusCode.SUCCESS);
        response.setData(m_service.listScoresByStudent(actor.getUuid()));
    }

    private void listTeaching(Message response, SessionEntry actor) {
        Role role = Role.fromDisplayName(actor.getRole());
        List<CourseSection> courses;
        if (role == Role.TEACHER) {
            courses = m_management.listCoursesByTeacher(actor.getUuid());
        } else if (role == Role.ADMIN) {
            courses = m_management.listCourses();
        } else {
            courses = new ArrayList<CourseSection>();
        }
        response.setStatusCode(StatusCode.SUCCESS);
        response.setData(toCourses(courses));
    }

    private void getPreference(Message response, SessionEntry actor) {
        List<Timeslot> timeslots = new ArrayList<Timeslot>();
        Teacher teacher = m_dao.findTeacher(actor.getUuid());
        if (teacher != null) {
            timeslots.addAll(teacher.getPreferenceTimeslots());
        }
        response.setStatusCode(StatusCode.SUCCESS);
        response.setData(timeslots);
    }

    private void listClassrooms(Message response) {
        response.setStatusCode(StatusCode.SUCCESS);
        response.setData(m_dao.findAllClassrooms());
    }

    private void listTeachers(Message response) {
        response.setStatusCode(StatusCode.SUCCESS);
        response.setData(m_dao.findAllTeachers());
    }

    private void listColleges(Message response) {
        response.setStatusCode(StatusCode.SUCCESS);
        response.setData(m_dao.findAllColleges());
    }

    private void listMySelections(Message response, SessionEntry actor) {
        List<Course> result = new ArrayList<Course>();
        Student student = m_dao.findStudent(actor.getUuid());
        if (student != null) {
            for (String uuid : student.getSelectedCourseUuids()) {
                CourseSection section = m_dao.findCourse(uuid);
                if (section != null) {
                    result.add(toCourse(section));
                }
            }
        }
        response.setStatusCode(StatusCode.SUCCESS);
        response.setData(result);
    }

    private void getAvailable(Message response, SessionEntry actor) {
        List<Timeslot> timeslots = new ArrayList<Timeslot>();
        Teacher teacher = m_dao.findTeacher(actor.getUuid());
        if (teacher != null) {
            timeslots.addAll(teacher.getAvailableTimeslots());
        }
        response.setStatusCode(StatusCode.SUCCESS);
        response.setData(timeslots);
    }

    private List<Course> toCourses(List<CourseSection> sections) {
        List<Course> result = new ArrayList<Course>();
        for (CourseSection section : sections) {
            result.add(toCourse(section));
        }
        return result;
    }

    private String teacherNameOf(String teacherUuid) {
        if (teacherUuid == null) {
            return null;
        }
        Teacher teacher = m_dao.findTeacher(teacherUuid);
        return teacher == null ? null : teacher.getName();
    }

    private Course toCourse(CourseSection section) {
        Course course = new Course();
        course.setUuid(section.getUuid());
        course.setCode(section.getCode());
        course.setName(section.getName());
        course.setCredit(section.getCredit());
        course.setTeacherUuid(section.getTeacherUuid());
        course.setTeacherName(teacherNameOf(section.getTeacherUuid()));
        course.setCapacity(section.getCapacity());
        course.setEnrolled(section.getEnrolledCount());
        course.setSemester(section.getSemester());
        course.setStartWeek(section.getStartWeek());
        course.setEndWeek(section.getEndWeek());
        course.setClassroomUuid(section.getClassroomUuid());
        course.setRequiredDirections(section.getRequiredDirections());
        course.setEligibleMajors(section.getEligibleMajors());
        course.setCollegeUuid(section.getCollegeUuid());
        Timeslot first = null;
        for (Timeslot slot : section.getTimeslots()) {
            first = slot;
            break;
        }
        course.setTimeslot(first);
        return course;
    }
}
