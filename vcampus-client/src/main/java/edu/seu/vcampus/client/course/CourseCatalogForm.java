package edu.seu.vcampus.client.course;

import edu.seu.vcampus.client.view.theme.UiTheme;
import edu.seu.vcampus.common.course.College;
import edu.seu.vcampus.common.course.Course;
import edu.seu.vcampus.common.course.Teacher;
import edu.seu.vcampus.common.course.dto.CourseSaveRequest;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * 课程目录的「新增 / 修改」表单。
 *
 * <p>
 * 授课教师与学院都用下拉选择：选项显示姓名 / 学院名称，取到的是 uuid。原实现让管理员手抄 36 位 uuid 才能认领教师，抄错只会得到「授课教师不存在」。
 *
 * <p>
 * 容量按课程实际值回填，服务端的 40-100 只在你改动容量时才校验（存量课程容量可能小于 40）。
 */
final class CourseCatalogForm extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JTextField codeField = new JTextField();
    private final JTextField nameField = new JTextField();
    private final JTextField creditField = new JTextField();
    private final JTextField semesterField = new JTextField();
    private final JTextField capacityField = new JTextField();
    private final JComboBox<String> teacherBox = new JComboBox<String>();
    private final JComboBox<String> collegeBox = new JComboBox<String>();

    private List<Teacher> teachers = new ArrayList<Teacher>();
    private List<College> colleges = new ArrayList<College>();

    /** 被编辑的课程；null 表示新增。 */
    private Course editing;

    /** 打开表单时的容量与学院，用于判断是否真的改动过。 */
    private int capacityBaseline;
    private String collegeBaseline;

    CourseCatalogForm() {
        setLayout(new GridBagLayout());
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        addRow(0, "课程编号", codeField);
        addRow(1, "课程名称", nameField);
        addRow(2, "学分", creditField);
        addRow(3, "学期", semesterField);
        addRow(4, "容量", capacityField);
        addRow(5, "授课教师", teacherBox);
        addRow(6, "开课学院", collegeBox);
    }

    /**
     * 提供下拉候选。
     *
     * @param teacherList 全部教师
     * @param collegeList 全部学院
     */
    void setOptions(List<Teacher> teacherList, List<College> collegeList) {
        this.teachers = teacherList == null ? new ArrayList<Teacher>() : teacherList;
        this.colleges = collegeList == null ? new ArrayList<College>() : collegeList;
    }

    /**
     * 填充表单。
     *
     * @param course 被编辑课程；null 表示新增
     */
    void render(Course course) {
        this.editing = course;
        codeField.setEditable(course == null);
        codeField.setText(course == null ? "" : nullSafe(course.getCode()));
        nameField.setText(course == null ? "" : nullSafe(course.getName()));
        creditField.setText(course == null ? "3" : String.valueOf(course.getCredit()));
        semesterField.setText(course == null ? "2026-2027-1" : nullSafe(course.getSemester()));
        capacityBaseline = course == null ? 40 : course.getCapacity();
        capacityField.setText(String.valueOf(capacityBaseline));
        collegeBaseline = course == null ? null : course.getCollegeUuid();
        populateTeacherBox(course == null ? null : course.getTeacherUuid());
        populateCollegeBox(collegeBaseline);
    }

    /**
     * 组装请求。
     *
     * @return 请求；校验不通过返回 null
     */
    CourseSaveRequest request() {
        if (codeField.getText().trim().length() == 0) {
            return null;
        }
        if (nameField.getText().trim().length() == 0) {
            return null;
        }
        Integer capacity;
        Integer credit;
        try {
            capacity = Integer.valueOf(capacityField.getText().trim());
            credit = Integer.valueOf(creditField.getText().trim());
        } catch (NumberFormatException exception) {
            return null;
        }
        CourseSaveRequest request = new CourseSaveRequest();
        request.setCode(codeField.getText().trim());
        request.setName(nameField.getText().trim());
        request.setCredit(credit);
        request.setSemester(semesterField.getText().trim());
        if (editing == null || capacity.intValue() != capacityBaseline) {
            request.setCapacity(capacity);
        }
        String college = selectedCollegeUuid();
        if (college != null) {
            String baseline = collegeBaseline == null ? "" : collegeBaseline;
            if (editing == null || !college.equals(baseline)) {
                request.setCollegeUuid(college);
            }
        }
        int teacherIndex = teacherBox.getSelectedIndex();
        request.setTeacherUuid(teacherIndex <= 0 ? ""
                : teachers.get(teacherIndex - 1).getUuid());
        return request;
    }

    /** @return 校验失败原因，通过返回 null */
    String errorMessage() {
        if (codeField.getText().trim().length() == 0) {
            return "课程编号不能为空";
        }
        if (nameField.getText().trim().length() == 0) {
            return "课程名称不能为空";
        }
        try {
            Integer.parseInt(creditField.getText().trim());
        } catch (NumberFormatException exception) {
            return "学分必须为整数";
        }
        try {
            Integer.parseInt(capacityField.getText().trim());
        } catch (NumberFormatException exception) {
            return "容量必须为整数";
        }
        return null;
    }

    private void populateTeacherBox(String selectedUuid) {
        teacherBox.removeAllItems();
        teacherBox.addItem("（未认领）");
        for (Teacher teacher : teachers) {
            String name = teacher.getName();
            teacherBox.addItem(name == null || name.trim().length() == 0
                    ? "（未命名教师）" + CourseDisplay.shortUuid(teacher.getUuid())
                    : name.trim() + "（" + CourseDisplay.shortUuid(teacher.getUuid()) + "）");
        }
        if (selectedUuid == null) {
            teacherBox.setSelectedIndex(0);
            return;
        }
        for (int i = 0; i < teachers.size(); i++) {
            if (selectedUuid.equals(teachers.get(i).getUuid())) {
                teacherBox.setSelectedIndex(i + 1);
                return;
            }
        }
        teacherBox.setSelectedIndex(0);
    }

    private void populateCollegeBox(String selectedUuid) {
        collegeBox.removeAllItems();
        // 第 0 项一律是「不改动学院」：之前是「（不修改）」，选中它会把学院清空，而学院是课程的非空外键
        collegeBox.addItem(editing == null ? "（按平台缺省学院）" : "（保持不变）");
        for (College college : colleges) {
            String name = college.getName();
            collegeBox.addItem(name == null || name.trim().length() == 0
                    ? "（未命名学院）" + CourseDisplay.shortUuid(college.getUuid())
                    : name.trim());
        }
        if (selectedUuid == null) {
            collegeBox.setSelectedIndex(0);
            return;
        }
        for (int i = 0; i < colleges.size(); i++) {
            if (selectedUuid.equals(colleges.get(i).getUuid())) {
                collegeBox.setSelectedIndex(i + 1);
                return;
            }
        }
        // 学院不在列表里（列表未加载成功等）：把原值摆出来供保留，不能让界面显示成「保持不变」
        collegeBox.addItem(selectedUuid);
        collegeBox.setSelectedIndex(collegeBox.getItemCount() - 1);
    }

    private String selectedCollegeUuid() {
        int index = collegeBox.getSelectedIndex();
        if (index <= 0) {
            return null;
        }
        if (index - 1 < colleges.size()) {
            return colleges.get(index - 1).getUuid();
        }
        String text = collegeBox.getItemAt(index);
        return text == null ? null : text.trim();
    }

    private void addRow(int row, String text, java.awt.Component field) {
        JLabel label = new JLabel(text);
        label.setForeground(UiTheme.MUTED);
        label.setFont(UiTheme.font(Font.BOLD, 14F));
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = row;
        labelConstraints.anchor = GridBagConstraints.WEST;
        labelConstraints.insets = new Insets(4, 4, 4, 10);
        add(label, labelConstraints);

        if (field instanceof JTextField) {
            ((JTextField) field).setColumns(16);
        }
        GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.gridx = 1;
        fieldConstraints.gridy = row;
        fieldConstraints.weightx = 1;
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.insets = new Insets(4, 0, 4, 4);
        add(field, fieldConstraints);
    }

    private static String nullSafe(String text) {
        return text == null ? "" : text;
    }
}
