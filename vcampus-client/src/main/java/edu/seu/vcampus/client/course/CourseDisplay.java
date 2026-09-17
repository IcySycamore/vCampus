package edu.seu.vcampus.client.course;

import edu.seu.vcampus.common.course.Course;
import edu.seu.vcampus.common.course.CourseScheduler;
import edu.seu.vcampus.common.course.Timeslot;

import java.util.Map;

/**
 * 选课界面共用的展示文本。
 *
 * <p>
 * 各处表格原先直接打印 uuid、时间槽对象，用户看到的是「00000000-0000-0000-0000-0000000000a3」和
 * 「Timeslot@1f2a」这类原始值。文案集中在这里，避免选课 / 课表 / 排课 / 课程目录四处各写一份而逐渐走样。
 */
final class CourseDisplay {

    private CourseDisplay() {
    }

    /**
     * 授课教师：优先姓名，其次「未认领」，最后退回 uuid 前 8 位（姓名缺失时仍要能分辨）。
     *
     * @param course 课程
     * @return 展示文案
     */
    static String teacherOf(Course course) {
        if (course == null) {
            return "--";
        }
        String name = course.getTeacherName();
        if (name != null && name.trim().length() > 0) {
            return name.trim();
        }
        if (course.getTeacherUuid() == null) {
            return "未认领";
        }
        return "（未命名教师）" + shortUuid(course.getTeacherUuid());
    }

    /**
     * 上课时间：星期 + 节次 + 教学周；未排课返回「未排课」。
     *
     * @param course 课程
     * @return 展示文案
     */
    static String timeOf(Course course) {
        Timeslot slot = course == null ? null : course.getTimeslot();
        if (slot == null) {
            return "未排课";
        }
        StringBuilder text = new StringBuilder();
        text.append(CourseScheduler.weekdayName(slot.getWeekday())).append(' ')
                .append(CourseScheduler.periodRangeText(slot));
        if (course.getStartWeek() != null && course.getEndWeek() != null) {
            text.append(' ').append(course.getStartWeek()).append('-')
                    .append(course.getEndWeek()).append('周');
        }
        return text.toString();
    }

    /**
     * 上课教室：按 uuid 查名称，查不到时退回短 uuid。
     *
     * @param course    课程
     * @param roomNames 教室 uuid → 名称
     * @return 展示文案
     */
    static String classroomOf(Course course, Map<String, String> roomNames) {
        if (course == null || course.getClassroomUuid() == null) {
            return "未安排";
        }
        String name = roomNames == null ? null : roomNames.get(course.getClassroomUuid());
        if (name != null && name.trim().length() > 0) {
            return name.trim();
        }
        return "（未知教室）" + shortUuid(course.getClassroomUuid());
    }

    /**
     * 余量：容量减已选，而不是让学生自己算（容量/已选两列同时给反而看不出能不能选）。
     *
     * @param course 课程
     * @return 展示文案
     */
    static String remainingOf(Course course) {
        if (course == null) {
            return "--";
        }
        int left = course.getCapacity() - course.getEnrolled();
        return left <= 0 ? "已满" : String.valueOf(left);
    }

    /**
     * 状态列：把我是否已选、时间是否冲突、是否已满合成一列。
     *
     * @param course   课程
     * @param selected 本人是否已选
     * @param conflict 是否与本人已选课程时间冲突
     * @return 展示文案
     */
    static String statusOf(Course course, boolean selected, boolean conflict) {
        if (selected) {
            return conflict ? "已选（时间冲突）" : "已选";
        }
        if (conflict) {
            return "与已选冲突";
        }
        if (course != null && course.getEnrolled() >= course.getCapacity()) {
            return "已满";
        }
        return "可选";
    }

    /**
     * @param uuid 完整 uuid
     * @return 前 8 位，用于在姓名缺失时仍可分辨
     */
    static String shortUuid(String uuid) {
        if (uuid == null) {
            return "--";
        }
        return uuid.length() <= 8 ? uuid : uuid.substring(0, 8);
    }
}
