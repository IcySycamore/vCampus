package edu.seu.vcampus.client.course;

import edu.seu.vcampus.common.course.Score;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 绩点计算工具测试。
 */
public class GpaCalculatorTest {

    /**
     * 应按学分加权平均各门绩点。
     */
    @Test
    void weightedGpaAveragesByCredit() {
        List<ScoreRecord> records = new ArrayList<ScoreRecord>();
        records.add(record(90.0, 3));
        records.add(record(80.0, 2));

        assertEquals(3.6, GpaCalculator.weightedGpa(records), 0.001);
    }

    /**
     * 未录入成绩的记录不应参与 GPA 计算。
     */
    @Test
    void weightedGpaIgnoresUnscoredRecords() {
        List<ScoreRecord> records = new ArrayList<ScoreRecord>();
        Score unscored = new Score("s1", "CS101", "2026-2027-1");
        records.add(new ScoreRecord(unscored, "数据结构", 3));
        records.add(record(90.0, 3));

        assertEquals(4.0, GpaCalculator.weightedGpa(records), 0.001);
    }

    /**
     * 空或 null 记录应返回默认绩点 0。
     */
    @Test
    void weightedGpaReturnsZeroWhenEmpty() {
        assertEquals(0.0, GpaCalculator.weightedGpa(null), 0.001);
        assertEquals(0.0, GpaCalculator.weightedGpa(new ArrayList<ScoreRecord>()), 0.001);
    }

    /**
     * 百分制分数应按分档映射为四分制绩点。
     */
    @Test
    void scoreToPointMapsScoreBands() {
        assertEquals(4.0, GpaCalculator.scoreToPoint(95), 0.001);
        assertEquals(4.0, GpaCalculator.scoreToPoint(90), 0.001);
        assertEquals(3.0, GpaCalculator.scoreToPoint(89.9), 0.001);
        assertEquals(3.0, GpaCalculator.scoreToPoint(80), 0.001);
        assertEquals(2.0, GpaCalculator.scoreToPoint(70), 0.001);
        assertEquals(1.0, GpaCalculator.scoreToPoint(60), 0.001);
        assertEquals(0.0, GpaCalculator.scoreToPoint(59.9), 0.001);
    }

    private ScoreRecord record(double value, int credit) {
        Score score = new Score("s1", "CS101", "2026-2027-1");
        score.setScore(Double.valueOf(value));
        return new ScoreRecord(score, "数据结构", credit);
    }
}

