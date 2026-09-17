package edu.seu.vcampus.server.course;

import edu.seu.vcampus.common.course.College;
import edu.seu.vcampus.common.course.CourseSection;
import edu.seu.vcampus.common.course.Teacher;
import edu.seu.vcampus.common.course.dto.CourseSaveRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 课程管理增删改规则测试：容量只增不减、编号/uuid 不可改、名称与教师可改、删除保护。
 */
class CourseManagementCrudTest {

    private CourseDao dao;
    private CourseManagementService service;
    private Teacher teacher;

    @BeforeEach
    void setUp() {
        dao = new CourseDao();
        service = new CourseManagementService(dao);

        College college = new College();
        college.setName("计算机学院");
        dao.saveCollege(college);

        teacher = new Teacher();
        teacher.setCollegeUuid(college.getUuid());
        dao.saveTeacher(teacher);
    }

    private CourseSaveRequest addRequest(String code, String name, int credit, int capacity) {
        CourseSaveRequest request = new CourseSaveRequest();
        request.setCode(code);
        request.setName(name);
        request.setCredit(Integer.valueOf(credit));
        request.setCapacity(Integer.valueOf(capacity));
        return request;
    }

    @Test
    void addCourseSucceedsAndAssignsUuid() {
        assertNull(service.addCourse(addRequest("CS101", "数据结构", 3, 40)));
        CourseSection course = dao.findCourseByCode("CS101");
        assertNotNull(course);
        assertNotNull(course.getUuid());
        assertEquals("数据结构", course.getName());
    }

    @Test
    void addRejectsDuplicateCode() {
        assertNull(service.addCourse(addRequest("CS101", "数据结构", 3, 40)));
        assertNotNull(service.addCourse(addRequest("CS101", "高等数学", 4, 50)));
    }

    @Test
    void addRejectsBlankCodeOrName() {
        assertNotNull(service.addCourse(addRequest(" ", "数据结构", 3, 40)));
        assertNotNull(service.addCourse(addRequest("CS102", "  ", 3, 40)));
    }

    @Test
    void addRejectsInvalidCreditOrCapacity() {
        assertNotNull(service.addCourse(addRequest("CS103", "操作系统", 0, 40)));
        assertNotNull(service.addCourse(addRequest("CS103", "操作系统", 3, 0)));
    }

    @Test
    void updateChangesNameOnly() {
        service.addCourse(addRequest("CS101", "数据结构", 3, 40));
        CourseSaveRequest update = new CourseSaveRequest();
        update.setCode("CS101");
        update.setName("数据结构（进阶）");
        assertNull(service.updateCourse(update));
        CourseSection course = dao.findCourseByCode("CS101");
        assertEquals("数据结构（进阶）", course.getName());
        assertEquals(40, course.getCapacity());
    }

    @Test
    void updateCapacityCanOnlyIncrease() {
        service.addCourse(addRequest("CS101", "数据结构", 3, 40));
        CourseSaveRequest increase = new CourseSaveRequest();
        increase.setCode("CS101");
        increase.setCapacity(Integer.valueOf(60));
        assertNull(service.updateCourse(increase));
        assertEquals(60, dao.findCourseByCode("CS101").getCapacity());

        CourseSaveRequest decrease = new CourseSaveRequest();
        decrease.setCode("CS101");
        decrease.setCapacity(Integer.valueOf(30));
        assertNotNull(service.updateCourse(decrease));
        assertEquals(60, dao.findCourseByCode("CS101").getCapacity());
    }

    @Test
    void updateCapacityCannotDropBelowEnrolled() {
        service.addCourse(addRequest("CS101", "数据结构", 3, 40));
        dao.findCourseByCode("CS101").getStudentUuids().add("s1");
        CourseSaveRequest update = new CourseSaveRequest();
        update.setCode("CS101");
        update.setCapacity(Integer.valueOf(1));
        assertNotNull(service.updateCourse(update));
    }

    @Test
    void updateKeepsCodeAndUuidImmutable() {
        service.addCourse(addRequest("CS101", "数据结构", 3, 40));
        CourseSection before = dao.findCourseByCode("CS101");
        String uuid = before.getUuid();

        CourseSaveRequest update = new CourseSaveRequest();
        update.setCode("CS101");
        update.setName("新名字");
        service.updateCourse(update);

        CourseSection after = dao.findCourseByCode("CS101");
        assertEquals("CS101", after.getCode());
        assertEquals(uuid, after.getUuid());
    }

    @Test
    void updateChangesTeacher() {
        service.addCourse(addRequest("CS101", "数据结构", 3, 40));
        CourseSaveRequest update = new CourseSaveRequest();
        update.setCode("CS101");
        update.setTeacherUuid(teacher.getUuid());
        assertNull(service.updateCourse(update));
        assertEquals(teacher.getUuid(), dao.findCourseByCode("CS101").getTeacherUuid());
    }

    @Test
    void updateRejectsUnknownTeacher() {
        service.addCourse(addRequest("CS101", "数据结构", 3, 40));
        CourseSaveRequest update = new CourseSaveRequest();
        update.setCode("CS101");
        update.setTeacherUuid("no-such-teacher");
        assertNotNull(service.updateCourse(update));
        assertNull(dao.findCourseByCode("CS101").getTeacherUuid());
    }

    @Test
    void deleteRemovesUnselectedCourse() {
        service.addCourse(addRequest("CS101", "数据结构", 3, 40));
        assertNull(service.deleteCourse("CS101"));
        assertNull(dao.findCourseByCode("CS101"));
    }

    @Test
    void deleteRejectsCourseWithStudents() {
        service.addCourse(addRequest("CS101", "数据结构", 3, 40));
        dao.findCourseByCode("CS101").getStudentUuids().add("s1");
        assertNotNull(service.deleteCourse("CS101"));
        assertNotNull(dao.findCourseByCode("CS101"));
    }

    @Test
    void deleteRejectsUnknownCourse() {
        assertNotNull(service.deleteCourse("CS999"));
    }
}
