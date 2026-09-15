package edu.seu.vcampus.client.course;

import edu.seu.vcampus.common.course.Course;

import java.util.ArrayList;
import java.util.List;

/**
 * 课程关键词筛选工具。
 */
final class CourseFilters {

    private CourseFilters() {
    }

    /**
     * 按关键词筛选课程：编号或名称包含关键词（忽略大小写），关键词为空返回全部。
     *
     * @param courses 待筛选课程
     * @param keyword 搜索关键词
     * @return 匹配的课程列表
     */
    static List<Course> filter(List<Course> courses, String keyword) {
        List<Course> result = new ArrayList<Course>();
        if (courses == null) {
            return result;
        }
        String key = keyword == null ? "" : keyword.trim().toLowerCase();
        for (Course course : courses) {
            if (key.length() == 0 || matches(course, key)) {
                result.add(course);
            }
        }
        return result;
    }

    private static boolean matches(Course course, String key) {
        return containsIgnoreCase(course.getCode(), key)
                || containsIgnoreCase(course.getName(), key);
    }

    private static boolean containsIgnoreCase(String value, String key) {
        return value != null && value.toLowerCase().contains(key);
    }
}
