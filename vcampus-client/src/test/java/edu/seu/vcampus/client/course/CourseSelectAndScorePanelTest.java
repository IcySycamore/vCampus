package edu.seu.vcampus.client.course;

import edu.seu.vcampus.common.course.Course;
import edu.seu.vcampus.common.course.Score;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 选课面板与成绩面板的单元测试。
 */
class CourseSelectAndScorePanelTest {

    /**
     * 选课面板应能按课程编号或名称忽略大小写筛选。
     */
    @Test
    void filtersCoursesByCodeOrNameIgnoreCase() {
        List<Course> courses = new ArrayList<Course>();
        courses.add(new Course("CS101", "数据结构", 3, "t1", 60));
        courses.add(new Course("MA201", "高等数学", 4, "t2", 80));

        List<Course> byCode = CourseSelectPanel.filterCourses(courses, "cs");
        List<Course> byName = CourseSelectPanel.filterCourses(courses, "数学");

        assertEquals(1, byCode.size());
        assertEquals("CS101", byCode.get(0).getCode());
        assertEquals(1, byName.size());
        assertEquals("MA201", byName.get(0).getCode());
    }

    /**
     * 空关键词应返回全部课程，null 列表应安全返回空列表。
     */
    @Test
    void filterCoursesHandlesEmptyAndNull() {
        List<Course> courses = new ArrayList<Course>();
        courses.add(new Course("CS101", "数据结构", 3, "t1", 60));

        assertEquals(1, CourseSelectPanel.filterCourses(courses, "  ").size());
        assertEquals(1, CourseSelectPanel.filterCourses(courses, null).size());
        assertEquals(0, CourseSelectPanel.filterCourses(null, "cs").size());
    }

    /**
     * 成绩展示记录应保存成绩主体、课程名称与学分。
     */
    @Test
    void scoreRecordStoresScoreNameAndCredit() {
        Score score = new Score("s1", "CS101", "2026-2027-1");
        ScoreRecord record = new ScoreRecord(score, "数据结构", 3);

        assertEquals(score, record.getScore());
        assertEquals("数据结构", record.getCourseName());
        assertEquals(3, record.getCredit());
    }

    /**
     * 未含课程信息的成绩展示记录应以占位名与零学分构建。
     */
    @Test
    void scoreRecordDefaultsMissingCourseInfo() {
        ScoreRecord record = new ScoreRecord(new Score("s1", "CS101", "2026-2027-1"));

        assertNull(record.getCourseName());
        assertEquals(0, record.getCredit());
    }

    /**
     * 学生视角应展示课程编号、名称、学分、学期、成绩五列。
     */
    @Test
    void studentScorePanelUsesStudentColumns() {
        ScorePanel panel = new ScorePanel();

        assertEquals(5, panel.scoreModel.getColumnCount());
        assertEquals("课程编号", panel.scoreModel.getColumnName(0));
        assertEquals("成绩", panel.scoreModel.getColumnName(4));
    }

    /**
     * 教师/管理员视角应额外展示学号列。
     */
    @Test
    void teacherScorePanelUsesTeacherColumns() {
        ScorePanel panel = new ScorePanel("教师");

        assertEquals(6, panel.scoreModel.getColumnCount());
        assertEquals("学号", panel.scoreModel.getColumnName(0));
        assertEquals("成绩", panel.scoreModel.getColumnName(5));
    }

    /**
     * 渲染成绩后应更新表格行数，并计算学分加权 GPA。
     */
    @Test
    void renderScoresPopulatesRowsAndGpa() {
        ScorePanel panel = new ScorePanel();
        List<ScoreRecord> records = new ArrayList<ScoreRecord>();
        records.add(record("s1", "CS101", 3, 90.0));
        records.add(record("s1", "MA201", 2, 80.0));

        panel.renderScores(records);

        assertEquals(2, panel.getScoreCount());
        assertEquals("GPA：3.60", panel.getGpaText());
        assertTrue(panel.getStatusText().contains("2"));
    }

    /**
     * 空成绩列表应显示零行与默认 GPA。
     */
    @Test
    void renderScoresHandlesEmptyList() {
        ScorePanel panel = new ScorePanel();

        panel.renderScores(new ArrayList<ScoreRecord>());

        assertEquals(0, panel.getScoreCount());
        assertEquals("GPA：0.00", panel.getGpaText());
    }

    /**
     * 无有效分数时 GPA 应为 0。
     */
    @Test
    void weightedGpaReturnsZeroWithoutScores() {
        List<ScoreRecord> records = new ArrayList<ScoreRecord>();
        Score score = new Score("s1", "CS101", "2026-2027-1");
        records.add(new ScoreRecord(score, "数据结构", 3));

        assertEquals(0.0, GpaCalculator.weightedGpa(records), 0.001);
    }

    /**
     * 选课面板命令码应覆盖课程列表、选课与退课。
     */
    @Test
    void courseCommandCodesAreDefined() {
        assertEquals(300, CourseCommand.COURSE_LIST);
        assertEquals(301, CourseCommand.COURSE_SELECT);
        assertEquals(302, CourseCommand.COURSE_DROP);
    }

    /**
     * 成绩面板命令码应覆盖成绩查询与成绩保存。
     */
    @Test
    void scoreCommandCodesAreDefined() {
        assertEquals(303, CourseCommand.SCORE_QUERY);
        assertEquals(304, CourseCommand.SCORE_SAVE);
    }

    private ScoreRecord record(String studentUuid, String courseCode, int credit,
            double value) {
        Score score = new Score(studentUuid, courseCode, "2026-2027-1");
        score.setScore(Double.valueOf(value));
        return new ScoreRecord(score, courseCode, credit);
    }
}

