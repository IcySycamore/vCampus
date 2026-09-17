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

    private final ConcurrentMap<String, CourseSection> m_courses =
            new ConcurrentHashMap<String, CourseSection>();
    private final ConcurrentMap<String, College> m_colleges =
            new ConcurrentHashMap<String, College>();
    private final ConcurrentMap<String, Teacher> m_teachers =
            new ConcurrentHashMap<String, Teacher>();
    private final ConcurrentMap<String, Student> m_students =
            new ConcurrentHashMap<String, Student>();
    private final ConcurrentMap<String, Classroom> m_classrooms =
            new ConcurrentHashMap<String, Classroom>();
    private final RandomGen m_random = new RandomGen();

    /** 构造一个空的内存课程数据访问对象。 */
    public CourseDao() {
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
        return true;
    }
    /** @return 全部课程快照。 */
    public List<CourseSection> findAllCourses() {
        return new ArrayList<CourseSection>(m_courses.values());
    }
}
