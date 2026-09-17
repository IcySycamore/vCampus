package edu.seu.vcampus.client.course;

import edu.seu.vcampus.common.course.Course;
import edu.seu.vcampus.common.course.Timeslot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 选课界面的业务动作控制器：筛选渲染 + 选课/退课前的校验。
 *
 * <p>
 * 原实现把「退课」按钮作用在整张课程表上，学生选中一门**没选过**的课也会发退课请求，由服务端报错兜底；
 * 而且表格里看不出哪些已经选过。现在控制器持有「我已选」集合，先按状态给出提示，再决定是否发请求。
 */
final class CourseController {

    final CourseSelectPanel panel;

    /** 本人已选课程的编号。 */
    private final Set<String> selectedCodes = new HashSet<String>();

    CourseController(CourseSelectPanel panel) {
        this.panel = panel;
    }

    /** @param code 课程编号 @return 本人是否已选 */
    boolean isSelected(String code) {
        return code != null && selectedCodes.contains(code);
    }

    /** @param courses 本人已选课程 */
    void setSelections(List<Course> courses) {
        selectedCodes.clear();
        if (courses != null) {
            for (Course course : courses) {
                if (course != null && course.getCode() != null) {
                    selectedCodes.add(course.getCode());
                }
            }
        }
    }

    /** 按关键词筛选并重渲染课程表格。 */
    void applyFilter() {
        List<Course> filtered = CourseSelectPanel.filterCourses(panel.allCourses,
                panel.keywordField.getText());
        panel.courseModel.setRowCount(0);
        int selected = 0;
        for (Course course : filtered) {
            boolean mine = isSelected(course.getCode());
            if (mine) {
                selected++;
            }
            panel.courseModel.addRow(row(course, mine, conflictsWithSelection(course)));
        }
        panel.statusLabel.setText("  共 " + filtered.size() + " 门课程，已选 " + selected
                + " 门（列表显示的余量为「容量 − 已选」）");
    }

    /** 选中行后发起选课。 */
    void selectSelected() {
        Course course = selectedCourse();
        if (course == null) {
            panel.statusLabel.setText("  请先选择一门课程");
            return;
        }
        if (isSelected(course.getCode())) {
            panel.statusLabel.setText("  该课程你已经选过，无需重复选课");
            return;
        }
        if (course.getEnrolled() >= course.getCapacity()) {
            panel.statusLabel.setText("  该课程已满（容量 " + course.getCapacity() + "）");
            return;
        }
        panel.selectCourse(course.getCode());
    }

    /** 选中行后发起退课。 */
    void dropSelected() {
        Course course = selectedCourse();
        if (course == null) {
            panel.statusLabel.setText("  请先选择一门课程");
            return;
        }
        if (!isSelected(course.getCode())) {
            panel.statusLabel.setText("  该课程你并未选修，无需退课");
            return;
        }
        panel.dropCourse(course.getCode());
    }

    /** @return 选中行的课程，未选中返回 null */
    Course selectedCourse() {
        int row = panel.courseTable.getSelectedRow();
        if (row < 0) {
            return null;
        }
        String code = String.valueOf(panel.courseModel.getValueAt(row, 0));
        for (Course course : panel.allCourses) {
            if (code.equals(course.getCode())) {
                return course;
            }
        }
        return null;
    }

    /**
     * 该课程是否与本人已选课程时间重叠。
     *
     * @param course 待判定课程
     * @return 重叠返回 true
     */
    boolean conflictsWithSelection(Course course) {
        Timeslot slot = course == null ? null : course.getTimeslot();
        if (slot == null || isSelected(course.getCode())) {
            return false;
        }
        for (Course mine : panel.allCourses) {
            if (!isSelected(mine.getCode()) || mine.getTimeslot() == null) {
                continue;
            }
            if (mine.getTimeslot().overlaps(slot)) {
                return true;
            }
        }
        return false;
    }

    private Object[] row(Course course, boolean selected, boolean conflict) {
        return new Object[] {
                course.getCode(), course.getName(), Integer.valueOf(course.getCredit()),
                CourseDisplay.teacherOf(course), CourseDisplay.timeOf(course),
                CourseDisplay.remainingOf(course), Integer.valueOf(course.getEnrolled()),
                CourseDisplay.statusOf(course, selected, conflict)
        };
    }

    /** @return 关键词为空的判断（供界面回车搜索提示用） */
    boolean hasKeyword() {
        String text = panel.keywordField.getText();
        return text != null && text.trim().length() > 0;
    }

    /** @return 已选课程编号快照 */
    List<String> selectedCodeList() {
        return new ArrayList<String>(selectedCodes);
    }
}
