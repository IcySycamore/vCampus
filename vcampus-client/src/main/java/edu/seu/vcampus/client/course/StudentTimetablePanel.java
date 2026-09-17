package edu.seu.vcampus.client.course;

import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.client.view.UiTasks;
import edu.seu.vcampus.client.view.theme.UiTheme;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * 学生「我的课表」：以「星期×节次」网格展示本人已选课程；同一时段出现多门课时标红提示冲突。
 */
public class StudentTimetablePanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final String[] COLUMNS = {"时间", "周一", "周二", "周三", "周四", "周五", "周六", "周日"};

    private final CourseService api;
    private final DefaultTableModel model = new DefaultTableModel(COLUMNS, CourseScheduler.PERIODS) {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private final JLabel statusLabel = new JLabel("  请登录后查看课表");

    private final List<Course> selections = new ArrayList<Course>();
    private final Set<String> conflictCells = new HashSet<String>();

    /** 创建离线预览界面。 */
    public StudentTimetablePanel() {
        this(null);
    }

    /** 创建接入选课服务的界面；{@code api} 为 null 时仅离线预览。 */
    public StudentTimetablePanel(CourseService api) {
        this.api = api;
        setLayout(new BorderLayout(0, 18));
        setBackground(UiTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(30, 34, 26, 34));
        add(heading(), BorderLayout.NORTH);
        JPanel center = new JPanel(new BorderLayout(0, 14));
        center.setOpaque(false);
        center.add(toolbar(), BorderLayout.NORTH);
        table.getTableHeader().setReorderingAllowed(false);
        table.setRowHeight(40);
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

    private JPanel heading() {
        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        JPanel text = new JPanel(new BorderLayout(0, 5));
        text.setOpaque(false);
        JLabel title = new JLabel("我的课表");
        title.setForeground(UiTheme.TEXT);
        title.setFont(UiTheme.font(Font.BOLD, 28F));
        JLabel subtitle = new JLabel("查看本人已选课程的时间安排，冲突时段标红");
        subtitle.setForeground(UiTheme.MUTED);
        subtitle.setFont(UiTheme.font(Font.PLAIN, 15F));
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
            statusLabel.setText("  请登录后查看课表");
            return;
        }
        UiTasks.run(new UiTasks.Task<List<Course>>() {
            @Override
            public List<Course> run() {
                return api.listMySelections();
            }
        }, new UiTasks.Success<List<Course>>() {
            @Override
            public void accept(List<Course> courses) {
                selections.clear();
                if (courses != null) {
                    selections.addAll(courses);
                }
                render();
            }
        }, new UiTasks.Failure() {
            @Override
            public void accept(ApiException error) {
                statusLabel.setText("  " + error.getMessage());
            }
        });
    }

    private void render() {
        conflictCells.clear();
        for (int row = 0; row < CourseScheduler.PERIODS; row++) {
            model.setValueAt(CourseScheduler.periodName(row), row, 0);
            for (int col = 1; col <= CourseScheduler.WEEKDAYS; col++) {
                model.setValueAt("", row, col);
            }
        }
        for (Course course : selections) {
            Timeslot t = course.getTimeslot();
            if (t == null) {
                continue;
            }
            String name = course.getName() == null ? course.getCode() : course.getName();
            for (int period = 0; period < CourseScheduler.PERIODS; period++) {
                if (CourseScheduler.periodTimeslot(t.getWeekday(), period).overlaps(t)) {
                    Object existing = model.getValueAt(period, t.getWeekday());
                    String key = t.getWeekday() + "-" + period;
                    if (existing != null && !"".equals(existing.toString())) {
                        model.setValueAt(existing + " / " + name, period, t.getWeekday());
                        conflictCells.add(key);
                    } else {
                        model.setValueAt(name, period, t.getWeekday());
                    }
                }
            }
        }
        statusLabel.setText("  已选 " + selections.size() + " 门课程"
                + (conflictCells.isEmpty() ? "" : "（存在时间冲突）"));
    }

    private class CellRenderer extends DefaultTableCellRenderer {
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
                c.setFont(UiTheme.font(Font.BOLD, 13F));
                return c;
            }
            c.setFont(UiTheme.font(Font.PLAIN, 12F));
            if (conflictCells.contains(column + "-" + row)) {
                c.setBackground(new Color(255, 214, 214));
                c.setForeground(new Color(180, 0, 0));
            } else {
                c.setBackground(UiTheme.SURFACE);
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
