package edu.seu.vcampus.client.course;

import edu.seu.vcampus.common.course.Course;
import edu.seu.vcampus.common.course.Timeslot;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 选课界面展示文案测试。
 *
 * <p>
 * 这些文案原先在各表格里就地拼字符串，结果是教师/教室位置直接显示 uuid 前 36 位。把它们收进 {@link CourseDisplay} 后可以用单测把「界面里不再出现裸
 * uuid」钉住。
 */
public class CourseDisplayTest {

    /**
     * 教师列优先显示姓名，未认领显示「未认领」，姓名缺失时退回短 uuid。
     */
    @Test
    void teacherPrefersNameAndFallsBackToShortUuid() {
        Course named = new Course("CS101", "数据结构", 3, "00000000-0000-0000-0000-0000000000a3", 40);
        named.setTeacherName("演示教师");
        assertEquals("演示教师", CourseDisplay.teacherOf(named));

        Course unnamed = new Course("CS102", "计算机网络", 2,
                "00000000-0000-0000-0000-0000000000a3", 40);
        assertTrue(CourseDisplay.teacherOf(unnamed).startsWith("（未命名教师）"));
        assertTrue(CourseDisplay.teacherOf(unnamed).indexOf("00000000") > 0);

        assertEquals("未认领", CourseDisplay.teacherOf(
                new Course("CS103", "操作系统", 3, null, 30)));
    }

    /**
     * 上课时间应给出星期与节次，未排课直接说明。
     */
    @Test
    void timeShowsWeekdayAndPeriods() {
        Course course = new Course("CS103", "操作系统", 3, null, 30);
        assertEquals("未排课", CourseDisplay.timeOf(course));

        course.setTimeslot(new Timeslot(1, 8 * 60, 10 * 60));
        String text = CourseDisplay.timeOf(course);
        assertTrue(text.contains("周一"), text);
        assertTrue(text.indexOf("未排课") < 0, text);
    }

    /**
     * 余量是「容量 − 已选」，选满直接说明。
     */
    @Test
    void remainingIsCapacityMinusEnrolled() {
        Course course = new Course("CS101", "数据结构", 3, null, 40);
        course.setEnrolled(12);
        assertEquals("28", CourseDisplay.remainingOf(course));

        course.setEnrolled(40);
        assertEquals("已满", CourseDisplay.remainingOf(course));
    }

    /**
     * 状态列要区分「已选」「已满」「与已选冲突」。
     */
    @Test
    void statusCombinesSelectionAndConflict() {
        Course course = new Course("CS101", "数据结构", 3, null, 40);
        assertEquals("可选", CourseDisplay.statusOf(course, false, false));
        assertEquals("已选", CourseDisplay.statusOf(course, true, false));
        assertEquals("已选（时间冲突）", CourseDisplay.statusOf(course, true, true));
        assertEquals("与已选冲突", CourseDisplay.statusOf(course, false, true));

        course.setEnrolled(40);
        assertEquals("已满", CourseDisplay.statusOf(course, false, false));
    }

    /**
     * 教室名优先查名称表，查不到才退回短 uuid——不能整串 uuid 打在界面上。
     */
    @Test
    void classroomUsesNameLookup() {
        Course course = new Course("CS101", "数据结构", 3, null, 40);
        assertEquals("未安排", CourseDisplay.classroomOf(course, null));

        course.setClassroomUuid("00000000-0000-0000-0000-0000000000a3");
        Map<String, String> rooms = new HashMap<String, String>();
        rooms.put(course.getClassroomUuid(), "教二201");
        assertEquals("教二201", CourseDisplay.classroomOf(course, rooms));

        String unknown = CourseDisplay.classroomOf(course, new HashMap<String, String>());
        assertTrue(unknown.startsWith("（未知教室）"), unknown);
        assertTrue(unknown.length() < 20, unknown);
    }
}
