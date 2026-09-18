package edu.seu.vcampus.common.course;

import java.util.List;
import java.util.Set;

/**
 * 选课规则谓词（双端共享）：学生选课资格、教师认领方向、时间槽覆盖/冲突与教室容量判定。
 *
 * <p>服务端据此做最终校验，客户端可复用同一份规则做预校验（与 {@code Permissions} 同为共享逻辑）。
 */
public final class CourseRules {

    private CourseRules() {
    }

    /**
     * 教师是否具备课程要求的研究方向；课程未要求研究方向时视为不限。
     * @param teacher 教师
     * @param course 课程
     * @return 是否具备
     */
    public static boolean hasRequiredDirection(Teacher teacher, CourseSection course) {
        return hasAllTags(teacher.getResearchDirections(), course.getRequiredDirections());
    }

    /**
     * isHave 语义：判断 {@code have} 是否包含 {@code need} 里的「全部」标签；
     * {@code need} 为空时视为不限。
     *
     * @param have 教师已拥有的标签集合
     * @param need 课程要求的标签集合
     * @return 是否全部满足
     */
    public static boolean hasAllTags(Set<Field> have, Set<Field> need) {
        if (need == null || need.isEmpty()) {
            return true;
        }
        if (have == null) {
            return false;
        }
        return have.containsAll(need);
    }

    /**
     * 学生是否符合选课条件：同学院，或学生专业在课程指定专业内。
     * @param student 学生
     * @param course 课程
     * @return 是否符合
     */
    public static boolean isEligible(Student student, CourseSection course) {
        if (course.getCollegeUuid() != null
                && course.getCollegeUuid().equals(student.getCollegeUuid())) {
            return true;
        }
        return student.getMajor() != null
                && course.getEligibleMajors().contains(student.getMajor());
    }

    /**
     * 时间槽是否被某个可用时间槽覆盖；可用集合为空表示未声明、视为不限。
     * @param available 可用时间槽集合
     * @param slot 待校验时间槽
     * @return 是否被覆盖
     */
    public static boolean coveredByAny(Set<Timeslot> available, Timeslot slot) {
        if (available == null || available.isEmpty()) {
            return true;
        }
        for (Timeslot candidate : available) {
            if (candidate.covers(slot)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 两组时间槽是否存在任意冲突。
     * @param a 时间槽集合
     * @param b 时间槽集合
     * @return 是否存在冲突
     */
    public static boolean overlapsAny(Set<Timeslot> a, Set<Timeslot> b) {
        if (a == null || b == null) {
            return false;
        }
        for (Timeslot x : a) {
            for (Timeslot y : b) {
                if (x.overlaps(y)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 教室是否满足课程安排：容量足够且全部时间槽可用。
     * @param classroom 教室
     * @param course 课程
     * @param timeslots 时间槽
     * @return 是否满足
     */
    public static boolean classroomFits(Classroom classroom, CourseSection course,
            List<Timeslot> timeslots) {
        if (classroom.getCapacity() < course.getCapacity()
                || classroom.getCapacity() < course.getEnrolledCount()) {
            return false;
        }
        for (Timeslot slot : timeslots) {
            if (!coveredByAny(classroom.getAvailableTimeslots(), slot)) {
                return false;
            }
        }
        return true;
    }
}
