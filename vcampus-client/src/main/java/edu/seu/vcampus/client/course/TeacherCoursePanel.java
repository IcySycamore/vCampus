package edu.seu.vcampus.client.course;

import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.client.view.UiTasks;
import edu.seu.vcampus.client.view.theme.UiFactory;
import edu.seu.vcampus.client.view.theme.UiTheme;
import edu.seu.vcampus.common.course.Course;

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
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/**
 * 教师「我的课程」界面：展示本人认领的课程及其排课与选课情况。
 */
public class TeacherCoursePanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final String[] COLUMNS = {"课程编号", "课程名称", "学分", "容量", "已选"};

    private final CourseService api;
    private final DefaultTableModel model = new DefaultTableModel(COLUMNS, 0) {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JLabel statusLabel = new JLabel("  请登录后查看授课课程");

    /**
     * 创建离线预览界面。
     */
    public TeacherCoursePanel() {
        this(null);
    }

    /**
     * 创建接入选课服务的界面；{@code api} 为 null 时仅离线预览。
     *
     * @param api 选课 API
     */
    public TeacherCoursePanel(CourseService api) {
        this.api = api;
        setLayout(new BorderLayout(0, 18));
        setBackground(UiTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(30, 34, 26, 34));
        add(heading(), BorderLayout.NORTH);
        JTable table = new JTable(model);
        UiFactory.styleTable(table);
        JPanel center = new JPanel(new BorderLayout(0, 14));
        center.setOpaque(false);
        center.add(toolbar(), BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER));
        scroll.getViewport().setBackground(UiTheme.SURFACE);
        center.add(scroll, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);
        styleStatus();
        add(statusLabel, BorderLayout.SOUTH);
        refresh();
    }

    /** @return 当前课程表格行数 */
    public int getCourseCount() {
        return model.getRowCount();
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
        JLabel title = new JLabel("我的课程");
        title.setForeground(UiTheme.TEXT);
        title.setFont(UiTheme.font(Font.BOLD, 28F));
        JLabel subtitle = new JLabel("查看本人授课课程及其选课情况");
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
        JButton claimButton = UiFactory.primaryButton("认领课程", "user");
        claimButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                openClaimDialog();
            }
        });
        toolbar.add(claimButton);
        JButton refreshButton = UiFactory.secondaryButton("刷新课程", "refresh");
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
            statusLabel.setText("  请登录后查看授课课程");
            return;
        }
        UiTasks.run(new UiTasks.Task<List<Course>>() {
            @Override
            public List<Course> run() {
                return api.listMyTeachingCourses();
            }
        }, new UiTasks.Success<List<Course>>() {
            @Override
            public void accept(List<Course> courses) {
                render(courses);
            }
        }, new UiTasks.Failure() {
            @Override
            public void accept(ApiException error) {
                statusLabel.setText("  " + error.getMessage());
            }
        });
    }

    private void openClaimDialog() {
        if (api == null) {
            statusLabel.setText("  服务器未连接，当前仅可预览界面");
            return;
        }
        UiTasks.run(new UiTasks.Task<List<Course>>() {
            @Override
            public List<Course> run() {
                return api.listCourses();
            }
        }, new UiTasks.Success<List<Course>>() {
            @Override
            public void accept(List<Course> courses) {
                showClaimPicker(courses);
            }
        }, new UiTasks.Failure() {
            @Override
            public void accept(ApiException error) {
                statusLabel.setText("  " + error.getMessage());
            }
        });
    }

    private void showClaimPicker(List<Course> courses) {
        List<Course> unclaimed = new ArrayList<Course>();
        if (courses != null) {
            for (Course course : courses) {
                if (course.getTeacherUuid() == null) {
                    unclaimed.add(course);
                }
            }
        }
        if (unclaimed.isEmpty()) {
            statusLabel.setText("  没有可认领的课程");
            return;
        }
        final JComboBox<String> box = new JComboBox<String>();
        for (Course course : unclaimed) {
            box.addItem(course.getCode() + " " + course.getName());
        }
        int result = JOptionPane.showConfirmDialog(this, box, "认领课程",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION || box.getSelectedIndex() < 0) {
            return;
        }
        submitClaim(unclaimed.get(box.getSelectedIndex()).getCode());
    }

    private void submitClaim(final String courseCode) {
        UiTasks.run(new UiTasks.Task<Void>() {
            @Override
            public Void run() {
                api.claimCourse(courseCode);
                return null;
            }
        }, new UiTasks.Success<Void>() {
            @Override
            public void accept(Void result) {
                statusLabel.setText("  课程已认领");
                refresh();
            }
        }, new UiTasks.Failure() {
            @Override
            public void accept(ApiException error) {
                statusLabel.setText("  " + error.getMessage());
            }
        });
    }

    private void render(List<Course> courses) {
        model.setRowCount(0);
        if (courses != null) {
            for (Course course : courses) {
                model.addRow(rowOf(course));
            }
        }
        statusLabel.setText("  共 " + model.getRowCount() + " 门授课课程");
    }

    private Object[] rowOf(Course course) {
        return new Object[] {
            course.getCode(), course.getName(), Integer.valueOf(course.getCredit()),
            Integer.valueOf(course.getCapacity()),
            Integer.valueOf(course.getEnrolled())
        };
    }

    private void styleStatus() {
        statusLabel.setOpaque(true);
        statusLabel.setForeground(UiTheme.MUTED);
        statusLabel.setBackground(new Color(234, 241, 245));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(9, 10, 9, 10));
    }
}
