package edu.seu.vcampus.server.course;

import edu.seu.vcampus.common.course.Student;
import edu.seu.vcampus.common.course.Teacher;
import edu.seu.vcampus.common.user.entity.Role;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * 开户钩子的学院归属规则：档案挂的学院必须来自学院池。
 *
 * <p>
 * 目录用替身，本用例只关心「挑了哪所学院」这条规则；学院是否真的在库里由 {@link CollegePoolBootstrapTest} 与课程模块的真库用例负责。规则之所以要钉死：
 * 挂到不存在的学院会被数据库以 1452 拒掉，而开户钩子失败会连带回滚整个注册。
 */
class CourseProvisionerTest {

    /** 学生档案挂到的学院一定来自学院池。 */
    @Test
    void studentIsPlacedInACollegeFromThePool() {
        CourseDao dao = mock(CourseDao.class);
        CourseProvisioner provisioner = new CourseProvisioner(dao, Arrays.asList("c-1", "c-2"));

        provisioner.provision("student-1", "张三", Role.STUDENT);

        Student saved = lastStudentOf(dao);
        assertEquals("student-1", saved.getUuid());
        assertTrue(Arrays.asList("c-1", "c-2").contains(saved.getCollegeUuid()),
                "学院必须来自池，实际为 " + saved.getCollegeUuid());
    }

    /** 教师档案同样来自学院池。 */
    @Test
    void teacherIsPlacedInACollegeFromThePool() {
        CourseDao dao = mock(CourseDao.class);
        CourseProvisioner provisioner = new CourseProvisioner(dao, Arrays.asList("c-1", "c-2"));

        provisioner.provision("teacher-1", "李四", Role.TEACHER);

        Teacher saved = lastTeacherOf(dao);
        assertEquals("teacher-1", saved.getUuid());
        assertTrue(Arrays.asList("c-1", "c-2").contains(saved.getCollegeUuid()),
                "学院必须来自池，实际为 " + saved.getCollegeUuid());
    }

    /** 同一个账号反复开户都落在同一所学院：规则不依赖任何内存状态，重启后也一样。 */
    @Test
    void assignmentIsStableForTheSameAccount() {
        CourseDao dao = mock(CourseDao.class);
        CourseProvisioner provisioner = new CourseProvisioner(dao, Arrays.asList("c-1", "c-2"));

        provisioner.provision("student-1", "张三", Role.STUDENT);
        provisioner.provision("student-1", "张三", Role.STUDENT);

        ArgumentCaptor<Student> saved = ArgumentCaptor.forClass(Student.class);
        verify(dao, times(2)).saveStudent(saved.capture());
        assertEquals(saved.getAllValues().get(0).getCollegeUuid(),
                saved.getAllValues().get(1).getCollegeUuid(), "两次开户应落在同一所学院");
    }

    /** 池里有多所学院时不会永远挑第一所。 */
    @Test
    void poolOfSeveralCollegesIsActuallySpread() {
        CourseDao dao = mock(CourseDao.class);
        CourseProvisioner provisioner = new CourseProvisioner(dao, Arrays.asList("c-1", "c-2"));

        Set<String> used = new HashSet<String>();
        for (int i = 0; i < 40; i++) {
            provisioner.provision("student-" + i, "学生" + i, Role.STUDENT);
        }

        ArgumentCaptor<Student> saved = ArgumentCaptor.forClass(Student.class);
        verify(dao, times(40)).saveStudent(saved.capture());
        for (Student student : saved.getAllValues()) {
            assertTrue(Arrays.asList("c-1", "c-2").contains(student.getCollegeUuid()));
            used.add(student.getCollegeUuid());
        }

        assertEquals(2, used.size(), "40 个账号应当把两所学院都用上，实际 " + used);
    }

    /** 池里只有一所学院时，全校都挂它 —— 与过去的单学院缺省行为一致。 */
    @Test
    void singleCollegePoolPutsEveryoneInIt() {
        CourseDao dao = mock(CourseDao.class);
        CourseProvisioner provisioner = new CourseProvisioner(dao, Arrays.asList("only"));

        provisioner.provision("student-9", "王五", Role.STUDENT);

        assertEquals("only", lastStudentOf(dao).getCollegeUuid());
    }

    /** 管理员不建课程档案。 */
    @Test
    void adminIsNotProvisioned() {
        CourseDao dao = mock(CourseDao.class);
        CourseProvisioner provisioner = new CourseProvisioner(dao, Arrays.asList("c-1"));

        provisioner.provision("admin-1", "管理员", Role.ADMIN);

        verify(dao, never()).saveStudent(any(Student.class));
        verify(dao, never()).saveTeacher(any(Teacher.class));
    }

    /** 空池是配置错误：直接拒掉，别等到注册时撞外键才发现。 */
    @Test
    void emptyPoolIsRejected() {
        final CourseDao dao = mock(CourseDao.class);

        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() {
                new CourseProvisioner(dao, new ArrayList<String>());
            }
        });
        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() {
                new CourseProvisioner(dao, null);
            }
        });
    }

    /**
     * 取替身里最近一次保存的学生档案。
     *
     * @param dao 目录替身
     * @return 学生档案
     */
    private static Student lastStudentOf(CourseDao dao) {
        ArgumentCaptor<Student> saved = ArgumentCaptor.forClass(Student.class);
        verify(dao, atLeastOnce()).saveStudent(saved.capture());
        return saved.getValue();
    }

    /**
     * 取替身里最近一次保存的教师档案。
     *
     * @param dao 目录替身
     * @return 教师档案
     */
    private static Teacher lastTeacherOf(CourseDao dao) {
        ArgumentCaptor<Teacher> saved = ArgumentCaptor.forClass(Teacher.class);
        verify(dao, atLeastOnce()).saveTeacher(saved.capture());
        return saved.getValue();
    }
}
