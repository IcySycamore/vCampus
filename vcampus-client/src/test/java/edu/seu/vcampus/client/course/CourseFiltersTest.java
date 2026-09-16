package edu.seu.vcampus.client.course;

import edu.seu.vcampus.common.course.Course;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 课程关键词筛选工具测试。
 */
public class CourseFiltersTest {

    /**
     * 应按课程编号忽略大小写匹配。
     */
    @Test
    void filtersByCodeIgnoringCase() {
        List<Course> courses = new ArrayList<Course>();
        courses.add(new Course("CS101", "数据结构", 3, "t1", 60));
        courses.add(new Course("MA201", "高等数学", 4, "t2", 80));

        List<Course> result = CourseFilters.filter(courses, "cs");

        assertEquals(1, result.size());
        assertEquals("CS101", result.get(0).getCode());
    }

    /**
     * 应按课程名称忽略大小写匹配。
     */
    @Test
    void filtersByNameIgnoringCase() {
        List<Course> courses = new ArrayList<Course>();
        courses.add(new Course("CS101", "数据结构", 3, "t1", 60));
        courses.add(new Course("MA201", "高等数学", 4, "t2", 80));

        assertEquals(1, CourseFilters.filter(courses, "数学").size());
    }

    /**
     * 空白或 null 关键词应返回全部课程。
     */
    @Test
    void blankKeywordReturnsAll() {
        List<Course> courses = new ArrayList<Course>();
        courses.add(new Course("CS101", "数据结构", 3, "t1", 60));

        assertEquals(1, CourseFilters.filter(courses, "  ").size());
        assertEquals(1, CourseFilters.filter(courses, null).size());
    }

    /**
     * null 课程列表应安全返回空列表。
     */
    @Test
    void nullCoursesReturnsEmpty() {
        assertEquals(0, CourseFilters.filter(null, "cs").size());
    }

    /**
     * 无匹配课程时应返回空列表。
     */
    @Test
    void noMatchReturnsEmpty() {
        List<Course> courses = new ArrayList<Course>();
        courses.add(new Course("CS101", "数据结构", 3, "t1", 60));

        assertEquals(0, CourseFilters.filter(courses, "不存在").size());
    }
}

