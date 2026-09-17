package edu.seu.vcampus.client.course;

import edu.seu.vcampus.common.course.College;
import edu.seu.vcampus.common.course.Course;
import edu.seu.vcampus.common.course.Teacher;
import edu.seu.vcampus.common.course.dto.CourseSaveRequest;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 课程新增 / 修改表单测试。
 *
 * <p>
 * 重点钉住两件事：不动容量时**不提交容量**（存量课程容量可能低于服务端下限 40，提交就会被拒）， 以及教师/学院用下拉得到的是 uuid 而不是界面上的姓名。
 */
public class CourseCatalogFormTest {

    /**
     * 修改课程但不动容量：请求里不带容量，避免服务端「不能小于 40」把存量课程卡死。
     */
    @Test
    void unchangedCapacityIsNotSubmitted() {
        CourseCatalogForm form = new CourseCatalogForm();
        form.setOptions(teachers(), colleges());
        Course course = new Course("CS103", "操作系统", 3, null, 30);
        form.render(course);

        CourseSaveRequest request = form.request();

        assertNotNull(request);
        assertEquals("CS103", request.getCode());
        assertNull(request.getCapacity());
    }

    /**
     * 新建表单初始为空编号/空名称：属于校验失败，不应该能组出请求。
     */
    @Test
    void createFormRequiresCodeAndName() {
        CourseCatalogForm form = new CourseCatalogForm();
        form.setOptions(teachers(), colleges());
        form.render(null);

        assertNull(form.request());
        assertTrue(form.errorMessage().contains("课程编号"), form.errorMessage());
    }

    /**
     * 选中教师下拉的第一项表示「未认领」，提交空字符串取消认领。
     */
    @Test
    void teacherBoxFirstItemMeansUnclaimed() {
        CourseCatalogForm form = new CourseCatalogForm();
        form.setOptions(teachers(), colleges());
        form.render(new Course("CS101", "数据结构", 3, null, 40));

        CourseSaveRequest request = form.request();

        assertNotNull(request);
        assertEquals("", request.getTeacherUuid());
    }

    /**
     * 必填项为空时给出可读原因，而不是等到服务端回「参数不合法」。
     */
    @Test
    void reportsReadableValidationErrors() {
        CourseCatalogForm form = new CourseCatalogForm();
        form.render(new Course("CS101", "数据结构", 3, null, 40));
        assertNull(form.errorMessage());

        form.render(new Course("CS101", "", 3, null, 40));
        assertTrue(form.errorMessage().contains("课程名称"), form.errorMessage());
    }

    private List<Teacher> teachers() {
        List<Teacher> list = new ArrayList<Teacher>();
        Teacher teacher = new Teacher("00000000-0000-0000-0000-0000000000a3", "c1");
        teacher.setName("演示教师");
        list.add(teacher);
        return list;
    }

    private List<College> colleges() {
        List<College> list = new ArrayList<College>();
        list.add(new College("00000000-0000-0000-0000-000000000c01", "计算机学院"));
        return list;
    }
}
