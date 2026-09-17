package edu.seu.vcampus.server.course;

import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.course.CourseSection;
import edu.seu.vcampus.common.course.dto.CourseScheduleRequest;
import edu.seu.vcampus.common.course.Score;
import edu.seu.vcampus.common.course.Timeslot;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.user.entity.SessionEntry;
import edu.seu.vcampus.server.user.UserRepository;

import java.util.List;

/**
 * 选课「写」命令执行器：选课 / 退课 / 录成绩 / 排课 / 设置偏好时间槽。
 *
 * <p>与 {@link CourseCommandExecutor} 同构：handler 只管鉴权，本类只负责拿到已通过鉴权的请求后
 * 落业务。返回 {@code String} 的失败原因统一回 400，成功回 200。
 */
final class CourseWriteExecutor {

    private final CourseDao m_dao;
    private final CourseManagementService m_management;
    private final CourseService m_service;
    private final UserRepository m_users;

    /**
     * 构造写命令执行器。
     *
     * @param dao 课程数据访问
     * @param management 课程管理服务
     * @param service 选课业务服务
     * @param users 用户凭证存储（用于把登录名解析成 uuid，可为 null）
     */
    CourseWriteExecutor(CourseDao dao, CourseManagementService management,
            CourseService service, UserRepository users) {
        this.m_dao = dao;
        this.m_management = management;
        this.m_service = service;
        this.m_users = users;
    }

    /**
     * 执行一条写命令；无法识别时返回 false 交由调用方兜底。
     *
     * @param command 命令码
     * @param request 请求
     * @param response 响应
     * @param actor 会话条目
     * @return 是否已处理
     */
    boolean execute(int command, Message request, Message response, SessionEntry actor) {
        if (command == Command.COURSE_SELECT) {
            selectCourse(request, response, actor);
        } else if (command == Command.COURSE_DROP) {
            dropCourse(request, response, actor);
        } else if (command == Command.SCORE_SAVE) {
            saveScore(request, response);
        } else if (command == Command.COURSE_SCHEDULE) {
            schedule(request, response);
        } else if (command == Command.COURSE_PREFERENCE_SET) {
            setPreference(request, response, actor);
        } else {
            return false;
        }
        return true;
    }

    private void selectCourse(Message request, Message response, SessionEntry actor) {
        CourseSection course = courseOf(request.getData());
        if (course == null) {
            fail(response, "课程不存在或未指定课程编号");
            return;
        }
        String error = m_service.selectCourse(actor.getUuid(), course.getUuid());
        finish(response, error);
    }

    private void dropCourse(Message request, Message response, SessionEntry actor) {
        CourseSection course = courseOf(request.getData());
        if (course == null) {
            fail(response, "课程不存在或未指定课程编号");
            return;
        }
        String error = m_service.dropCourse(actor.getUuid(), course.getUuid());
        finish(response, error);
    }

    private void saveScore(Message request, Message response) {
        if (!(request.getData() instanceof Score)) {
            fail(response, "成绩参数不正确");
            return;
        }
        Score score = (Score) request.getData();
        CourseSection course = findCourseByCode(score.getCourseCode());
        if (course == null) {
            fail(response, "课程不存在");
            return;
        }
        String studentUuid = resolveStudentUuid(score.getStudentUuid());
        if (studentUuid == null) {
            fail(response, "学生不存在");
            return;
        }
        if (score.getScore() == null) {
            fail(response, "成绩不能为空");
            return;
        }
        String error = m_service.recordScore(course.getUuid(), studentUuid,
                score.getScore().doubleValue());
        finish(response, error);
    }

    private void schedule(Message request, Message response) {
        if (!(request.getData() instanceof CourseScheduleRequest)) {
            fail(response, "排课参数不正确");
            return;
        }
        CourseScheduleRequest schedule = (CourseScheduleRequest) request.getData();
        CourseSection course = findCourseByCode(schedule.getCourseCode());
        if (course == null) {
            fail(response, "课程不存在");
            return;
        }
        String error = m_management.scheduleCourse(course.getUuid(),
                schedule.getClassroomUuid(), schedule.getTimeslots());
        finish(response, error);
    }

    private void setPreference(Message request, Message response, SessionEntry actor) {
        if (!(request.getData() instanceof List)) {
            fail(response, "偏好时间槽参数不正确");
            return;
        }
        List<Timeslot> timeslots = (List<Timeslot>) request.getData();
        String error = m_management.setTeacherPreferenceTimeslots(actor.getUuid(), timeslots);
        finish(response, error);
    }

    private CourseSection courseOf(Object data) {
        if (!(data instanceof String)) {
            return null;
        }
        return findCourseByCode((String) data);
    }

    private CourseSection findCourseByCode(String code) {
        if (code == null) {
            return null;
        }
        for (CourseSection course : m_dao.findAllCourses()) {
            if (code.equals(course.getCode())) {
                return course;
            }
        }
        return null;
    }

    private String resolveStudentUuid(String ref) {
        if (ref == null) {
            return null;
        }
        if (m_dao.findStudent(ref) != null) {
            return ref;
        }
        if (m_users != null) {
            UserRepository.Credential credential = m_users.findByUsername(ref);
            if (credential != null) {
                return credential.getUuid();
            }
        }
        return null;
    }

    private void finish(Message response, String error) {
        if (error != null) {
            fail(response, error);
            return;
        }
        response.setStatusCode(StatusCode.SUCCESS);
        response.setData(null);
    }

    private void fail(Message response, String message) {
        response.setStatusCode(StatusCode.BAD_REQUEST);
        response.setData(message);
    }
}
