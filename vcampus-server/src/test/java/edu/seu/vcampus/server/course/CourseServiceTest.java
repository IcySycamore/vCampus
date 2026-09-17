package edu.seu.vcampus.server.course;

import edu.seu.vcampus.common.course.Classroom;
import edu.seu.vcampus.common.course.College;
import edu.seu.vcampus.common.course.CourseSection;
import edu.seu.vcampus.common.course.Field;
import edu.seu.vcampus.common.course.Student;
import edu.seu.vcampus.common.course.Timeslot;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 选课服务测试：学生选课、退课与成绩录入。
 */
class CourseServiceTest extends CourseDbTestBase {

    private CourseDao courseDao;
    private ScoreDao scoreDao;
    private CourseService service;
    private CourseManagementService management;

    private College college;
    private Student student;

    /** 同一用例内课程编号的递增值：库上编号唯一，开两门课不能同编号。 */
    private int codeSeq;

    @BeforeEach
    void setUp() {
        courseDao = dbCourse;
        scoreDao = dbScore;
        service = new CourseService(courseDao, scoreDao);
        management = new CourseManagementService(courseDao);

        college = new College();
        college.setName("计算机学院");
        college.getMajors().add(new Field("软件工程"));
        courseDao.saveCollege(college);

        student = new Student();
        student.setUuid(account("stu"));
        student.setCollegeUuid(college.getUuid());
        student.setMajor(new Field("软件工程"));
        student.getAvailableTimeslots().add(slot(1, 8, 12));
        courseDao.saveStudent(student);
    }

    private static Timeslot slot(int weekday, int startHour, int endHour) {
        return new Timeslot(weekday, startHour * 60, endHour * 60);
    }

    private CourseSection newCourse(int capacity) {
        CourseSection course = new CourseSection();
        course.setCode("IT" + (++codeSeq));
        course.setName("数据结构");
        course.setCollegeUuid(college.getUuid());
        course.setCapacity(capacity);
        course.setSemester("2026-2027-1");
        course.getEligibleMajors().add(new Field("软件工程"));
        return course;
    }

    private List<Timeslot> timeslots(int startHour, int endHour) {
        List<Timeslot> result = new ArrayList<Timeslot>();
        result.add(slot(1, startHour, endHour));
        return result;
    }

    private CourseSection scheduledCourse(int capacity, int startHour, int endHour) {
        CourseSection course = newCourse(capacity);
        management.openCourse(course);
        Classroom classroom = new Classroom();
        classroom.setCollegeUuid(college.getUuid());
        classroom.setCapacity(capacity + 20);
        classroom.setLocation("教一");
        classroom.getAvailableTimeslots().add(slot(1, 8, 12));
        courseDao.saveClassroom(classroom);
        management.scheduleCourse(course.getUuid(), classroom.getUuid(),
                timeslots(startHour, endHour));
        return course;
    }

    @Test
    void selectCourseAndRecordScore() {
        CourseSection course = scheduledCourse(30, 8, 10);
        assertNull(service.selectCourse(student.getUuid(), course.getUuid()));
        assertEquals(1, course.getEnrolledCount());

        assertNull(service.recordScore(course.getUuid(), student.getUuid(), 90.0));
        assertEquals(Double.valueOf(90.0), scoreDao.find(student.getUuid(), course.getCode()).getScore());
    }

    @Test
    void selectRejectsFullCourse() {
        CourseSection course = scheduledCourse(1, 8, 10);
        Student other = new Student();
        other.setUuid(account("other"));
        other.setCollegeUuid(college.getUuid());
        other.setMajor(new Field("软件工程"));
        courseDao.saveStudent(other);

        assertNull(service.selectCourse(student.getUuid(), course.getUuid()));
        assertNotNull(service.selectCourse(other.getUuid(), course.getUuid()));
        assertEquals(1, course.getEnrolledCount());
    }

    @Test
    void selectRejectsTimeConflict() {
        CourseSection first = scheduledCourse(30, 8, 10);
        service.selectCourse(student.getUuid(), first.getUuid());

        CourseSection second = scheduledCourse(30, 8, 10);
        assertNotNull(service.selectCourse(student.getUuid(), second.getUuid()));
    }

    @Test
    void selectRejectsOutsideStudentAvailable() {
        Student limited = new Student();
        limited.setUuid(account("limited"));
        limited.setCollegeUuid(college.getUuid());
        limited.setMajor(new Field("软件工程"));
        limited.getAvailableTimeslots().add(slot(1, 8, 10));
        courseDao.saveStudent(limited);

        CourseSection course = scheduledCourse(30, 10, 12);
        assertNotNull(service.selectCourse(limited.getUuid(), course.getUuid()));
        assertEquals(0, course.getEnrolledCount());
    }

    @Test
    void selectEligibilityByCollegeOrMajor() {
        College otherCollege = new College();
        otherCollege.setName("其他学院");
        courseDao.saveCollege(otherCollege);

        CourseSection course = newCourse(30);
        management.openCourse(course);
        Student outsider = new Student();
        outsider.setUuid(account("outsider"));
        outsider.setCollegeUuid(otherCollege.getUuid());
        outsider.setMajor(new Field("软件工程"));
        courseDao.saveStudent(outsider);
        assertNull(service.selectCourse(outsider.getUuid(), course.getUuid()));

        CourseSection onlyCollege = newCourse(30);
        onlyCollege.getEligibleMajors().clear();
        management.openCourse(onlyCollege);
        Student outsider2 = new Student();
        outsider2.setUuid(account("outsider2"));
        outsider2.setCollegeUuid(otherCollege.getUuid());
        outsider2.setMajor(new Field("软件工程"));
        courseDao.saveStudent(outsider2);
        assertNotNull(service.selectCourse(outsider2.getUuid(), onlyCollege.getUuid()));
    }

    @Test
    void dropCourseRemovesEnrollment() {
        CourseSection course = scheduledCourse(30, 8, 10);
        service.selectCourse(student.getUuid(), course.getUuid());
        assertEquals(1, course.getEnrolledCount());

        assertNull(service.dropCourse(student.getUuid(), course.getUuid()));
        assertEquals(0, course.getEnrolledCount());
        assertFalse(student.getSelectedCourseUuids().contains(course.getUuid()));
    }

    @Test
    void recordScoreRequiresEnrollmentAndRange() {
        CourseSection course = scheduledCourse(30, 8, 10);
        assertNotNull(service.recordScore(course.getUuid(), student.getUuid(), 90.0));

        service.selectCourse(student.getUuid(), course.getUuid());
        assertNotNull(service.recordScore(course.getUuid(), student.getUuid(), 120.0));
        assertNotNull(service.recordScore(course.getUuid(), student.getUuid(), -5.0));
        assertNull(scoreDao.find(student.getUuid(), course.getCode()));
    }
}
