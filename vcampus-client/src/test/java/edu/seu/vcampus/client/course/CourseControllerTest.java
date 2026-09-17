package edu.seu.vcampus.client.course;

import edu.seu.vcampus.common.course.Course;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 选课控制器测试。
 */
public class CourseControllerTest {

    /**
     * 应用筛选后应按记录条数渲染课程表格。
     */
    @Test
    void applyFilterRendersCourses() {
        CourseSelectPanel panel = new CourseSelectPanel();
        panel.renderCourses(courses());

        assertEquals(2, panel.getCourseCount());
    }

    /**
     * 未选中课程时发起选课应提示先选择。
     */
    @Test
    void selectWithoutSelectionPrompts() {
        CourseSelectPanel panel = new CourseSelectPanel();

        panel.controller.selectSelected();

        assertTrue(panel.getStatusText().contains("请先选择"));
    }

    /**
     * 未选中课程时发起退课应提示先选择。
     */
    @Test
    void dropWithoutSelectionPrompts() {
        CourseSelectPanel panel = new CourseSelectPanel();

        panel.controller.dropSelected();

        assertTrue(panel.getStatusText().contains("请先选择"));
    }

    private List<Course> courses() {
        List<Course> courses = new ArrayList<Course>();
        courses.add(new Course("CS101", "数据结构", 3, "t1", 60));
        courses.add(new Course("MA201", "高等数学", 4, "t2", 80));
        return courses;
    }
}

