package edu.seu.vcampus.client.course;

import java.util.List;

/**
 * 按学分加权计算平均绩点（GPA）的工具类。
 */
final class GpaCalculator {

    /** 无成绩记录时的默认 GPA。 */
    private static final double DEFAULT_GPA = 0.0;

    private GpaCalculator() {
    }

    /**
     * 计算绩点：按学分加权平均各门已录入成绩的绩点。
     *
     * @param records 成绩展示记录
     * @return 加权平均绩点；无有效记录时为 0
     */
    static double weightedGpa(List<ScoreRecord> records) {
        if (records == null || records.isEmpty()) {
            return DEFAULT_GPA;
        }
        double totalPoints = 0.0;
        double totalCredit = 0.0;
        for (ScoreRecord record : records) {
            Double value = record.getScore().getScore();
            if (value == null) {
                continue;
            }
            double credit = Math.max(record.getCredit(), 0.0);
            totalPoints += scoreToPoint(value.doubleValue()) * credit;
            totalCredit += credit;
        }
        if (totalCredit <= 0.0) {
            return DEFAULT_GPA;
        }
        return totalPoints / totalCredit;
    }

    /**
     * 将百分制分数转换为四分制绩点。
     *
     * @param score 百分制分数
     * @return 对应绩点
     */
    static double scoreToPoint(double score) {
        if (score >= 90.0) {
            return 4.0;
        }
        if (score >= 80.0) {
            return 3.0;
        }
        if (score >= 70.0) {
            return 2.0;
        }
        if (score >= 60.0) {
            return 1.0;
        }
        return 0.0;
    }
}
