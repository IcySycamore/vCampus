package edu.seu.vcampus.server.course;

import edu.seu.vcampus.common.course.Classroom;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 课程管理服务测试：学院开设课程、教师认领、手动排课与教室推荐。
 */
class CourseManagementServiceTest extends CourseDbTestBase {

    private CourseDao courseDao;
    private CourseManagementService service;
    private College college;
    private Teacher teacher;
    private Classroom classroom;

    @BeforeEach
    void setUp() {
        courseDao = dbCourse;
        service = new CourseManagementService(courseDao);

        college = new College();
        college.setName("计算机学院");
        college.getResearchDirections().add(new Field("人工智能"));
        courseDao.saveCollege(college);

        teacher = new Teacher();
        teacher.setCollegeUuid(college.getUuid());
        teacher.getResearchDirections().add(new Field("人工智能"));
        teacher.getAvailableTimeslots().add(slot(1, 8, 12));
        courseDao.saveTeacher(teacher);

        classroom = new Classroom();
        classroom.setCollegeUuid(college.getUuid());
        classroom.setCapacity(50);
        classroom.setLocation("教一");
        classroom.getAvailableTimeslots().add(slot(1, 8, 12));
        courseDao.saveClassroom(classroom);
    }

    private static Timeslot slot(int weekday, int startHour, int endHour) {
        return new Timeslot(weekday, startHour * 60, endHour * 60);
    }

    private CourseSection newCourse(int capacity) {
        CourseSection course = new CourseSection();
        course.setCode("CS101");
        course.setName("数据结构");
        course.setCollegeUuid(college.getUuid());
        course.setCapacity(capacity);
        course.setPreferredLocation("教一");
        course.setSemester("2026-2027-1");
        course.getRequiredDirections().add(new Field("人工智能"));
        return course;
    }

    private List<Timeslot> timeslots(int startHour, int endHour) {
        List<Timeslot> result = new ArrayList<Timeslot>();
        result.add(slot(1, startHour, endHour));
        return result;
    }

    @Test
    void openClaimAndSchedule() {
        CourseSection course = newCourse(30);
        assertNull(service.openCourse(course));
        assertNotNull(course.getUuid());

        assertNull(service.claimCourse(teacher.getUuid(), course.getUuid()));
        assertEquals(teacher.getUuid(), course.getTeacherUuid());

        assertNull(service.scheduleCourse(course.getUuid(), classroom.getUuid(), timeslots(8, 10)));
        assertEquals(classroom.getUuid(), course.getClassroomUuid());
    }

    @Test
    void claimRequiresResearchDirection() {
        Teacher other = new Teacher();
        other.setCollegeUuid(college.getUuid());
        other.getResearchDirections().add(new Field("网络"));
        courseDao.saveTeacher(other);

        CourseSection course = newCourse(30);
        service.openCourse(course);
        assertNotNull(service.claimCourse(other.getUuid(), course.getUuid()));
        assertNull(course.getTeacherUuid());
    }

    @Test
    void scheduleRejectsInsufficientCapacity() {
        CourseSection course = newCourse(60);
        service.openCourse(course);
        assertNotNull(service.scheduleCourse(course.getUuid(), classroom.getUuid(),
                timeslots(8, 10)));
        assertNull(course.getClassroomUuid());
    }

    @Test
    void scheduleRejectsOutsideAvailableTime() {
        CourseSection course = newCourse(30);
        service.openCourse(course);
        assertNotNull(service.scheduleCourse(course.getUuid(), classroom.getUuid(),
                timeslots(20, 22)));
        assertNull(course.getClassroomUuid());
    }

    @Test
    void suggestClassroomPrefersLocation() {
        CourseSection course = newCourse(30);
        course.setPreferredLocation("教二");
        service.openCourse(course);

        Classroom preferred = new Classroom();
        preferred.setCollegeUuid(college.getUuid());
        preferred.setCapacity(40);
        preferred.setLocation("教二");
        preferred.getAvailableTimeslots().add(slot(1, 8, 12));
        courseDao.saveClassroom(preferred);

        assertEquals(preferred.getUuid(),
                service.suggestClassroom(course.getUuid(), timeslots(8, 10)));
    }

    @Test
    void collegeTeacherBidirectionalIndex() {
        assertTrue(college.getTeacherUuids().contains(teacher.getUuid()));
        courseDao.deleteTeacher(teacher.getUuid());
        assertFalse(college.getTeacherUuids().contains(teacher.getUuid()));
    }
}
