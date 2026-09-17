package edu.seu.vcampus.client.course;

import edu.seu.vcampus.common.course.Score;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 成绩控制器测试。
 */
class ScoreControllerTest {

    /**
     * 应用筛选后应渲染成绩行并刷新 GPA。
     */
    @Test
    void applyFilterRendersScoresAndGpa() {
        ScorePanel panel = new ScorePanel();
        panel.renderScores(records());

        assertEquals(2, panel.getScoreCount());
        assertEquals("GPA：3.60", panel.getGpaText());
    }

    /**
     * 表单未填全时应提示补齐。
     */
    @Test
    void saveSelectedRequiresAllFields() {
        ScorePanel panel = new ScorePanel("教师");

        panel.controller.saveSelected();

        assertTrue(panel.getStatusText().contains("请填写"));
    }

    /**
     * 成绩非数字时应提示重新输入。
     */
    @Test
    void saveSelectedRejectsNonNumericScore() {
        ScorePanel panel = new ScorePanel("教师");
        panel.studentIdField.setText("s1");
        panel.courseCodeField.setText("CS101");
        panel.scoreField.setText("abc");

        panel.controller.saveSelected();

        assertTrue(panel.getStatusText().contains("数字"));
    }

    private static List<ScoreRecord> records() {
        List<ScoreRecord> records = new ArrayList<ScoreRecord>();
        records.add(record(90.0, 3));
        records.add(record(80.0, 2));
        return records;
    }

    private static ScoreRecord record(double value, int credit) {
        Score score = new Score("s1", "CS101", "2026-2027-1");
        score.setScore(Double.valueOf(value));
        return new ScoreRecord(score, "数据结构", credit);
    }
}

