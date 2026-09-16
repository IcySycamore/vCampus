package edu.seu.vcampus.client.course;

import edu.seu.vcampus.common.course.Score;

import java.io.Serializable;

/**
 * 成绩界面使用的展示记录。
 *
 * <p>在 {@link Score} 基础上补充课程名称与学分，供成绩表格渲染与 GPA
 * 计算使用。课程名称与学分为可选字段，缺失时界面以占位符展示。
 */
public final class ScoreRecord implements Serializable {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 成绩主体。 */
    private final Score score;

    /** 课程名称；缺失时为 null。 */
    private final String courseName;

    /** 学分；缺失时为 0。 */
    private final int credit;

    /**
     * 构造成绩展示记录。
     *
     * @param score 成绩主体
     * @param courseName 课程名称
     * @param credit 学分
     */
    public ScoreRecord(Score score, String courseName, int credit) {
        this.score = score;
        this.courseName = courseName;
        this.credit = credit;
    }

    /**
     * 构造不含课程信息的成绩展示记录。
     *
     * @param score 成绩主体
     */
    public ScoreRecord(Score score) {
        this(score, null, 0);
    }

    /** @return 成绩主体 */
    public Score getScore() {
        return score;
    }

    /** @return 课程名称 */
    public String getCourseName() {
        return courseName;
    }

    /** @return 学分 */
    public int getCredit() {
        return credit;
    }
}
