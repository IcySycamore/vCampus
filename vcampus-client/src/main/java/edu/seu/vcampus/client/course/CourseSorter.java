package edu.seu.vcampus.client.course;

import edu.seu.vcampus.common.course.Course;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 课程管理表格的排序工具：按列排序（升/降），空值安全。
 *
 * <p>列索引与 {@code CourseAdminPanel} 的表格列顺序一致，作为纯函数供界面与单测复用。
 */
final class CourseSorter {

    /** 列索引：课程编号。 */
    static final int COL_CODE = 0;
    /** 列索引：课程名称。 */
    static final int COL_NAME = 1;
    /** 列索引：学分。 */
    static final int COL_CREDIT = 2;
    /** 列索引：学期。 */
    static final int COL_SEMESTER = 3;
    /** 列索引：授课教师。 */
    static final int COL_TEACHER = 4;
    /** 列索引：容量。 */
    static final int COL_CAPACITY = 5;
    /** 列索引：已选。 */
    static final int COL_ENROLLED = 6;

    private CourseSorter() {
    }

    /**
     * 按列排序课程。
     *
     * @param courses 待排序课程
     * @param column 列索引
     * @param ascending 是否升序
     * @return 新的已排序列表
     */
    static List<Course> sort(List<Course> courses, int column, boolean ascending) {
        List<Course> result = courses == null ? new ArrayList<Course>() : new ArrayList<Course>(courses);
        Comparator<Course> comparator = comparator(column);
        if (comparator == null) {
            return result;
        }
        Collections.sort(result, ascending ? comparator : Collections.reverseOrder(comparator));
        return result;
    }

    private static Comparator<Course> comparator(final int column) {
        switch (column) {
            case COL_CODE:
                return new Comparator<Course>() {
                    @Override
                    public int compare(Course a, Course b) {
                        return nullSafe(a.getCode()).compareTo(nullSafe(b.getCode()));
                    }
                };
            case COL_NAME:
                return new Comparator<Course>() {
                    @Override
                    public int compare(Course a, Course b) {
                        return nullSafe(a.getName()).compareTo(nullSafe(b.getName()));
                    }
                };
            case COL_CREDIT:
                return new Comparator<Course>() {
                    @Override
                    public int compare(Course a, Course b) {
                        return Integer.compare(a.getCredit(), b.getCredit());
                    }
                };
            case COL_SEMESTER:
                return new Comparator<Course>() {
                    @Override
                    public int compare(Course a, Course b) {
                        return nullSafe(a.getSemester()).compareTo(nullSafe(b.getSemester()));
                    }
                };
            case COL_TEACHER:
                return new Comparator<Course>() {
                    @Override
                    public int compare(Course a, Course b) {
                        return nullSafe(a.getTeacherUuid()).compareTo(nullSafe(b.getTeacherUuid()));
                    }
                };
            case COL_CAPACITY:
                return new Comparator<Course>() {
                    @Override
                    public int compare(Course a, Course b) {
                        return Integer.compare(a.getCapacity(), b.getCapacity());
                    }
                };
            case COL_ENROLLED:
                return new Comparator<Course>() {
                    @Override
                    public int compare(Course a, Course b) {
                        return Integer.compare(a.getEnrolled(), b.getEnrolled());
                    }
                };
            default:
                return null;
        }
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
