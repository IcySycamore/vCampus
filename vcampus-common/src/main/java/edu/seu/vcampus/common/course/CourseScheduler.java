package edu.seu.vcampus.common.course;

import java.util.ArrayList;
import java.util.List;

/**
 * 排课时间坐标与冲突检测（双端共享）。
 *
 * <p>一周按「周一到周日 × 每天 13 节」划分：上午 5 节、下午 5 节、晚上 3 节，课间 10 分钟。
 * 每节课时长默认 45 分钟，可在 30-50 分钟之间以 5 分钟步长全局调整（下课时间随之顺延）。
 * 本类只负责节次坐标与冲突检测；自动排课已移除，只保留手动排课。
 */
public final class CourseScheduler {

    /** 一周可排课的天数（周一到周日）。 */
    public static final int WEEKDAYS = 7;

    /** 每天节次数（上午 5 + 下午 5 + 晚上 3）。 */
    public static final int PERIODS = 13;

    /** 默认每节课时长（分钟）。 */
    public static final int DEFAULT_PERIOD_DURATION = 45;

    /** 每节课时长下限（分钟）。 */
    public static final int MIN_PERIOD_DURATION = 30;

    /** 每节课时长上限（分钟）。 */
    public static final int MAX_PERIOD_DURATION = 50;

    /** 课间休息（分钟）。 */
    public static final int BREAK_MINUTES = 10;

    /** 时间段划分：{起始节次, 起始分钟(自 00:00), 节数}——上午 5 节 08:00、下午 5 节 14:00、晚上 3 节 20:00。 */
    private static final int[][] SEGMENTS = {
        {0, 480, 5}, {5, 840, 5}, {10, 1200, 3}
    };

    /** 当前每节课时长（分钟），默认 45，可在 30-50 之间以 5 分钟步长调整。 */
    private static int periodDuration = DEFAULT_PERIOD_DURATION;

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

    /** @return 当前每节课时长（分钟）。 */
    public static int getPeriodDuration() {
        return periodDuration;
    }

    /** 设置每节课时长（分钟），自动限制在 30-50 区间。 */
    public static void setPeriodDuration(int duration) {
        periodDuration = Math.max(MIN_PERIOD_DURATION,
                Math.min(MAX_PERIOD_DURATION, duration));
    }

    /** @return 指定节次（0 起）的 [开始, 结束] 分钟，按当前每节课时长动态计算。 */
    public static int[] periodMinutes(int period) {
        for (int[] segment : SEGMENTS) {
            if (period >= segment[0] && period < segment[0] + segment[2]) {
                int start = segment[1] + (period - segment[0]) * (periodDuration + BREAK_MINUTES);
                return new int[] { start, start + periodDuration };
            }
        }
        throw new IllegalArgumentException("period out of range: " + period);
    }

    /** @return 节次名称（供网格行标题展示）。 */
    public static String periodName(int period) {
        return PERIOD_NAMES[period];
    }

    /** @return 节次对应的时间段文本，如 "08:00-08:45"。 */
    public static String periodTimeRange(int period) {
        int[] minutes = periodMinutes(period);
        return ScheduleTimeCalculator.format(minutes[0])
                + "-" + ScheduleTimeCalculator.format(minutes[1]);
    }

    /** @return 第 period 节（0 起）的开始分钟数。 */
    public static int periodStartMinute(int period) {
        return periodMinutes(period)[0];
    }

    /** @return 第 period 节（0 起）的结束分钟数。 */
    public static int periodEndMinute(int period) {
        return periodMinutes(period)[1];
    }

    /** @return 指定星期、第 startPeriod 节到第 endPeriod 节（含）的时间槽。 */
    public static Timeslot timeslotOf(int weekday, int startPeriod, int endPeriod) {
        return new Timeslot(weekday, periodMinutes(startPeriod)[0],
                periodMinutes(endPeriod)[1]);
    }

    /** @return 时间槽覆盖的节次范围 [起始节, 结束节]（0 起，含）；无交叠返回 null。 */
    public static int[] periodRangeOf(Timeslot timeslot) {
        if (timeslot == null) {
            return null;
        }
        int start = -1;
        int end = -1;
        for (int period = 0; period < PERIODS; period++) {
            if (periodTimeslot(timeslot.getWeekday(), period).overlaps(timeslot)) {
                if (start < 0) {
                    start = period;
                }
                end = period;
            }
        }
        if (start < 0) {
            return null;
        }
        return new int[] { start, end };
    }

    /** @return 时间槽对应的「第 a-b 节」文本；单节显示「第 a 节」。 */
    public static String periodRangeText(Timeslot timeslot) {
        int[] range = periodRangeOf(timeslot);
        if (range == null) {
            return "";
        }
        if (range[0] == range[1]) {
            return "第" + (range[0] + 1) + "节";
        }
        return "第" + (range[0] + 1) + "-" + (range[1] + 1) + "节";
    }

    /** @return 星期名称（供网格列标题展示）。 */
    public static String weekdayName(int weekday) {
        return WEEKDAY_NAMES[weekday - 1];
    }

    /** @return 指定星期/节次对应的时间槽。 */
    public static Timeslot periodTimeslot(int weekday, int period) {
        int[] minutes = periodMinutes(period);
        return new Timeslot(weekday, minutes[0], minutes[1]);
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
