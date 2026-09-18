package edu.seu.vcampus.client.course;

import edu.seu.vcampus.client.view.component.RoundedCellRenderer;

import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.client.view.UiTasks;
import edu.seu.vcampus.client.view.theme.UiTheme;
import edu.seu.vcampus.common.course.Classroom;
import edu.seu.vcampus.common.course.Course;
import edu.seu.vcampus.common.course.CourseScheduler;
import edu.seu.vcampus.common.course.Timeslot;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/**
 * 教师「我的课表」：以「星期×节次」网格展示本人授课课程的每周时间安排；同一时段多门课标红提示冲突。
 */
public class TeacherTimetablePanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final String[] COLUMNS = { "时间", "周一", "周二", "周三", "周四", "周五", "周六", "周日" };
    private static final Color[] BLOCK_COLORS = {
            new Color(181, 208, 236), new Color(186, 224, 202), new Color(246, 205, 160),
            new Color(216, 190, 236), new Color(250, 220, 148), new Color(174, 220, 220)
    };

    private final CourseService api;
    private final DefaultTableModel model = new DefaultTableModel(COLUMNS,
            CourseScheduler.PERIODS) {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private final JLabel statusLabel = new JLabel("  请登录后查看授课课表");

    private final List<Course> teaching = new ArrayList<Course>();
    private final Set<String> conflictCells = new HashSet<String>();
    private final Map<String, Color> cellColors = new HashMap<String, Color>();
    private final Map<String, String> classroomNames = new HashMap<String, String>();

    /** 创建离线预览界面。 */
    public TeacherTimetablePanel() {
        this(null);
    }

    /** 创建接入选课服务的界面；{@code api} 为 null 时仅离线预览。 */
    public TeacherTimetablePanel(CourseService api) {
        this.api = api;
        setLayout(new BorderLayout(0, 18));
        setBackground(UiTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(30, 34, 26, 34));
        add(heading(), BorderLayout.NORTH);
        JPanel center = new JPanel(new BorderLayout(0, 14));
        center.setOpaque(false);
        center.add(toolbar(), BorderLayout.NORTH);
        table.getTableHeader().setReorderingAllowed(false);
        table.setRowHeight(52);
        table.setDefaultRenderer(Object.class, new CellRenderer());
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER));
        scroll.getViewport().setBackground(UiTheme.SURFACE);
        center.add(scroll, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);
        styleStatus();
        add(statusLabel, BorderLayout.SOUTH);
        refresh();
    }

    /** @return 底部状态栏文本 */
    public String getStatusText() {
        return statusLabel.getText();
    }

    private JPanel heading() {
        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        JPanel text = new JPanel(new BorderLayout(0, 5));
        text.setOpaque(false);
        JLabel title = new JLabel("我的课表");
        title.setForeground(UiTheme.TEXT);
        title.setFont(UiTheme.font(Font.BOLD, UiTheme.SIZE_TITLE));
        JLabel subtitle = new JLabel("查看本人每周授课课程的时间安排，冲突时段标红");
        subtitle.setForeground(UiTheme.MUTED);
        subtitle.setFont(UiTheme.font(Font.PLAIN, UiTheme.SIZE_SUBTITLE));
        text.add(title, BorderLayout.NORTH);
        text.add(subtitle, BorderLayout.SOUTH);
        heading.add(text, BorderLayout.WEST);
        return heading;
    }

    private JPanel toolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        toolbar.setOpaque(false);
        JButton refreshButton = new JButton("刷新课表");
        refreshButton.setForeground(UiTheme.TEXT);
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
            statusLabel.setText("  请登录后查看授课课表");
            return;
        }
        UiTasks.run(new UiTasks.Task<Void>() {
            @Override
            public Void run() {
                loadClassrooms();
                List<Course> courses = api.listMyTeachingCourses();
                teaching.clear();
                if (courses != null) {
                    teaching.addAll(courses);
                }
                return null;
            }
        }, new UiTasks.Success<Void>() {
            @Override
            public void accept(Void result) {
                render();
            }
        }, UiTasks.failureWithDialog(this, "课表加载失败", statusLabel));
    }

    private void loadClassrooms() {
        classroomNames.clear();
        try {
            List<Classroom> rooms = api.listClassrooms();
            if (rooms != null) {
                for (Classroom room : rooms) {
                    classroomNames.put(room.getUuid(), room.getLocation() + room.getName());
                }
            }
        } catch (ApiException exception) {
            // 教室名只是辅助信息：取不到不该让整张课表打不开
        }
    }

    private void render() {
        conflictCells.clear();
        cellColors.clear();
        for (int row = 0; row < CourseScheduler.PERIODS; row++) {
            model.setValueAt(periodCell(row), row, 0);
            for (int col = 1; col <= CourseScheduler.WEEKDAYS; col++) {
                model.setValueAt("", row, col);
            }
        }
        Map<String, Integer> count = new HashMap<String, Integer>();
        for (Course course : teaching) {
            Timeslot t = course.getTimeslot();
            if (t == null) {
                continue;
            }
            int[] range = CourseScheduler.periodRangeOf(t);
            if (range == null) {
                continue;
            }
            for (int p = range[0]; p <= range[1]; p++) {
                String key = t.getWeekday() + "-" + p;
                Integer c = count.get(key);
                count.put(key, c == null ? Integer.valueOf(1) : Integer.valueOf(c.intValue() + 1));
            }
        }
        for (Course course : teaching) {
            Timeslot t = course.getTimeslot();
            if (t == null) {
                continue;
            }
            int[] range = CourseScheduler.periodRangeOf(t);
            if (range == null) {
                continue;
            }
            Color color = colorOf(course);
            for (int p = range[0]; p <= range[1]; p++) {
                String key = t.getWeekday() + "-" + p;
                if (count.get(key).intValue() > 1) {
                    conflictCells.add(key);
                }
                if (p == range[0]) {
                    model.setValueAt(blockText(course), p, t.getWeekday());
                }
                cellColors.put(key, color);
            }
        }
        statusLabel.setText("  共 " + teaching.size() + " 门授课课程"
                + (conflictCells.isEmpty() ? "" : "（存在时间冲突）"));
    }

    private Color colorOf(Course course) {
        String key = course.getCode() == null ? course.getName() : course.getCode();
        int hash = key == null ? 0 : Math.abs(key.hashCode());
        return BLOCK_COLORS[hash % BLOCK_COLORS.length];
    }

    private String blockText(Course course) {
        Timeslot t = course.getTimeslot();
        String name = course.getName() == null ? course.getCode() : course.getName();
        StringBuilder sb = new StringBuilder("<html><center>");
        sb.append(name);
        sb.append("<br><font color='#405060' size='2'>");
        sb.append(CourseScheduler.weekdayName(t.getWeekday())).append(" ");
        sb.append(CourseScheduler.periodRangeText(t));
        int duration = t.getEndMinute() - t.getStartMinute();
        if (duration != 45) {
            sb.append(" ").append(duration).append("分钟");
        }
        if (course.getStartWeek() != null && course.getEndWeek() != null) {
            sb.append(" ").append(course.getStartWeek()).append("-")
                    .append(course.getEndWeek()).append("周");
        }
        String room = classroomNames.get(course.getClassroomUuid());
        if (room != null) {
            sb.append("<br>").append(room);
        }
        sb.append("</font></center></html>");
        return sb.toString();
    }

    private static String periodCell(int period) {
        return "<html><center>" + CourseScheduler.periodName(period)
                + "<br><font color='#687B8A' size='2'>" + CourseScheduler.periodTimeRange(period)
                + "</font></center></html>";
    }

    private class CellRenderer extends RoundedCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus,
                    row, column);
            setHorizontalAlignment(CENTER);
            if (column == 0) {
                c.setBackground(UiTheme.SURFACE);
                c.setForeground(UiTheme.MUTED);
                c.setFont(UiTheme.font(Font.BOLD, UiTheme.SIZE_SMALL));
                return c;
            }
            c.setFont(UiTheme.font(Font.PLAIN, UiTheme.SIZE_CELL));
            String key = column + "-" + row;
            if (conflictCells.contains(key)) {
                c.setBackground(new Color(255, 214, 214));
                c.setForeground(new Color(180, 0, 0));
            } else {
                Color color = cellColors.get(key);
                c.setBackground(color == null ? UiTheme.SURFACE : color);
                c.setForeground(UiTheme.TEXT);
            }
            return c;
        }
    }

    private void styleStatus() {
        statusLabel.setOpaque(true);
        statusLabel.setForeground(UiTheme.MUTED);
        statusLabel.setBackground(new Color(234, 241, 245));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(9, 10, 9, 10));
    }
}
