package edu.seu.vcampus.server.course;

import edu.seu.vcampus.common.course.Classroom;
import edu.seu.vcampus.common.course.College;
import edu.seu.vcampus.common.course.CourseSection;
import edu.seu.vcampus.common.course.Student;
import edu.seu.vcampus.common.course.Teacher;
import edu.seu.vcampus.common.random.RandomGen;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 课程模块数据访问对象（内存实现）：管理课程目录与课程，学院与教师双向索引。
 */
public class CourseDao {

    private final ConcurrentMap<String, CourseSection> m_courses = new ConcurrentHashMap<String, CourseSection>();
    private final ConcurrentMap<String, College> m_colleges = new ConcurrentHashMap<String, College>();
    private final ConcurrentMap<String, Teacher> m_teachers = new ConcurrentHashMap<String, Teacher>();
    private final ConcurrentMap<String, Student> m_students = new ConcurrentHashMap<String, Student>();
    private final ConcurrentMap<String, Classroom> m_classrooms = new ConcurrentHashMap<String, Classroom>();
    private final RandomGen m_random = new RandomGen();

    /** 持久化后端；缺省为不落库的内存实现。 */
    private final CourseStore m_store;

    /** 构造一个空的内存课程数据访问对象。 */
    public CourseDao() {
        this(new CourseStoreMemory());
    }

    /**
     * 指定持久化后端构造，并立即恢复已落库的学院、教师、学生、教室与课程。
     *
     * @param store 持久化后端；null 视作不落库
     */
    public CourseDao(CourseStore store) {
        m_store = store == null ? new CourseStoreMemory() : store;
        restore();
    }

    /**
     * 从后端恢复数据，并重建三处反向索引。
     *
     * <p>
     * 反查关系（学院下的教师、教师认领的课程、学生已选的课程）在库里只存正方向，恢复时由正方向 推出来，避免同一关系存两份、日后对不上。
     */
    private void restore() {
        for (College college : m_store.loadColleges()) {
            m_colleges.put(college.getUuid(), college);
        }
        for (Teacher teacher : m_store.loadTeachers()) {
            m_teachers.put(teacher.getUuid(), teacher);
            // 授课学院是可空外键，而 ConcurrentHashMap 不接受 null key（直接 get 抛 NPE）。
            // 之前走内存后端时 loadTeachers() 返回空表、循环不执行，这条一直没被踩到。
            College college = teacher.getCollegeUuid() == null ? null
                    : m_colleges.get(teacher.getCollegeUuid());
            if (college != null) {
                college.getTeacherUuids().add(teacher.getUuid());
            }
        }
        for (Student student : m_store.loadStudents()) {
            m_students.put(student.getUuid(), student);
        }
        for (Classroom classroom : m_store.loadClassrooms()) {
            m_classrooms.put(classroom.getUuid(), classroom);
        }
        for (CourseSection course : m_store.loadCourses()) {
            m_courses.put(course.getUuid(), course);
            // 授课教师可空（尚未排课），同上不可直接当 map key
            Teacher teacher = course.getTeacherUuid() == null ? null
                    : m_teachers.get(course.getTeacherUuid());
            if (teacher != null) {
                teacher.getClaimedCourseUuids().add(course.getUuid());
            }
            for (String studentUuid : course.getStudentUuids()) {
                Student student = m_students.get(studentUuid);
                if (student != null) {
                    student.getSelectedCourseUuids().add(course.getUuid());
                }
            }
        }
    }

    /**
     * @return uuid 字符串
     */
    public String newUuid() {
        return m_random.getUuid().toString();
    }

    /**
     * @param uuid 课程 uuid
     * @return 课程，不存在返回 null
     */
    public CourseSection findCourse(String uuid) {
        return uuid == null ? null : m_courses.get(uuid);
    }

