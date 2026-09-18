package edu.seu.vcampus.server.course;

import edu.seu.vcampus.common.course.Building;
import edu.seu.vcampus.common.course.Classroom;
import edu.seu.vcampus.common.course.College;
import edu.seu.vcampus.common.course.CourseSection;
import edu.seu.vcampus.common.course.Student;
import edu.seu.vcampus.common.course.Teacher;

import java.util.List;

/**
 * 课程目录的持久化后端。
 *
 * <p>
 * {@link CourseDao} 属于「全量装进内存」的一类 DAO：选课匹配、时间冲突、学院与教师的双向索引 都在内存里算，改造它的成本远高于换掉存储。所以这里按银行
 * {@code BankStore}、成绩 {@code ScoreStore} 的同一套做法，只负责「把变更写下去、启动时读回来」，{@code CourseDao} 的 方法签名一律不动（有
 * 5 个生产文件 + 4 个测试在用）。
 *
 * <p>
 * <b>集合字段拆子表</b>，不是一个字段一列：
 * <ul>
 * <li>{@code College.researchDirections} / {@code College.majors} → {@code tblCollegeField}， 用 kind
 * 区分方向与专业；</li>
 * <li>{@code Teacher.researchDirections} → {@code tblTeacherField}；</li>
 * <li>{@code CourseSection.eligibleMajors} / {@code requiredDirections} →
 * {@code tblCourseField}；</li>
 * <li>教师、学生、课程的时间槽 → {@code tblTimeslot}，按 ownerType + ownerUuid 归属；</li>
 * <li>{@code CourseSection.studentUuids} → {@code tblCourseSelection}。</li>
 * </ul>
 * 拆表的理由是这些值要参与匹配运算（可选专业、教师方向要求），拼成逗号串就只能整串拉回来在 内存里比，索引用不上。
 *
 * <p>
 * <b>双向索引不落库</b>：{@code College.teacherUuids} 与 {@code Teacher.claimedCourseUuids} 都是反查出来的关系，库里只有
 * {@code tblTeacher.tcCollegeUuid} 与 {@code tblCourse.coTeacherUuid}
 * 这两个正方向；启动恢复时由它们重建，避免两处各存一份、日后互相矛盾。
 *
 * <p>
 * <b>写入即全量覆盖该实体的子表</b>：集合字段先清后写，否则删掉一个研究方向之后旧行还留在库里。
 */
public interface CourseStore {

    /**
     * 加载全部学院，并填好各自的研究方向与专业。
     *
     * @return 学院列表，不返回 null
     */
    List<College> loadColleges();

    /**
     * 加载全部教师，并填好研究方向与时间槽。
     *
     * @return 教师列表，不返回 null
     */
    List<Teacher> loadTeachers();

    /**
     * 加载全部选课模块的学生档案，并填好可用/偏好时间槽与已选课程。
     *
     * @return 学生列表，不返回 null
     */
    List<Student> loadStudents();

    /**
     * 加载全部教室。
     *
     * @return 教室列表，不返回 null
     */
    List<Classroom> loadClassrooms();

    /**
     * 加载全部课程，并填好可选专业、教师方向要求、时间槽与已选学生。
     *
     * @return 课程列表，不返回 null
     */
    List<CourseSection> loadCourses();

    /**
     * 写入或覆盖一个学院（含研究方向与专业）。
     *
     * @param college 学院
     * @return 写入成功为 true
     */
    boolean saveCollege(College college);

    /**
     * 写入或覆盖一个教师（含研究方向与时间槽）。
     *
     * @param teacher 教师
     * @return 写入成功为 true
     */
    boolean saveTeacher(Teacher teacher);

    /**
     * 写入或覆盖一个选课模块的学生档案（含时间槽）。
     *
     * @param student 学生
     * @return 写入成功为 true
     */
    boolean saveStudent(Student student);

    /**
     * 写入或覆盖一个教室。
     *
     * @param classroom 教室
     * @return 写入成功为 true
     */
    boolean saveClassroom(Classroom classroom);

    /**
     * 写入或覆盖一门课程（含可选专业、教师方向要求、时间槽与已选学生）。
     *
     * @param course 课程
     * @return 写入成功为 true
     */
    boolean saveCourse(CourseSection course);

    /**
     * 删除一个教师及其研究方向、时间槽。
     *
     * @param uuid 教师 uuid
     * @return 命中记录为 true
     */
    boolean deleteTeacher(String uuid);

    /**
     * 删除一门课程及其课程领域、选课、时间槽与成绩记录。
     *
     * <p>
     * 成绩表上有指向课程的外键，删课程前必须先把这些子行清掉，否则外键会拦下这条删除。
     *
     * @param uuid 课程 uuid
     * @return 命中记录为 true
     */
    boolean deleteCourse(String uuid);

    /**
     * 加载全部教学楼。
     *
     * @return 教学楼列表，不返回 null
     */
    List<Building> loadBuildings();

    /**
     * 写入或覆盖一个教学楼。
     *
     * @param building 教学楼
     * @return 写入成功为 true
     */
    boolean saveBuilding(Building building);
}
