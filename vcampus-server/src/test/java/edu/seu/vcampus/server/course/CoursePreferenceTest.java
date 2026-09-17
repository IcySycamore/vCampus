package edu.seu.vcampus.server.course;

import edu.seu.vcampus.common.course.College;
import edu.seu.vcampus.common.course.CourseSection;
import edu.seu.vcampus.common.course.Field;
import edu.seu.vcampus.common.course.Teacher;
import edu.seu.vcampus.common.course.Timeslot;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 教师偏好时间槽与授课课程列表的单元测试。
 */
class CoursePreferenceTest {

    private CourseDao dao;
    private CourseManagementService management;
    private College college;
    private Teacher teacher;

    @BeforeEach
    void setUp() {
        dao = new CourseDao();
        management = new CourseManagementService(dao);

        college = new College();
        college.setName("计算机学院");
        college.getResearchDirections().add(new Field("人工智能"));
        college.getMajors().add(new Field("软件工程"));
        dao.saveCollege(college);

        teacher = new Teacher();
        teacher.setCollegeUuid(college.getUuid());
        teacher.getResearchDirections().add(new Field("人工智能"));
        dao.saveTeacher(teacher);
    }

    private Timeslot slot(int weekday, int startHour, int endHour) {
        return new Timeslot(weekday, startHour * 60, endHour * 60);
    }

    private List<Timeslot> slots(Timeslot... items) {
        List<Timeslot> result = new ArrayList<Timeslot>();
        for (Timeslot item : items) {
            result.add(item);
        }
        return result;
    }

    @Test
    void setTeacherPreferenceOverwrites() {
        List<Timeslot> first = slots(slot(1, 8, 10));
        List<Timeslot> second = slots(slot(2, 14, 16), slot(3, 9, 11));

        assertNull(management.setTeacherPreferenceTimeslots(teacher.getUuid(), first));
        assertEquals(1, teacher.getPreferenceTimeslots().size());

        assertNull(management.setTeacherPreferenceTimeslots(teacher.getUuid(), second));
        assertEquals(2, teacher.getPreferenceTimeslots().size());
    }

    @Test
    void setTeacherPreferenceRejectsUnknownTeacher() {
        assertNotNull(management.setTeacherPreferenceTimeslots("missing", null));
    }

    @Test
    void listCoursesByTeacherReturnsClaimedOnly() {
        CourseSection course = new CourseSection();
        course.setCode("CS101");
        course.setName("数据结构");
        course.setCollegeUuid(college.getUuid());
        course.setCapacity(40);
        course.setSemester("2026-2027-1");
        management.openCourse(course);
        management.claimCourse(teacher.getUuid(), course.getUuid());

        assertEquals(1, management.listCoursesByTeacher(teacher.getUuid()).size());
        assertEquals(0, management.listCoursesByTeacher("other").size());
    }
}
