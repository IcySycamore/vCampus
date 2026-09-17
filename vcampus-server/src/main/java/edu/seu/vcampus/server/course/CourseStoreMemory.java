package edu.seu.vcampus.server.course;

import edu.seu.vcampus.common.course.Classroom;
import edu.seu.vcampus.common.course.College;
import edu.seu.vcampus.common.course.CourseSection;
import edu.seu.vcampus.common.course.Student;
import edu.seu.vcampus.common.course.Teacher;

import java.util.Collections;
import java.util.List;

/**
 * 不持久化的课程目录后端，保持改造前的纯内存行为。
 *
 * <p>
 * 缺省装配用它，因此没接数据库的机器上（含绝大多数单元测试）行为与改造前逐字一致：读回空、 写入都成功，真正的状态仍只在 {@link CourseDao} 的内存表里。
 */
public final class CourseStoreMemory implements CourseStore {

    /** @return 空列表，内存版没有可恢复的历史 */
    @Override
    public List<College> loadColleges() {
        return Collections.emptyList();
    }

    /** @return 空列表，内存版没有可恢复的历史 */
    @Override
    public List<Teacher> loadTeachers() {
        return Collections.emptyList();
    }

    /** @return 空列表，内存版没有可恢复的历史 */
    @Override
    public List<Student> loadStudents() {
        return Collections.emptyList();
    }

    /** @return 空列表，内存版没有可恢复的历史 */
    @Override
    public List<Classroom> loadClassrooms() {
        return Collections.emptyList();
    }

    /** @return 空列表，内存版没有可恢复的历史 */
    @Override
    public List<CourseSection> loadCourses() {
        return Collections.emptyList();
    }

    /** @return 恒为 true，内存版无需写入 */
    @Override
    public boolean saveCollege(College college) {
        return true;
    }

    /** @return 恒为 true，内存版无需写入 */
    @Override
    public boolean saveTeacher(Teacher teacher) {
        return true;
    }

    /** @return 恒为 true，内存版无需写入 */
    @Override
    public boolean saveStudent(Student student) {
        return true;
    }

    /** @return 恒为 true，内存版无需写入 */
    @Override
    public boolean saveClassroom(Classroom classroom) {
        return true;
    }

    /** @return 恒为 true，内存版无需写入 */
    @Override
    public boolean saveCourse(CourseSection course) {
        return true;
    }

    /** @return 恒为 true，内存版没有可删的行（内存里已由 {@link CourseDao} 自己删掉） */
    @Override
    public boolean deleteTeacher(String uuid) {
        return true;
    }
}
