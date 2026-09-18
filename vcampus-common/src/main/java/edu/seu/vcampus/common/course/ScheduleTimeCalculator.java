package edu.seu.vcampus.common.course;

/**
 * 上课时间段计算（纯函数，双端共享）：开始时间、结束时间、持续时长三者联动。
 *
 * <p>时间以「自 00:00 起的分钟数」表示。规则：必须先有开始时间，才能由结束时间或持续时长
 * 推算其余；结束时间与持续时长二者之一变化时，另一个随之变化。
 */
public final class ScheduleTimeCalculator {

    /** 一天的分钟数。 */
    public static final int DAY_MINUTES = 24 * 60;

    private ScheduleTimeCalculator() {
    }

    /** @return 开始时间是否合法（00:00–23:59 之间）。 */
    public static boolean validStart(int start) {
        return start >= 0 && start < DAY_MINUTES;
    }

    /** @return 结束时间是否合法（00:00–24:00 之间）。 */
    public static boolean validEnd(int end) {
        return end >= 0 && end <= DAY_MINUTES;
    }

    /** 由「开始 + 持续时长」算结束时间；参数非法或超界返回 null。 */
    public static Integer endOf(int start, int duration) {
        if (!validStart(start) || duration <= 0) {
            return null;
        }
        int end = start + duration;
        return end <= DAY_MINUTES ? Integer.valueOf(end) : null;
    }

    /** 由「开始 + 结束」算持续时长；结束必须晚于开始。 */
    public static Integer durationOf(int start, int end) {
        if (!validStart(start) || !validEnd(end)) {
            return null;
        }
        return end > start ? Integer.valueOf(end - start) : null;
    }

    /** 由「结束 + 持续时长」反推开始时间。 */
    public static Integer startOf(int end, int duration) {
        if (!validEnd(end) || duration <= 0) {
            return null;
        }
        int start = end - duration;
        return start >= 0 ? Integer.valueOf(start) : null;
    }

    /** 分钟 → "HH:MM"。 */
    public static String format(int minute) {
        int hour = minute / 60;
        int min = minute % 60;
        return (hour < 10 ? "0" : "") + hour + ":" + (min < 10 ? "0" : "") + min;
    }

    /** "HH:MM" → 分钟；非法返回 -1。 */
    public static int parse(String text) {
        if (text == null) {
            return -1;
        }
        String[] parts = text.trim().split(":");
        if (parts.length != 2) {
            return -1;
        }
        try {
            int hour = Integer.parseInt(parts[0]);
            int min = Integer.parseInt(parts[1]);
            if (hour < 0 || hour > 23 || min < 0 || min > 59) {
                return -1;
            }
            return hour * 60 + min;
        } catch (NumberFormatException exception) {
            return -1;
        }
    }
}
