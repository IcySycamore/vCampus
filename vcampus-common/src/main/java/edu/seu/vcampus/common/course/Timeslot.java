package edu.seu.vcampus.common.course;

import java.io.Serializable;

/**
 * 周时间槽（值对象）：一周内某天的某个连续时间段，以「分钟」为最小粒度。
 *
 * <p>用于表示教师/学生/教室的可用时间、偏好时间，以及课程被安排的上课时间。提供
 * 两种比较：{@link #overlaps(Timeslot)}（是否冲突，用于学生排课不重叠）与
 * {@link #covers(Timeslot)}（是否覆盖，用于「上课时间须落在可用时间槽内」的校验）。
 */
public final class Timeslot implements Serializable {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 一天的总分钟数。 */
    private static final int DAY_MINUTES = 24 * 60;

    /** 星期几：1=周一 … 7=周日。 */
    private final int weekday;

    /** 开始时间（自 00:00 起的分钟数，含）。 */
    private final int startMinute;

    /** 结束时间（自 00:00 起的分钟数，不含）。 */
    private final int endMinute;

    /**
     * 构造一个时间槽。
     *
     * @param weekday 星期几（1..7）
     * @param startMinute 开始分钟数
     * @param endMinute 结束分钟数（必须大于开始分钟数）
     */
    public Timeslot(int weekday, int startMinute, int endMinute) {
        if (weekday < 1 || weekday > 7) {
            throw new IllegalArgumentException("weekday must be 1..7");
        }
        if (startMinute < 0 || endMinute > DAY_MINUTES || startMinute >= endMinute) {
            throw new IllegalArgumentException("invalid time range");
        }
        this.weekday = weekday;
        this.startMinute = startMinute;
        this.endMinute = endMinute;
    }

    /** @return 星期几（1=周一 … 7=周日） */
    public int getWeekday() {
        return weekday;
    }

    /** @return 开始分钟数 */
    public int getStartMinute() {
        return startMinute;
    }

    /** @return 结束分钟数 */
    public int getEndMinute() {
        return endMinute;
    }

    /**
     * 判断两个时间槽是否冲突：同一天且区间有交叠。
     *
     * @param other 另一个时间槽
     * @return 是否冲突
     */
    public boolean overlaps(Timeslot other) {
        if (other == null || this.weekday != other.weekday) {
            return false;
        }
        return this.startMinute < other.endMinute && other.startMinute < this.endMinute;
    }

    /**
     * 判断本时间槽是否完整覆盖另一个时间槽（同一天且区间包含）。
     *
     * @param other 被覆盖的时间槽
     * @return 是否覆盖
     */
    public boolean covers(Timeslot other) {
        if (other == null || this.weekday != other.weekday) {
            return false;
        }
        return this.startMinute <= other.startMinute && this.endMinute >= other.endMinute;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Timeslot)) {
            return false;
        }
        Timeslot other = (Timeslot) obj;
        return this.weekday == other.weekday && this.startMinute == other.startMinute
                && this.endMinute == other.endMinute;
    }

    @Override
    public int hashCode() {
        int result = weekday;
        result = 31 * result + startMinute;
        result = 31 * result + endMinute;
        return result;
    }

    @Override
    public String toString() {
        return "周" + weekday + " " + format(startMinute) + "-" + format(endMinute);
    }

    private static String format(int minute) {
        int hour = minute / 60;
        int min = minute % 60;
        return (hour < 10 ? "0" : "") + hour + ":" + (min < 10 ? "0" : "") + min;
    }
}
