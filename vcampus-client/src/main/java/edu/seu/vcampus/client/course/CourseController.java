package edu.seu.vcampus.client.course;

import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.course.Course;
import edu.seu.vcampus.common.message.Message;

import java.util.ArrayList;
import java.util.List;

/**
 * 选课界面的业务动作与响应处理控制器。
 */
final class CourseController {

    final CourseSelectPanel panel;

    CourseController(CourseSelectPanel panel) {
        this.panel = panel;
    }

    void applyFilter() {
        List<Course> filtered = CourseSelectPanel.filterCourses(panel.allCourses,
                panel.keywordField.getText());
        panel.courseModel.setRowCount(0);
        for (Course course : filtered) {
            panel.courseModel.addRow(row(course));
        }
    }

    void applyResponse(Message message) {
        if (!StatusCode.SUCCESS.equals(message.getStatusCode())) {
            panel.statusLabel.setText("  " + String.valueOf(message.getData()));
            return;
        }
        if (message.getCommand() == CourseCommand.COURSE_LIST) {
            panel.renderCourses(toCourseList(message.getData()));
            panel.statusLabel.setText("  可选课程已更新，共 " + panel.courseModel.getRowCount() + " 门");
        } else {
            panel.statusLabel.setText("  " + String.valueOf(message.getData()));
        }
    }

    void selectSelected() {
        int row = panel.courseTable.getSelectedRow();
        if (row < 0) {
            panel.statusLabel.setText("  请先选择一门课程");
            return;
        }
        panel.selectCourse(String.valueOf(panel.courseModel.getValueAt(row, 0)));
    }

    void dropSelected() {
        int row = panel.courseTable.getSelectedRow();
        if (row < 0) {
            panel.statusLabel.setText("  请先选择一门课程");
            return;
        }
        panel.dropCourse(String.valueOf(panel.courseModel.getValueAt(row, 0)));
    }

    private Object[] row(Course course) {
        return new Object[] {
            course.getCode(), course.getName(), Integer.valueOf(course.getCredit()),
            course.getTeacherUuid(), Integer.valueOf(course.getCapacity()),
            Integer.valueOf(course.getEnrolled())
        };
    }

    private List<Course> toCourseList(Object data) {
        List<Course> result = new ArrayList<Course>();
        if (data instanceof List) {
            List<?> values = (List<?>) data;
            for (Object value : values) {
                if (value instanceof Course) {
                    result.add((Course) value);
                }
            }
        }
        return result;
    }
}
