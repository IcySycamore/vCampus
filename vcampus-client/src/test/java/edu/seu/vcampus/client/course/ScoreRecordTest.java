package edu.seu.vcampus.client.course;

import edu.seu.vcampus.common.course.Score;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 成绩展示记录测试。
 */
public class ScoreRecordTest {

    /**
     * 应保存成绩主体、课程名称与学分。
     */
    @Test
    void storesScoreNameAndCredit() {
        Score score = new Score("s1", "CS101", "2026-2027-1");
        ScoreRecord record = new ScoreRecord(score, "数据结构", 3);

        assertEquals(score, record.getScore());
        assertEquals("数据结构", record.getCourseName());
        assertEquals(3, record.getCredit());
    }

    /**
     * 缺省课程信息时名称应为 null、学分为 0。
     */
    @Test
    void defaultsMissingCourseInfo() {
        ScoreRecord record = new ScoreRecord(new Score("s1", "CS101", "2026-2027-1"));

        assertNull(record.getCourseName());
        assertEquals(0, record.getCredit());
    }
}

