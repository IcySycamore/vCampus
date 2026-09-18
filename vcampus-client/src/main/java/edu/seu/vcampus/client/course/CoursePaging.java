package edu.seu.vcampus.client.course;

import edu.seu.vcampus.common.course.Course;
import edu.seu.vcampus.common.message.PageResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * 课程管理表格的分页工具：把已筛选排序的课程列表切成一页。
 *
 * <p>作为纯函数供界面与单测复用；页码越界时夹到合法区间。
 */
final class CoursePaging {

    private CoursePaging() {
    }

    /**
     * 切出指定页。
     *
     * @param courses 已排序课程
     * @param pageNumber 页码（从 1 开始）
     * @param pageSize 每页条数
     * @return 分页结果
     */
    static PageResponse<Course> page(List<Course> courses, int pageNumber, int pageSize) {
        List<Course> all = courses == null ? new ArrayList<Course>() : courses;
        int number = PageResponse.normalizePageNumber(pageNumber);
        int size = PageResponse.normalizePageSize(pageSize);
        int total = all.size();
        int totalPages = total == 0 ? 0 : (total + size - 1) / size;
        int clamped = totalPages == 0 ? 1 : Math.min(number, totalPages);
        int from = (clamped - 1) * size;
        int to = Math.min(from + size, total);
        List<Course> items = from >= total ? new ArrayList<Course>()
                : new ArrayList<Course>(all.subList(from, to));
        return new PageResponse<Course>(items, total, clamped, size);
    }
}
