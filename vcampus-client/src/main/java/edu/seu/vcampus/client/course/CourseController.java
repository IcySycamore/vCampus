package edu.seu.vcampus.client.course;

import edu.seu.vcampus.common.course.Course;

import java.util.List;

/**
 * 选课界面的业务动作控制器。
 */
final class CourseController {

    final CourseSelectPanel panel;

    CourseController(CourseSelectPanel panel) {
        this.panel = panel;
    }

    /** 按关键词筛选并重渲染课程表格。 */
    void applyFilter() {
        List<Course> filtered = CourseSelectPanel.filterCourses(panel.allCourses,
                panel.keywordField.getText());
        panel.courseModel.setRowCount(0);
        for (Course course : filtered) {
            panel.courseModel.addRow(row(course));
        }
    }

    /** 选中行后发起选课。 */
    void selectSelected() {
        int row = panel.courseTable.getSelectedRow();
        if (row < 0) {
            panel.statusLabel.setText("  请先选择一门课程");
            return;
        }
        panel.selectCourse(String.valueOf(panel.courseModel.getValueAt(row, 0)));
    }

    /** 选中行后发起退课。 */
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
}
