package edu.seu.vcampus.client.course;

import edu.seu.vcampus.client.view.UiTasks;
import edu.seu.vcampus.client.view.theme.UiFactory;
import edu.seu.vcampus.client.view.theme.UiTheme;
import edu.seu.vcampus.common.course.Classroom;
import edu.seu.vcampus.common.course.Course;
import edu.seu.vcampus.common.course.Timeslot;
import edu.seu.vcampus.common.course.dto.CourseScheduleRequest;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * 管理员「排课」界面：把课程安排到教室的某个时间槽。学生与教师不具备该能力。
 */
public class SchedulePanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final String[] WEEKDAYS = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};

    private final CourseService api;
    private final JComboBox<String> courseBox = new JComboBox<String>();
    private final JComboBox<String> classroomBox = new JComboBox<String>();
    private final JComboBox<String> weekdayBox = new JComboBox<String>(WEEKDAYS);
    private final JTextField startField = new JTextField(3);
    private final JTextField endField = new JTextField(3);
    private final JLabel statusLabel = new JLabel("  请登录后使用排课功能");
    private final List<Course> courses = new ArrayList<Course>();
    private final List<Classroom> classrooms = new ArrayList<Classroom>();

    /** 创建离线预览界面。 */
    public SchedulePanel() {
        this(null);
    }

    /**
     * 创建接入排课服务的界面；{@code api} 为 null 时仅离线预览。
     *
     * @param api 选课 API
     */
    public SchedulePanel(CourseService api) {
        this.api = api;
        setLayout(new BorderLayout(0, 18));
        setBackground(UiTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(30, 34, 26, 34));
        add(heading(), BorderLayout.NORTH);
        add(toolbar(), BorderLayout.CENTER);
        styleStatus();
        add(statusLabel, BorderLayout.SOUTH);
        refresh();
    }

    private JPanel heading() {
        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        JLabel title = new JLabel("排课");
        title.setForeground(UiTheme.TEXT);
        title.setFont(UiTheme.font(Font.BOLD, 28F));
        heading.add(title, BorderLayout.WEST);
        return heading;
    }

    private JPanel toolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        toolbar.setOpaque(false);
        toolbar.add(label("课程"));
        toolbar.add(courseBox);
        toolbar.add(label("教室"));
        toolbar.add(classroomBox);
        toolbar.add(label("星期"));
        toolbar.add(weekdayBox);
        toolbar.add(label("开始(时)"));
        toolbar.add(startField);
        toolbar.add(label("结束(时)"));
        toolbar.add(endField);
        JButton scheduleButton = UiFactory.primaryButton("排课", "user");
        scheduleButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                schedule();
            }
        });
        toolbar.add(scheduleButton);
        JButton refreshButton = UiFactory.secondaryButton("刷新", "refresh");
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                refresh();
            }
        });
        toolbar.add(refreshButton);
        return toolbar;
    }

    private void refresh() {
        if (api == null) {
            statusLabel.setText("  请登录后使用排课功能");
            return;
        }
        UiTasks.run(new UiTasks.Task<Void>() {
            @Override
            public Void run() {
                courses.clear();
                courses.addAll(api.listCourses());
                classrooms.clear();
                classrooms.addAll(api.listClassrooms());
                return null;
            }
        }, new UiTasks.Success<Void>() {
            @Override
            public void accept(Void result) {
                renderCourses();
                renderClassrooms();
                statusLabel.setText("  已载入 " + courses.size() + " 门课程、"
                        + classrooms.size() + " 间教室");
            }
        });
    }

    private void renderCourses() {
        courseBox.removeAllItems();
        for (Course course : courses) {
            courseBox.addItem(course.getCode() + " " + course.getName());
        }
    }

    private void renderClassrooms() {
        classroomBox.removeAllItems();
        for (Classroom room : classrooms) {
            classroomBox.addItem(room.getLocation() + "（容量 " + room.getCapacity() + "）");
        }
    }

    private void schedule() {
        int row = courseBox.getSelectedIndex();
        if (row < 0 || row >= courses.size()
                || classroomBox.getSelectedIndex() < 0) {
            statusLabel.setText("  请先选择课程与教室");
            return;
        }
        try {
            CourseScheduleRequest request = new CourseScheduleRequest();
            request.setCourseCode(courses.get(row).getCode());
            request.setClassroomUuid(classrooms.get(classroomBox.getSelectedIndex()).getUuid());
            int weekday = weekdayBox.getSelectedIndex() + 1;
            int start = Integer.parseInt(startField.getText().trim());
            int end = Integer.parseInt(endField.getText().trim());
            List<Timeslot> timeslots = new ArrayList<Timeslot>();
            timeslots.add(new Timeslot(weekday, start * 60, end * 60));
            request.setTimeslots(timeslots);
            submit(request);
        } catch (RuntimeException exception) {
            statusLabel.setText("  时间格式不正确，请按 0-24 小时填写");
        }
    }

    private void submit(final CourseScheduleRequest request) {
        UiTasks.run(new UiTasks.Task<Void>() {
            @Override
            public Void run() {
                api.scheduleCourse(request);
                return null;
            }
        }, new UiTasks.Success<Void>() {
            @Override
            public void accept(Void result) {
                statusLabel.setText("  排课成功");
                refresh();
            }
        });
    }

    private JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(UiTheme.MUTED);
        label.setFont(UiTheme.font(Font.BOLD, 13F));
        return label;
    }

    private void styleStatus() {
        statusLabel.setOpaque(true);
        statusLabel.setForeground(UiTheme.MUTED);
        statusLabel.setBackground(new Color(234, 241, 245));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(9, 10, 9, 10));
    }
}