    /**
     * @param course 课程
     * @return 是否成功
     */
    public boolean saveCourse(CourseSection course) {
        if (course == null) {
            return false;
        }
        if (course.getUuid() == null) {
            course.setUuid(newUuid());
        }
        m_courses.put(course.getUuid(), course);
        m_store.saveCourse(course);
        return true;
    }

    /**
     * @param uuid 学院 uuid
     * @return 学院，不存在返回 null
     */
    public College findCollege(String uuid) {
        return uuid == null ? null : m_colleges.get(uuid);
    }

    /**
     * @param college 学院
     * @return 是否成功
     */
    public boolean saveCollege(College college) {
        if (college == null) {
            return false;
        }
        if (college.getUuid() == null) {
            college.setUuid(newUuid());
        }
        m_colleges.put(college.getUuid(), college);
        m_store.saveCollege(college);
        return true;
    }

    /**
     * @param uuid 教师 uuid
     * @return 教师，不存在返回 null
     */
    public Teacher findTeacher(String uuid) {
        return uuid == null ? null : m_teachers.get(uuid);
    }

    /**
     * @param teacher 教师
     * @return 是否成功
     */
    public boolean saveTeacher(Teacher teacher) {
        if (teacher == null) {
            return false;
        }
        if (teacher.getUuid() == null) {
            teacher.setUuid(newUuid());
        }
        Teacher old = m_teachers.get(teacher.getUuid());
        if (old != null && old.getCollegeUuid() != null) {
            College oldCollege = m_colleges.get(old.getCollegeUuid());
            if (oldCollege != null) {
                oldCollege.getTeacherUuids().remove(teacher.getUuid());
            }
        }
        m_teachers.put(teacher.getUuid(), teacher);
        if (teacher.getCollegeUuid() != null) {
            College college = m_colleges.get(teacher.getCollegeUuid());
            if (college != null) {
                college.getTeacherUuids().add(teacher.getUuid());
            }
        }
        m_store.saveTeacher(teacher);
        return true;
    }

    /**
     * @param uuid 教师 uuid
     * @return 是否成功
     */
    public boolean deleteTeacher(String uuid) {
        Teacher teacher = findTeacher(uuid);
        if (teacher == null) {
            return false;
        }
        if (teacher.getCollegeUuid() != null) {
            College college = m_colleges.get(teacher.getCollegeUuid());
            if (college != null) {
                college.getTeacherUuids().remove(uuid);
            }
        }
        m_teachers.remove(uuid);
        m_store.deleteTeacher(uuid);
        return true;
    }

    /**
     * @param uuid 学生 uuid
     * @return 学生，不存在返回 null
     */
    public Student findStudent(String uuid) {
        return uuid == null ? null : m_students.get(uuid);
    }

    /**
     * @param student 学生
     * @return 是否成功
     */
    public boolean saveStudent(Student student) {
        if (student == null) {
            return false;
        }
        if (student.getUuid() == null) {
            student.setUuid(newUuid());
        }
        m_students.put(student.getUuid(), student);
        m_store.saveStudent(student);
        return true;
    }

    /**
     * @param uuid 教室 uuid
     * @return 教室，不存在返回 null
     */
    public Classroom findClassroom(String uuid) {
        return uuid == null ? null : m_classrooms.get(uuid);
    }

    /**
     * @return 全部教室快照
     */
    public List<Classroom> findAllClassrooms() {
        return new ArrayList<Classroom>(m_classrooms.values());
    }

    /**
     * @param classroom 教室
     * @return 是否成功
     */
    public boolean saveClassroom(Classroom classroom) {
        if (classroom == null) {
            return false;
        }
        if (classroom.getUuid() == null) {
            classroom.setUuid(newUuid());
        }
        m_classrooms.put(classroom.getUuid(), classroom);
        m_store.saveClassroom(classroom);
        return true;
    }

    /** @return 全部课程快照。 */
    public List<CourseSection> findAllCourses() {
        return new ArrayList<CourseSection>(m_courses.values());
    }
}
