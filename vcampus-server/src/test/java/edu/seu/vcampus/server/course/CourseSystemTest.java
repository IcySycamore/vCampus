package edu.seu.vcampus.server.course;

import edu.seu.vcampus.common.course.Classroom;
import edu.seu.vcampus.common.course.College;
import edu.seu.vcampus.common.course.CourseSection;
import edu.seu.vcampus.common.course.Field;
import edu.seu.vcampus.common.course.Student;
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
 * 选课系统整体流程测试：开课、认领、排课、选课、退课、成绩录入全链路。
 */
class CourseSystemTest {

    private CourseDao courseDao;
    private ScoreDao scoreDao;
    private CourseManagementService management;
    private CourseService service;

    private College college;
    private Classroom classroomA;
    private Classroom classroomB;
    private Teacher teacherAi;
    private Teacher teacherNet;
    private Student s1;
    private Student s2;
    private Student s3;
    private CourseSection cs101;
    private CourseSection cs102;

    @BeforeEach
    void setUp() {
        courseDao = new CourseDao();
        scoreDao = new ScoreDao();
        management = new CourseManagementService(courseDao);
        service = new CourseService(courseDao, scoreDao);

        college = new College();
        college.setName("计算机学院");
        college.getResearchDirections().add(new Field("人工智能"));
        college.getResearchDirections().add(new Field("网络"));
        college.getMajors().add(new Field("软件工程"));
        courseDao.saveCollege(college);

        classroomA = new Classroom();
        classroomA.setCollegeUuid(college.getUuid());
        classroomA.setCapacity(50);
        classroomA.setLocation("教一");
        classroomA.getAvailableTimeslots().add(slot(1, 8, 12));
        courseDao.saveClassroom(classroomA);

        classroomB = new Classroom();
        classroomB.setCollegeUuid(college.getUuid());
        classroomB.setCapacity(50);
        classroomB.setLocation("教二");
        classroomB.getAvailableTimeslots().add(slot(1, 8, 12));
        courseDao.saveClassroom(classroomB);

        teacherAi = newTeacher("人工智能");
        teacherNet = newTeacher("网络");

        s1 = newStudent("软件工程");
        s2 = newStudent("软件工程");
        s3 = newStudent("软件工程");

        cs101 = newCourse("CS101", "数据结构", 2, "人工智能");
        cs102 = newCourse("CS102", "计算机网络", 30, "网络");
    }

    private Teacher newTeacher(String direction) {
        Teacher teacher = new Teacher();
        teacher.setCollegeUuid(college.getUuid());
        teacher.getResearchDirections().add(new Field(direction));
        teacher.getAvailableTimeslots().add(slot(1, 8, 12));
        courseDao.saveTeacher(teacher);
        return teacher;
    }

    private Student newStudent(String major) {
        Student student = new Student();
        student.setCollegeUuid(college.getUuid());
        student.setMajor(new Field(major));
        student.getAvailableTimeslots().add(slot(1, 8, 12));
        courseDao.saveStudent(student);
        return student;
    }

    private CourseSection newCourse(String code, String name, int capacity, String direction) {
        CourseSection course = new CourseSection();
        course.setCode(code);
        course.setName(name);
        course.setCollegeUuid(college.getUuid());
        course.setCapacity(capacity);
        course.setPreferredLocation("教一");
        course.setSemester("2026-2027-1");
        course.getRequiredDirections().add(new Field(direction));
        course.getEligibleMajors().add(new Field("软件工程"));
        return course;
    }

    private static Timeslot slot(int weekday, int startHour, int endHour) {
        return new Timeslot(weekday, startHour * 60, endHour * 60);
    }

    private List<Timeslot> timeslots(int startHour, int endHour) {
        List<Timeslot> result = new ArrayList<Timeslot>();
        result.add(slot(1, startHour, endHour));
        return result;
    }

    @Test
    void fullFlowFromOpenToScore() {
        // 学院开课
        assertNull(management.openCourse(cs101));
        assertNull(management.openCourse(cs102));
        assertNotNull(cs101.getUuid());

        // 教师认领：方向匹配成功、方向不符失败
        assertNull(management.claimCourse(teacherAi.getUuid(), cs101.getUuid()));
        assertNotNull(management.claimCourse(teacherNet.getUuid(), cs101.getUuid()));
        assertEquals(teacherAi.getUuid(), cs101.getTeacherUuid());

        // 手动排课：容量与时间可用性校验
        assertNull(management.scheduleCourse(cs101.getUuid(), classroomA.getUuid(),
                timeslots(8, 10)));
        assertEquals(classroomA.getUuid(), cs101.getClassroomUuid());

        // 学生选课：容量 2，满员后拒绝
        assertNull(service.selectCourse(s1.getUuid(), cs101.getUuid()));
        assertNull(service.selectCourse(s2.getUuid(), cs101.getUuid()));
        assertNotNull(service.selectCourse(s3.getUuid(), cs101.getUuid()));
        assertEquals(2, cs101.getEnrolledCount());

        // 退课腾出名额后 s3 可再选
        assertNull(service.dropCourse(s2.getUuid(), cs101.getUuid()));
        assertEquals(1, cs101.getEnrolledCount());
        assertNull(service.selectCourse(s3.getUuid(), cs101.getUuid()));
        assertEquals(2, cs101.getEnrolledCount());

        // 成绩录入：已选修成功、未选修失败
        assertNull(service.recordScore(cs101.getUuid(), s1.getUuid(), 90.0));
        assertEquals(1, service.listScoresByCourse("CS101").size());
        Student outsider = newStudent("软件工程");
        assertNotNull(service.recordScore(cs101.getUuid(), outsider.getUuid(), 80.0));
    }

    @Test
    void timeConflictAndBidirectionalIndex() {
        assertNull(management.openCourse(cs101));
        assertNull(management.openCourse(cs102));
        assertNull(management.claimCourse(teacherAi.getUuid(), cs101.getUuid()));
        assertNull(management.claimCourse(teacherNet.getUuid(), cs102.getUuid()));
        // 两门课都安排在周一 8-10，但教室不同（避免教室冲突，仅保留时间重叠以测学生选课冲突）
        assertNull(management.scheduleCourse(cs101.getUuid(), classroomA.getUuid(),
                timeslots(8, 10)));
        assertNull(management.scheduleCourse(cs102.getUuid(), classroomB.getUuid(),
                timeslots(8, 10)));

        assertNull(service.selectCourse(s1.getUuid(), cs101.getUuid()));
        // 同一时间选第二门课 → 时间冲突
        assertNotNull(service.selectCourse(s1.getUuid(), cs102.getUuid()));

        // 学院 ↔ 教师双向索引
        assertTrue(college.getTeacherUuids().contains(teacherAi.getUuid()));
        courseDao.deleteTeacher(teacherAi.getUuid());
        assertFalse(college.getTeacherUuids().contains(teacherAi.getUuid()));
    }

    @Test
    void scheduleRejectsClassroomConflict() {
        assertNull(management.openCourse(cs101));
        assertNull(management.openCourse(cs102));
        assertNull(management.claimCourse(teacherAi.getUuid(), cs101.getUuid()));
        assertNull(management.claimCourse(teacherNet.getUuid(), cs102.getUuid()));
        assertNull(management.scheduleCourse(cs101.getUuid(), classroomA.getUuid(),
                timeslots(8, 10)));
        // 同一教室同一时间排第二门课 → 教室被占用
        assertNotNull(management.scheduleCourse(cs102.getUuid(), classroomA.getUuid(),
                timeslots(8, 10)));
    }
}
