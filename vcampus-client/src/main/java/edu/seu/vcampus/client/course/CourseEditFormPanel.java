package edu.seu.vcampus.client.course;

import edu.seu.vcampus.client.view.theme.UiFactory;
import edu.seu.vcampus.client.view.theme.UiTheme;
import edu.seu.vcampus.common.course.Classroom;
import edu.seu.vcampus.common.course.CourseRules;
import edu.seu.vcampus.common.course.CourseScheduler;
import edu.seu.vcampus.common.course.Field;
import edu.seu.vcampus.common.course.ScheduleEntry;
import edu.seu.vcampus.common.course.ScheduleTimeCalculator;
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
import javax.swing.JTextField;
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
    private final JTextField capacityField = new JTextField();
    private final JComboBox<String> teacherBox = new JComboBox<String>();
    private final JComboBox<String> classroomBox = new JComboBox<String>();
    private final JTextField startHourField = new JTextField(2);
    private final JTextField startMinuteField = new JTextField(2);
    private final JTextField endHourField = new JTextField(2);
    private final JTextField endMinuteField = new JTextField(2);
    private final JTextField durationField = new JTextField();
    private final JComboBox<String> weekdayBox = new JComboBox<String>(WEEKDAYS);
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
    private boolean updating;
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
        setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));

        JLabel title = new JLabel("课程编辑");
        title.setForeground(UiTheme.TEXT);
        title.setFont(UiTheme.font(Font.BOLD, 16F));
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
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.setOpaque(false);
        uuidLabel.setForeground(UiTheme.MUTED);
        addRow(form, "uuid（不可改）", uuidLabel);
        addRow(form, "课程名", nameField);
        addRow(form, "课程编号", codeField);
        addRow(form, "容量（只增）", capacityField);
        addRow(form, "授课教师", teacherBox);
        addRow(form, "授课教室", classroomBox);
        addRow(form, "开始时间", timePanel(startHourField, startMinuteField));
        addRow(form, "结束时间", timePanel(endHourField, endMinuteField));
        addRow(form, "持续时长(分钟)", durationField);
        addRow(form, "星期", weekdayBox);
        addRow(form, "研究方向(逗号分隔)", directionsField);
        addRow(form, "专业(逗号分隔)", majorsField);
        addRow(form, "学院", collegeField);
        return form;
    }

    private void addRow(JPanel form, String text, java.awt.Component field) {
        JLabel label = new JLabel(text);
        label.setForeground(UiTheme.MUTED);
        label.setFont(UiTheme.font(Font.BOLD, 13F));
        form.add(label);
        form.add(field);
    }

    private JPanel timePanel(JTextField hourField, JTextField minuteField) {
        JPanel panel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 4, 0));
        panel.setOpaque(false);
        panel.add(hourField);
        JLabel colon = new JLabel(":");
        colon.setForeground(UiTheme.MUTED);
        panel.add(colon);
        panel.add(minuteField);
        return panel;
    }

    private void styleStatus() {
        statusLabel.setOpaque(true);
        statusLabel.setForeground(UiTheme.MUTED);
        statusLabel.setBackground(new Color(234, 241, 245));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    }

    private void bindTimeListeners() {
        startHourField.getDocument().addDocumentListener(new TimeDocumentListener("start"));
        startMinuteField.getDocument().addDocumentListener(new TimeDocumentListener("start"));
        endHourField.getDocument().addDocumentListener(new TimeDocumentListener("end"));
        endMinuteField.getDocument().addDocumentListener(new TimeDocumentListener("end"));
        durationField.getDocument().addDocumentListener(new TimeDocumentListener("duration"));
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

    private class TimeDocumentListener implements DocumentListener {
        private final String source;

        TimeDocumentListener(String source) {
            this.source = source;
        }

        @Override
        public void insertUpdate(DocumentEvent event) {
            recomputeTime(source);
        }

        @Override
        public void removeUpdate(DocumentEvent event) {
            recomputeTime(source);
        }

        @Override
        public void changedUpdate(DocumentEvent event) {
            recomputeTime(source);
        }
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
        capacityField.setText("40");
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
            capacityField.setText("");
            startHourField.setText("");
            startMinuteField.setText("");
            endHourField.setText("");
            endMinuteField.setText("");
            durationField.setText("");
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
        capacityField.setText(String.valueOf(entry.getCapacity()));
        populateClassroomBox(entry.getClassroomUuid());
        if (entry.getTimeslot() != null) {
            Timeslot t = entry.getTimeslot();
            setTime(startHourField, startMinuteField, t.getStartMinute());
            setTime(endHourField, endMinuteField, t.getEndMinute());
            durationField.setText(String.valueOf(t.getEndMinute() - t.getStartMinute()));
            weekdayBox.setSelectedIndex(t.getWeekday() - 1);
        } else {
            startHourField.setText("");
            startMinuteField.setText("");
            endHourField.setText("");
            endMinuteField.setText("");
            durationField.setText("");
            weekdayBox.setSelectedIndex(0);
        }
        directionsField.setText(fieldsToText(entry.getRequiredDirections()));
        majorsField.setText(fieldsToText(entry.getEligibleMajors()));
        collegeField.setText(entry.getCollegeUuid() == null ? "" : entry.getCollegeUuid());
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
        int start = timeOf(startHourField, startMinuteField);
        int end = timeOf(endHourField, endMinuteField);
        if (start >= 0 && end > start) {
            return new Timeslot(weekday, start, end);
        }
        return CourseScheduler.periodTimeslot(weekday, period);
    }

    private void recomputeTime(String source) {
        if (updating) {
            return;
        }
        updating = true;
        try {
            int start = timeOf(startHourField, startMinuteField);
            if (start < 0) {
                if (!"start".equals(source)) {
                    statusLabel.setText("  请先填写开始时间");
                }
                return;
            }
            if ("start".equals(source) || "duration".equals(source)) {
                int duration = parseDuration(durationField.getText().trim());
                if (duration > 0) {
                    Integer end = ScheduleTimeCalculator.endOf(start, duration);
                    if (end != null) {
                        setTime(endHourField, endMinuteField, end.intValue());
                        return;
                    }
                }
            }
            if ("start".equals(source) || "end".equals(source)) {
                int end = timeOf(endHourField, endMinuteField);
                if (end > 0) {
                    Integer duration = ScheduleTimeCalculator.durationOf(start, end);
                    if (duration != null) {
                        durationField.setText(String.valueOf(duration.intValue()));
                    } else {
                        statusLabel.setText("  结束时间必须晚于开始时间");
                    }
                }
            }
        } finally {
            updating = false;
        }
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
        try {
            int capacity = Integer.parseInt(capacityField.getText().trim());
            if (current != null && capacity < current.getCapacity()) {
                statusLabel.setText("  容量只增不减（当前 " + current.getCapacity() + "）");
                return;
            }
            request.setCapacity(Integer.valueOf(capacity));
        } catch (NumberFormatException exception) {
            statusLabel.setText("  容量必须为整数");
            return;
        }
        int teacherIndex = teacherBox.getSelectedIndex();
        request.setTeacherUuid(teacherIndex <= 0 ? "" : filteredTeachers.get(teacherIndex - 1).getUuid());
        int roomIndex = classroomBox.getSelectedIndex();
        request.setClassroomUuid(roomIndex <= 0 ? "" : classrooms.get(roomIndex - 1).getUuid());
        int start = timeOf(startHourField, startMinuteField);
        int end = timeOf(endHourField, endMinuteField);
        if (start >= 0 && end >= 0) {
            request.setTimeslot(new Timeslot(weekdayBox.getSelectedIndex() + 1, start, end));
        }
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

    private static int parseDuration(String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException exception) {
            return -1;
        }
    }

    private static int timeOf(JTextField hourField, JTextField minuteField) {
        try {
            int hour = Integer.parseInt(hourField.getText().trim());
            int minute = Integer.parseInt(minuteField.getText().trim());
            if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
                return -1;
            }
            return hour * 60 + minute;
        } catch (NumberFormatException exception) {
            return -1;
        }
    }

    private static void setTime(JTextField hourField, JTextField minuteField, int minuteOfDay) {
        hourField.setText(String.valueOf(minuteOfDay / 60));
        minuteField.setText(String.valueOf(minuteOfDay % 60));
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
