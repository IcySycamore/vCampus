package edu.seu.vcampus.common.course.dto;

import edu.seu.vcampus.common.course.Timeslot;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 排课请求（值对象）：管理员把一门课程安排到某个教室的若干时间槽。
 *
 * <p>课程以「课程编号」（如 CS101）引用，服务端据此解析课程后再调用排课逻辑；
 * 时间槽须落在教室与教师的可用时间槽内，具体校验见排课服务。
 */
public class CourseScheduleRequest implements Serializable {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 课程编号（如 CS101）。 */
    private String m_course_code;

    /** 目标教室 uuid。 */
    private String m_classroom_uuid;

    /** 上课时间槽集合。 */
    private List<Timeslot> m_timeslots = new ArrayList<Timeslot>();

    /** 构造一个空排课请求，供对象流填充字段。 */
    public CourseScheduleRequest() {
    }

    /** @return 课程编号 */
    public String getCourseCode() {
        return m_course_code;
    }

    /** @param courseCode 课程编号 */
    public void setCourseCode(String courseCode) {
        this.m_course_code = courseCode;
    }

    /** @return 目标教室 uuid */
    public String getClassroomUuid() {
        return m_classroom_uuid;
    }

    /** @param classroomUuid 目标教室 uuid */
    public void setClassroomUuid(String classroomUuid) {
        this.m_classroom_uuid = classroomUuid;
    }

    /** @return 上课时间槽集合 */
    public List<Timeslot> getTimeslots() {
        return m_timeslots;
    }

    /** @param timeslots 上课时间槽集合 */
    public void setTimeslots(List<Timeslot> timeslots) {
        this.m_timeslots = timeslots == null ? new ArrayList<Timeslot>() : timeslots;
    }
}
