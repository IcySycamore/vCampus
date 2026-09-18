package edu.seu.vcampus.client.course;

import edu.seu.vcampus.common.course.Course;
import edu.seu.vcampus.common.message.PageResponse;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 课程管理表格的排序与分页纯函数测试。
 */
public class CourseAdminTableTest {

    private static Course course(String code, String name, int credit, int capacity) {
        return new Course(code, name, credit, "t", capacity);
    }

    @Test
    void sortsByCodeAscendingAndDescending() {
        List<Course> courses = new ArrayList<Course>();
        courses.add(course("CS102", "计算机网络", 2, 40));
        courses.add(course("CS101", "数据结构", 3, 40));
        courses.add(course("CS103", "操作系统", 3, 30));

        List<Course> ascending = CourseSorter.sort(courses, CourseSorter.COL_CODE, true);
        assertEquals("CS101", ascending.get(0).getCode());
        assertEquals("CS103", ascending.get(2).getCode());

        List<Course> descending = CourseSorter.sort(courses, CourseSorter.COL_CODE, false);
        assertEquals("CS103", descending.get(0).getCode());
        assertEquals("CS101", descending.get(2).getCode());
    }

    @Test
    void sortsByCapacityNumerically() {
        List<Course> courses = new ArrayList<Course>();
        courses.add(course("CS102", "计算机网络", 2, 40));
        courses.add(course("CS101", "数据结构", 3, 120));
        courses.add(course("CS103", "操作系统", 3, 30));

        List<Course> sorted = CourseSorter.sort(courses, CourseSorter.COL_CAPACITY, true);
        assertEquals(30, sorted.get(0).getCapacity());
        assertEquals(120, sorted.get(2).getCapacity());
    }

    @Test
    void pagesWithinBounds() {
        List<Course> courses = new ArrayList<Course>();
        for (int i = 1; i <= 12; i++) {
            courses.add(course("CS" + i, "课程" + i, 3, 40));
        }

        PageResponse<Course> page = CoursePaging.page(courses, 1, 5);
        assertEquals(5, page.getItems().size());
        assertEquals(12, page.getTotal());
        assertEquals(3, page.getTotalPages());

        PageResponse<Course> last = CoursePaging.page(courses, 3, 5);
        assertEquals(2, last.getItems().size());
    }

    @Test
    void pagesClampOutOfRange() {
        List<Course> courses = new ArrayList<Course>();
        courses.add(course("CS101", "数据结构", 3, 40));

        PageResponse<Course> page = CoursePaging.page(courses, 99, 5);
        assertEquals(1, page.getPageNumber());
        assertEquals(1, page.getItems().size());
    }

    @Test
    void pagesEmptyList() {
        PageResponse<Course> page = CoursePaging.page(new ArrayList<Course>(), 1, 5);
        assertEquals(0, page.getTotal());
        assertEquals(0, page.getItems().size());
    }

    @Test
    void sortHandlesNullListAndNullNames() {
        assertEquals(0, CourseSorter.sort(null, CourseSorter.COL_CODE, true).size());

        List<Course> courses = new ArrayList<Course>();
        courses.add(new Course("CS101", null, 3, "t", 40));
        courses.add(new Course("CS102", "B", 3, "t", 40));
        List<Course> sorted = CourseSorter.sort(courses, CourseSorter.COL_NAME, true);
        assertEquals("CS101", sorted.get(0).getCode());
    }
}
