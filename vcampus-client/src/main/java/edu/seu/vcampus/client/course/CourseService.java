package edu.seu.vcampus.client.course;

import edu.seu.vcampus.client.network.ClientMessageDispatcher;
import edu.seu.vcampus.client.user.UserService;
import edu.seu.vcampus.common.course.Classroom;
import edu.seu.vcampus.common.course.Course;
import edu.seu.vcampus.common.course.Score;
import edu.seu.vcampus.common.course.Timeslot;
import edu.seu.vcampus.common.course.dto.CourseScheduleRequest;
import edu.seu.vcampus.common.message.Message;

import java.util.ArrayList;
import java.util.List;

/**
 * 选课客户端服务：给界面提供查询课程、选课、退课与成绩的同步方法。
 *
 * <p>调用约定：方法同步阻塞并直接返回结果；失败统一抛非受检
 * {@code ApiException}，界面用 {@code UiTasks.run(...)} 调用、不必 try/catch。
 * 身份一律由服务端按会话解析，查询「我的成绩」不传 uuid。
 */
public class CourseService {

    /** 请求发送与响应校验。 */
    private final CourseApiClient m_client;

    /** 用户模块 API：token 每次发请求时现取。 */
    private final UserService m_user;

    /**
     * 构造服务，使用默认 5 秒超时。
     *
     * @param dispatcher 消息分发器
     * @param user 用户模块 API（提供当前 token）
     */
    public CourseService(ClientMessageDispatcher dispatcher, UserService user) {
        this(dispatcher, user, 5000L);
    }

    /**
     * 构造服务。
     *
     * @param dispatcher 消息分发器
     * @param user 用户模块 API（提供当前 token）
     * @param timeoutMillis 请求超时，毫秒
     */
    public CourseService(ClientMessageDispatcher dispatcher, UserService user,
            long timeoutMillis) {
        if (dispatcher == null) {
            throw new IllegalArgumentException("dispatcher must not be null");
        }
        if (user == null) {
            throw new IllegalArgumentException("user must not be null");
        }
        this.m_user = user;
        this.m_client = new CourseApiClient(dispatcher, timeoutMillis);
    }

    /**
     * 查询可选课程列表（命令 300）。
     *
     * @return 可选课程
     */
    public List<Course> listCourses() {
        Message response = send(CourseCommand.COURSE_LIST, null);
        return toList(response.getData(), Course.class);
    }

    /**
     * 选课（命令 301）。
     *
     * @param courseCode 课程编号
     */
    public void selectCourse(String courseCode) {
        send(CourseCommand.COURSE_SELECT, courseCode);
    }

    /**
     * 退课（命令 302）。
     *
     * @param courseCode 课程编号
     */
    public void dropCourse(String courseCode) {
        send(CourseCommand.COURSE_DROP, courseCode);
    }

    /**
     * 查询「我的」成绩（命令 303）。
     *
     * @return 本人成绩列表
     */
    public List<Score> listMyScores() {
        Message response = send(CourseCommand.SCORE_QUERY, null);
        return toList(response.getData(), Score.class);
    }

    /**
     * 保存成绩（命令 304，教师/管理员使用）。
     *
     * @param score 成绩记录
     */
    public void saveScore(Score score) {
        send(CourseCommand.SCORE_SAVE, score);
    }

    /**
     * 查询「我授课的」课程列表（命令 305，教师视角）。
     *
     * @return 本人授课课程
     */
    public List<Course> listMyTeachingCourses() {
        Message response = send(CourseCommand.COURSE_TEACHING_LIST, null);
        return toList(response.getData(), Course.class);
    }

    /**
     * 查询教室列表（命令 309，管理员排课用）。
     *
     * @return 教室列表
     */
    public List<Classroom> listClassrooms() {
        Message response = send(CourseCommand.COURSE_CLASSROOM_LIST, null);
        return toList(response.getData(), Classroom.class);
    }

    /**
     * 手动排课（命令 306，管理员）。
     *
     * @param schedule 排课请求
     */
    public void scheduleCourse(CourseScheduleRequest schedule) {
        send(CourseCommand.COURSE_SCHEDULE, schedule);
    }

    /**
     * 查询本人偏好时间槽（命令 307，教师）。
     *
     * @return 偏好时间槽列表
     */
    public List<Timeslot> getMyPreferenceTimeslots() {
        Message response = send(CourseCommand.COURSE_PREFERENCE_GET, null);
        return toList(response.getData(), Timeslot.class);
    }

    /**
     * 设置本人偏好时间槽（命令 308，教师）。
     *
     * @param timeslots 偏好时间槽列表
     */
    public void setMyPreferenceTimeslots(List<Timeslot> timeslots) {
        send(CourseCommand.COURSE_PREFERENCE_SET, timeslots);
    }

    /** 发送请求（薄壳，真正的校验在 {@link CourseApiClient}）。 */
    private Message send(int command, Object data) {
        return m_client.call(command, data, m_user.currentToken());
    }

    private static <T> List<T> toList(Object data, Class<T> type) {
        List<T> result = new ArrayList<T>();
        if (data instanceof List) {
            for (Object value : (List<?>) data) {
                if (type.isInstance(value)) {
                    result.add(type.cast(value));
                }
            }
        }
        return result;
    }
}
