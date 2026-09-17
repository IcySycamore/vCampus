package edu.seu.vcampus.common.course;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 上课时间段联动计算测试。
 */
class ScheduleTimeCalculatorTest {

    @Test
    void computesEndFromStartAndDuration() {
        assertEquals(Integer.valueOf(580), ScheduleTimeCalculator.endOf(480, 100));
        assertNull(ScheduleTimeCalculator.endOf(-1, 100));
        assertNull(ScheduleTimeCalculator.endOf(480, 0));
        assertNull(ScheduleTimeCalculator.endOf(1400, 100)); // 超出 24:00
    }

    @Test
    void computesDurationFromStartAndEnd() {
        assertEquals(Integer.valueOf(100), ScheduleTimeCalculator.durationOf(480, 580));
        assertNull(ScheduleTimeCalculator.durationOf(580, 480)); // 结束早于开始
        assertNull(ScheduleTimeCalculator.durationOf(480, 480)); // 相等
    }

    @Test
    void computesStartFromEndAndDuration() {
        assertEquals(Integer.valueOf(480), ScheduleTimeCalculator.startOf(580, 100));
        assertNull(ScheduleTimeCalculator.startOf(50, 100)); // 开始会为负
    }

    @Test
    void formatsAndParsesTime() {
        assertEquals("08:00", ScheduleTimeCalculator.format(480));
        assertEquals("12:25", ScheduleTimeCalculator.format(745));
        assertEquals(480, ScheduleTimeCalculator.parse("08:00"));
        assertEquals(745, ScheduleTimeCalculator.parse("12:25"));
        assertEquals(-1, ScheduleTimeCalculator.parse("25:00"));
        assertEquals(-1, ScheduleTimeCalculator.parse("abc"));
        assertEquals(-1, ScheduleTimeCalculator.parse(null));
    }
}
