package edu.seu.vcampus.client.course;

/**
 * 选课系统命令码常量（300-399）。
 *
 * <p>按 docs/应用层协议规定.md §4，300-399 归属选课系统。当前在客户端集中
 * 定义，待服务端选课模块落地后，应统一登记到 common 的 Command.java，此处
 * 改为引用该常量。
 */
public final class CourseCommand {

    /** 查询可选课程列表。 */
    public static final int COURSE_LIST = 300;

    /** 选课。 */
    public static final int COURSE_SELECT = 301;

    /** 退课。 */
    public static final int COURSE_DROP = 302;

    /** 成绩查询。 */
    public static final int SCORE_QUERY = 303;

    /** 成绩录入/修改。 */
    public static final int SCORE_SAVE = 304;

    /** 禁止实例化常量类。 */
    private CourseCommand() {
    }
}
