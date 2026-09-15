package edu.seu.vcampus.common.course;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 选课系统公共实体测试：课程与成绩值对象。
 */
class CourseAndScoreTest {

    /**
     * 课程构造器应正确保存各字段，初始已选人数为 0。
     */
    @Test
    void courseConstructorStoresFieldsAndZeroEnrollment() {
        Course course = new Course("CS101", "数据结构", 3, "uuid-t1", 60);

        assertEquals("CS101", course.getCode());
        assertEquals("数据结构", course.getName());
        assertEquals(3, course.getCredit());
        assertEquals("uuid-t1", course.getTeacherUuid());
        assertEquals(60, course.getCapacity());
        assertEquals(0, course.getEnrolled());
    }

    /**
     * 空课程对象应可通过 setter 设置主键与选课人数。
     */
    @Test
    void emptyCourseAllowsSettingIdAndEnrollment() {
        Course course = new Course();
        assertNull(course.getId());

        course.setId(7L);
        course.setEnrolled(12);

        assertEquals(Long.valueOf(7L), course.getId());
        assertEquals(12, course.getEnrolled());
    }

    /**
     * 选课记录构造器应保存学生、课程与学期，且成绩尚未录入。
     */
    @Test
    void scoreConstructorStoresReferencesAndNullScore() {
        Score score = new Score("uuid-s1", "CS101", "2026-2027-1");

        assertEquals("uuid-s1", score.getStudentUuid());
        assertEquals("CS101", score.getCourseCode());
        assertEquals("2026-2027-1", score.getSemester());
        assertNull(score.getScore());
    }

    /**
     * 空成绩对象应可通过 setter 设置主键与分数。
     */
    @Test
    void emptyScoreAllowsSettingIdAndScore() {
        Score score = new Score();
        assertNull(score.getId());
        assertNull(score.getScore());

        score.setId(9L);
        score.setScore(Double.valueOf(88.5));

        assertEquals(Long.valueOf(9L), score.getId());
        assertEquals(Double.valueOf(88.5), score.getScore());
    }
}
