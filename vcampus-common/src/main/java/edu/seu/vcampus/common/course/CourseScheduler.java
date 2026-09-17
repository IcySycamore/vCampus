package edu.seu.vcampus.common.course;

import java.util.ArrayList;
import java.util.List;

/**
 * 排课时间坐标与冲突检测（纯函数，双端共享）。
 *
 * <p>一周按「周一到周日 × 每天 13 节」划分：上午 5 节（08:00-12:25）、下午 5 节（14:00-18:25）、
 * 晚上 3 节（20:00-22:35），每节 45 分钟、课间 10 分钟。每门课占一个时间槽（一个格子）。
 * 本类只负责节次坐标与冲突检测；自动排课已移除，只保留手动排课。
 */
public final class CourseScheduler {

    /** 一周可排课的天数（周一到周日）。 */
    public static final int WEEKDAYS = 7;

    /** 每天节次数（上午 5 + 下午 5 + 晚上 3）。 */
    public static final int PERIODS = 13;

    /** 每节的时间段（开始/结束分钟，自 00:00 起）。 */
    private static final int[][] PERIOD_MINUTES = {
        {480, 525}, {535, 580}, {590, 635}, {645, 690}, {700, 745},
        {840, 885}, {895, 940}, {950, 995}, {1005, 1050}, {1060, 1105},
        {1200, 1245}, {1255, 1300}, {1310, 1355}
    };

    /** 节次名称。 */
    private static final String[] PERIOD_NAMES = {
        "第1节", "第2节", "第3节", "第4节", "第5节", "第6节", "第7节",
        "第8节", "第9节", "第10节", "第11节", "第12节", "第13节"
    };

    /** 星期名称。 */
    private static final String[] WEEKDAY_NAMES = {
        "周一", "周二", "周三", "周四", "周五", "周六", "周日"
    };

    private CourseScheduler() {
    }

    /** @return 节次名称（供网格行标题展示）。 */
    public static String periodName(int period) {
        return PERIOD_NAMES[period];
    }

    /** @return 星期名称（供网格列标题展示）。 */
    public static String weekdayName(int weekday) {
        return WEEKDAY_NAMES[weekday - 1];
    }

    /** @return 指定星期/节次对应的时间槽。 */
    public static Timeslot periodTimeslot(int weekday, int period) {
        return new Timeslot(weekday, PERIOD_MINUTES[period][0], PERIOD_MINUTES[period][1]);
    }

    /**
     * 冲突检测：返回冲突原因列表，空列表表示可排。
     *
     * @param candidate 待排条目（含教室与时间槽）
     * @param existing 已排条目
     * @param classroom 目标教室（可空，空则提示选教室）
     * @param teacher 授课教师（可空，空则跳过教师可用时间校验）
     * @return 冲突原因；无冲突返回空列表
     */
    public static List<String> conflicts(ScheduleEntry candidate, List<ScheduleEntry> existing,
            Classroom classroom, Teacher teacher) {
        List<String> reasons = new ArrayList<String>();
        if (candidate.getTimeslot() == null) {
            reasons.add("请选择上课时间");
            return reasons;
        }
        if (classroom == null) {
            reasons.add("请选择教室");
            return reasons;
        }
        if (classroom.getCapacity() < candidate.getCapacity()
                || classroom.getCapacity() < candidate.getEnrolled()) {
            reasons.add("教室容量不足");
        }
        if (teacher != null && !CourseRules.coveredByAny(teacher.getAvailableTimeslots(),
                candidate.getTimeslot())) {
            reasons.add("上课时间不在教师可用时间槽内");
        }
        if (!CourseRules.coveredByAny(classroom.getAvailableTimeslots(), candidate.getTimeslot())) {
            reasons.add("上课时间不在教室可用时间槽内");
        }
        for (ScheduleEntry entry : existing) {
            if (entry.getTimeslot() == null
                    || !candidate.getTimeslot().overlaps(entry.getTimeslot())) {
                continue;
            }
            if (candidate.getTeacherUuid() != null
                    && candidate.getTeacherUuid().equals(entry.getTeacherUuid())) {
                reasons.add("该教师此时已有课（" + entry.getCourseName() + "）");
            }
            if (candidate.getClassroomUuid() != null
                    && candidate.getClassroomUuid().equals(entry.getClassroomUuid())) {
                reasons.add("该教室此时已被占用（" + entry.getCourseName() + "）");
            }
        }
        return reasons;
    }
}
