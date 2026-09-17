package edu.seu.vcampus.client.course;

import edu.seu.vcampus.client.view.component.RoundedBorder;
import edu.seu.vcampus.client.view.theme.UiFactory;
import edu.seu.vcampus.client.view.theme.UiTheme;
import edu.seu.vcampus.common.course.Classroom;
import edu.seu.vcampus.common.course.CourseRules;
import edu.seu.vcampus.common.course.CourseScheduler;
import edu.seu.vcampus.common.course.Field;
import edu.seu.vcampus.common.course.ScheduleEntry;
import edu.seu.vcampus.common.course.Teacher;
import edu.seu.vcampus.common.course.Timeslot;
import edu.seu.vcampus.common.course.dto.CourseSaveRequest;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * 排课左侧「待排课程」编辑表单：课程名 / uuid（只读）/ 课程编号 / 容量（只增）/ 授课教室 /
 * 授课教师 / 开始时间 / 结束时间 / 持续时长 / 课程标签（研究方向、专业、学院）。
 *
 * <p>时间联动（开始/结束/持续时长）由 {@link ScheduleTimeCalculator} 提供；教师下拉按
 * 「拥有课程全部研究方向标签」过滤（{@link CourseRules#hasAllTags}）。
 */
public class CourseEditFormPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final String[] WEEKDAYS = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};

    private final CourseService api;
    private final JLabel uuidLabel = new JLabel("--");
    private final JTextField nameField = new JTextField();
    private final JTextField codeField = new JTextField();
    private final JSpinner capacitySpinner = new JSpinner(
            new SpinnerNumberModel(40, 40, 100, 1));
    private final JComboBox<String> teacherBox = new JComboBox<String>();
    private final JComboBox<String> classroomBox = new JComboBox<String>();
    private final JComboBox<String> startPeriodBox = new JComboBox<String>(periodOptions());
    private final JComboBox<String> endPeriodBox = new JComboBox<String>(periodOptions());
    private final JComboBox<String> weekdayBox = new JComboBox<String>(WEEKDAYS);
    private final JTextField startWeekField = new JTextField();
    private final JTextField endWeekField = new JTextField();
    private final JTextField directionsField = new JTextField();
    private final JTextField majorsField = new JTextField();
    private final JTextField collegeField = new JTextField();
    private final JLabel statusLabel = new JLabel("  ");

    private ScheduleEntry current;
    private List<Teacher> teachers = new ArrayList<Teacher>();
    private List<Classroom> classrooms = new ArrayList<Classroom>();
    private List<Teacher> filteredTeachers = new ArrayList<Teacher>();
    private Runnable afterSave;
    private SaveListener saveListener;
    private boolean addMode;

    /** 创建离线编辑表单。 */
    public CourseEditFormPanel() {
        this(null);
    }

    /**
     * 创建接入课程服务的编辑表单；{@code api} 为 null 时仅预览。
     *
     * @param api 选课 API
     */
    public CourseEditFormPanel(CourseService api) {
        this.api = api;
        setLayout(new BorderLayout(0, 8));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(18, 18, 0, 18));

        JLabel title = new JLabel("课程编辑");
        title.setForeground(UiTheme.TEXT);
        title.setFont(UiTheme.font(Font.BOLD, 20F));
        add(title, BorderLayout.NORTH);

        add(buildForm(), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(0, 6));
        bottom.setOpaque(false);
        JButton saveButton = UiFactory.primaryButton("保存", "user");
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                save();
            }
        });
        bottom.add(saveButton, BorderLayout.NORTH);
        styleStatus();
        bottom.add(statusLabel, BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);

        bindTimeListeners();
    }

    private JPanel buildForm() {
        JPanel form = new JPanel(new GridLayout(0, 2, 4, 12));
        form.setOpaque(false);
        uuidLabel.setForeground(UiTheme.MUTED);
        styleField(nameField);
        styleField(codeField);
        styleField(startWeekField);
        styleField(endWeekField);
        styleField(directionsField);
        styleField(majorsField);
        styleField(collegeField);
        addRow(form, "uuid（不可改）", uuidLabel);
        addRow(form, "课程名", nameField);
        addRow(form, "课程编号", codeField);
        addRow(form, "容量(40-100)", capacitySpinner);
        addRow(form, "授课教师", teacherBox);
        addRow(form, "授课教室", classroomBox);
        addRow(form, "开始节次", startPeriodBox);
        addRow(form, "结束节次", endPeriodBox);
        addRow(form, "星期", weekdayBox);
        addRow(form, "起始周", startWeekField);
        addRow(form, "结束周", endWeekField);
        addRow(form, "研究方向(逗号分隔)", directionsField);
        addRow(form, "专业(逗号分隔)", majorsField);
        addRow(form, "学院", collegeField);
        return form;
    }

    private void styleField(JTextField field) {
        field.setBorder(new RoundedBorder(UiTheme.BORDER, 8));
        field.setPreferredSize(new java.awt.Dimension(220, 38));
        field.setFont(UiTheme.font(Font.PLAIN, 16F));
    }

    private void addRow(JPanel form, String text, java.awt.Component field) {
        JLabel label = new JLabel(text);
        label.setForeground(UiTheme.MUTED);
        label.setFont(UiTheme.font(Font.BOLD, 16F));
        label.setVerticalAlignment(javax.swing.SwingConstants.CENTER);
        form.add(label);
        form.add(field);
    }

    private static String[] periodOptions() {
        String[] options = new String[CourseScheduler.PERIODS + 1];
        options[0] = "（未排课）";
        for (int i = 0; i < CourseScheduler.PERIODS; i++) {
            options[i + 1] = CourseScheduler.periodName(i);
        }
        return options;
    }

    private void styleStatus() {
        statusLabel.setOpaque(true);
        statusLabel.setForeground(UiTheme.MUTED);
        statusLabel.setBackground(new Color(234, 241, 245));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    }

    private void bindTimeListeners() {
        directionsField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                repopulateTeacherBox(parseTags(directionsField.getText()));
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                repopulateTeacherBox(parseTags(directionsField.getText()));
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                repopulateTeacherBox(parseTags(directionsField.getText()));
            }
        });
    }

    /** @param afterSave 保存成功后的回调（通常用于刷新列表与网格）。 */
    public void setAfterSave(Runnable afterSave) {
        this.afterSave = afterSave;
    }

    /** @param saveListener 保存回调：表单校验通过后把请求交给宿主落地。 */
    public void setSaveListener(SaveListener saveListener) {
        this.saveListener = saveListener;
    }

    /** @param addMode 是否为「添加课程」模式（无 uuid、容量无下限）。 */
    public void setAddMode(boolean addMode) {
        this.addMode = addMode;
    }

    /** 进入「添加课程」模式：清空字段，uuid 显示为自动分配。 */
    public void renderNew(List<Teacher> teacherList, List<Classroom> roomList) {
        this.addMode = true;
        render(null, teacherList, roomList);
        uuidLabel.setText("（自动分配）");
        capacitySpinner.setValue(Integer.valueOf(40));
    }

    /**
     * 用选中的课程填充表单。
     *
     * @param entry 课程
     * @param teacherList 全部教师
     * @param roomList 全部教室
     */
    public void render(ScheduleEntry entry, List<Teacher> teacherList, List<Classroom> roomList) {
        this.current = entry;
        this.teachers = teacherList == null ? new ArrayList<Teacher>() : teacherList;
        this.classrooms = roomList == null ? new ArrayList<Classroom>() : roomList;
        if (entry == null) {
            uuidLabel.setText("--");
            nameField.setText("");
            codeField.setText("");
            capacitySpinner.setValue(Integer.valueOf(40));
            startPeriodBox.setSelectedIndex(0);
            endPeriodBox.setSelectedIndex(0);
            startWeekField.setText("");
            endWeekField.setText("");
            directionsField.setText("");
            majorsField.setText("");
            collegeField.setText("");
            populateClassroomBox(null);
            repopulateTeacherBox(new HashSet<Field>());
            return;
        }
        uuidLabel.setText(entry.getUuid() == null ? "--" : entry.getUuid());
        nameField.setText(entry.getCourseName() == null ? "" : entry.getCourseName());
        codeField.setText(entry.getCourseCode() == null ? "" : entry.getCourseCode());
        capacitySpinner.setValue(Integer.valueOf(Math.max(40, entry.getCapacity())));
        populateClassroomBox(entry.getClassroomUuid());
        if (entry.getTimeslot() != null) {
            Timeslot t = entry.getTimeslot();
            int[] range = CourseScheduler.periodRangeOf(t);
            if (range != null) {
                startPeriodBox.setSelectedIndex(range[0] + 1);
                endPeriodBox.setSelectedIndex(range[1] + 1);
            }
            weekdayBox.setSelectedIndex(t.getWeekday() - 1);
        } else {
            startPeriodBox.setSelectedIndex(0);
            endPeriodBox.setSelectedIndex(0);
            weekdayBox.setSelectedIndex(0);
        }
        directionsField.setText(fieldsToText(entry.getRequiredDirections()));
        majorsField.setText(fieldsToText(entry.getEligibleMajors()));
        collegeField.setText(entry.getCollegeUuid() == null ? "" : entry.getCollegeUuid());
        startWeekField.setText(entry.getStartWeek() == null ? "" : String.valueOf(entry.getStartWeek()));
        endWeekField.setText(entry.getEndWeek() == null ? "" : String.valueOf(entry.getEndWeek()));
        repopulateTeacherBox(parseTags(directionsField.getText()));
    }

    private void populateClassroomBox(String selectedUuid) {
        classroomBox.removeAllItems();
        classroomBox.addItem("（未安排）");
        for (Classroom room : classrooms) {
            classroomBox.addItem(room.getLocation() + room.getName());
        }
        selectClassroom(selectedUuid);
    }

    private void selectClassroom(String uuid) {
        if (uuid == null) {
            classroomBox.setSelectedIndex(0);
            return;
        }
        for (int i = 0; i < classrooms.size(); i++) {
            if (uuid.equals(classrooms.get(i).getUuid())) {
                classroomBox.setSelectedIndex(i + 1);
                return;
            }
        }
        classroomBox.setSelectedIndex(0);
    }

    private void repopulateTeacherBox(Set<Field> tags) {
        filteredTeachers.clear();
        for (Teacher teacher : teachers) {
            if (CourseRules.hasAllTags(teacher.getResearchDirections(), tags)) {
                filteredTeachers.add(teacher);
            }
        }
        teacherBox.removeAllItems();
        teacherBox.addItem("（未认领）");
        for (Teacher teacher : filteredTeachers) {
            teacherBox.addItem(teacher.getUuid());
        }
        selectTeacher(current == null ? null : current.getTeacherUuid());
    }

    private void selectTeacher(String uuid) {
        if (uuid == null) {
            teacherBox.setSelectedIndex(0);
            return;
        }
        for (int i = 0; i < filteredTeachers.size(); i++) {
            if (uuid.equals(filteredTeachers.get(i).getUuid())) {
                teacherBox.setSelectedIndex(i + 1);
                return;
            }
        }
        teacherBox.setSelectedIndex(0);
    }

    /**
     * 解析当前表单的排课时间：若已填开始与结束时间则用它们；否则退回指定节次的 45 分钟时间槽。
     *
     * @param weekday 星期
     * @param period 节次
     * @return 上课时间槽
     */
    public Timeslot resolveTimeslot(int weekday, int period) {
        int start = startPeriodBox.getSelectedIndex() - 1;
        int end = endPeriodBox.getSelectedIndex() - 1;
        if (start >= 0 && end >= start) {
            return CourseScheduler.timeslotOf(weekday, start, end);
        }
        return CourseScheduler.periodTimeslot(weekday, period);
    }

    private void save() {
        if (current == null && !addMode) {
            statusLabel.setText("  请先选择课程");
            return;
        }
        final CourseSaveRequest request = new CourseSaveRequest();
        if (current != null) {
            request.setUuid(current.getUuid());
        }
        request.setCode(codeField.getText().trim());
        request.setName(nameField.getText().trim());
        request.setCapacity((Integer) capacitySpinner.getValue());
        int teacherIndex = teacherBox.getSelectedIndex();
        request.setTeacherUuid(teacherIndex <= 0 ? "" : filteredTeachers.get(teacherIndex - 1).getUuid());
        int roomIndex = classroomBox.getSelectedIndex();
        request.setClassroomUuid(roomIndex <= 0 ? "" : classrooms.get(roomIndex - 1).getUuid());
        int startPeriod = startPeriodBox.getSelectedIndex() - 1;
        int endPeriod = endPeriodBox.getSelectedIndex() - 1;
        if (startPeriod >= 0 && endPeriod >= startPeriod) {
            request.setTimeslot(CourseScheduler.timeslotOf(weekdayBox.getSelectedIndex() + 1,
                    startPeriod, endPeriod));
        }
        request.setStartWeek(parseWeek(startWeekField.getText()));
        request.setEndWeek(parseWeek(endWeekField.getText()));
        request.setRequiredDirections(parseTags(directionsField.getText()));
        request.setEligibleMajors(parseTags(majorsField.getText()));
        request.setCollegeUuid(collegeField.getText().trim());
        if (saveListener != null) {
            saveListener.onSave(request);
            statusLabel.setText(addMode ? "  已添加（待应用到服务器）" : "  已保存（待应用到服务器）");
        }
    }

    /** 保存回调：表单校验通过后，把请求交给宿主（排课面板）落地。 */
    public interface SaveListener {
        void onSave(CourseSaveRequest request);
    }

    private static Set<Field> parseTags(String text) {
        Set<Field> tags = new HashSet<Field>();
        if (text != null) {
            for (String part : text.split("[,，;；]")) {
                String trimmed = part.trim();
                if (trimmed.length() > 0) {
                    tags.add(new Field(trimmed));
                }
            }
        }
        return tags;
    }

    private static Integer parseWeek(String text) {
        if (text == null) {
            return null;
        }
        try {
            int week = Integer.parseInt(text.trim());
            return week > 0 ? Integer.valueOf(week) : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static String fieldsToText(Set<Field> fields) {
        StringBuilder sb = new StringBuilder();
        if (fields != null) {
            for (Field field : fields) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(field.getName());
            }
        }
        return sb.toString();
    }
}
