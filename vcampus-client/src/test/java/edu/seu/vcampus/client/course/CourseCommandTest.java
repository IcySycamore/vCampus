package edu.seu.vcampus.client.course;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 选课命令码常量测试。
 */
public class CourseCommandTest {

    /**
     * 五个命令码应分别落在 300-304 号段。
     */
    @Test
    void definesCourseCommandCodes() {
        assertEquals(300, CourseCommand.COURSE_LIST);
        assertEquals(301, CourseCommand.COURSE_SELECT);
        assertEquals(302, CourseCommand.COURSE_DROP);
        assertEquals(303, CourseCommand.SCORE_QUERY);
        assertEquals(304, CourseCommand.SCORE_SAVE);
    }
}

