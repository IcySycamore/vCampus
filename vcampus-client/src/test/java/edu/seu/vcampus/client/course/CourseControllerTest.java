package edu.seu.vcampus.client.course;

import edu.seu.vcampus.common.course.Course;
import edu.seu.vcampus.common.course.Timeslot;

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

    /**
     * 选中一门「没选过」的课程再点退课，应在本地就拦住，不发请求。
     */
    @Test
    void dropUnselectedCourseIsRejectedLocally() {
        CourseSelectPanel panel = new CourseSelectPanel();
        panel.renderCourses(courses());
        panel.courseTable.setRowSelectionInterval(0, 0);

        panel.controller.dropSelected();

        assertTrue(panel.getStatusText().contains("并未选修"), panel.getStatusText());
    }

    /**
     * 选中一门「已选过」的课程再点选课，应提示无需重复选课。
     */
    @Test
    void selectingAlreadySelectedCourseIsRejectedLocally() {
        CourseSelectPanel panel = new CourseSelectPanel();
        List<Course> list = courses();
        panel.renderCourses(list);
        panel.renderSelections(list.subList(0, 1));
        panel.courseTable.setRowSelectionInterval(0, 0);

        panel.controller.selectSelected();

        assertTrue(panel.getStatusText().contains("已经选过"), panel.getStatusText());
    }

    /**
     * 状态列要能区分「已选」与「与已选冲突」。
     */
    @Test
    void statusColumnMarksSelectionAndConflict() {
        CourseSelectPanel panel = new CourseSelectPanel();
        List<Course> list = new ArrayList<Course>();
        Course mine = new Course("CS101", "数据结构", 3, null, 40);
        mine.setTimeslot(new Timeslot(1, 8 * 60, 10 * 60));
        Course overlapping = new Course("CS102", "计算机网络", 2, null, 40);
        overlapping.setTimeslot(new Timeslot(1, 9 * 60, 11 * 60));
        list.add(mine);
        list.add(overlapping);
        panel.renderCourses(list);

        assertEquals("可选", panel.courseModel.getValueAt(0, 7));
        assertEquals("可选", panel.courseModel.getValueAt(1, 7));

        panel.renderSelections(list.subList(0, 1));

        assertEquals("已选", panel.courseModel.getValueAt(0, 7));
        assertEquals("与已选冲突", panel.courseModel.getValueAt(1, 7));
    }

    /**
     * 表格里不应再出现裸 uuid：教师列显示姓名。
     */
    @Test
    void teacherColumnDoesNotPrintRawUuid() {
        CourseSelectPanel panel = new CourseSelectPanel();
        List<Course> list = new ArrayList<Course>();
        Course course = new Course("CS101", "数据结构", 3, "t1", 40);
        course.setTeacherName("演示教师");
        list.add(course);
        panel.renderCourses(list);

        assertEquals("演示教师", panel.courseModel.getValueAt(0, 3));
    }
}
