package edu.seu.vcampus.client.course;

import edu.seu.vcampus.common.constant.Command;

/**
 * 选课系统命令码常量（300-399）。
 *
 * <p>按 docs/应用层协议规定.md §4，300-399 归属选课系统。命令码统一登记在 common 的
 * {@link Command}，此处只做引用，保证双端一致。
 */
public final class CourseCommand {

    /** 查询可选课程列表。 */
    public static final int COURSE_LIST = Command.COURSE_LIST;

    /** 选课。 */
    public static final int COURSE_SELECT = Command.COURSE_SELECT;

    /** 退课。 */
    public static final int COURSE_DROP = Command.COURSE_DROP;

    /** 成绩查询。 */
    public static final int SCORE_QUERY = Command.SCORE_QUERY;

    /** 成绩录入/修改。 */
    public static final int SCORE_SAVE = Command.SCORE_SAVE;

    /** 查询本人授课课程（教师）。 */
    public static final int COURSE_TEACHING_LIST = Command.COURSE_TEACHING_LIST;

    /** 手动排课（管理员）。 */
    public static final int COURSE_SCHEDULE = Command.COURSE_SCHEDULE;

    /** 查询本人偏好时间槽（教师）。 */
    public static final int COURSE_PREFERENCE_GET = Command.COURSE_PREFERENCE_GET;

    /** 设置本人偏好时间槽（教师）。 */
    public static final int COURSE_PREFERENCE_SET = Command.COURSE_PREFERENCE_SET;

    /** 查询教室列表（管理员）。 */
    public static final int COURSE_CLASSROOM_LIST = Command.COURSE_CLASSROOM_LIST;

    /** 禁止实例化常量类。 */
    private CourseCommand() {
    }
}
