package edu.seu.vcampus.client.course;

import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.client.view.UiTasks;
import edu.seu.vcampus.client.view.component.PageBarPanel;
import edu.seu.vcampus.client.view.theme.UiFactory;
import edu.seu.vcampus.client.view.theme.UiTheme;
import edu.seu.vcampus.common.course.Course;
import edu.seu.vcampus.common.course.dto.CourseSaveRequest;
import edu.seu.vcampus.common.message.PageResponse;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

/**
 * 管理员「课程管理」界面：课程表格 + 表头排序 + 分页 + 关键字筛选，支持添加 / 修改 / 删除课程。
 *
 * <p>排序与分页是纯函数（{@link CourseSorter} / {@link CoursePaging}），本类只做界面组合，
 * 增删改规则由服务端 {@code CourseManagementService} 最终把关。
 */
public class CourseAdminPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final String[] COLUMNS = {"课程编号", "课程名称", "学分", "学期", "授课教师", "容量", "已选"};
    private static final int PAGE_SIZE = 5;

    private final CourseService api;
    private final DefaultTableModel model = new DefaultTableModel(COLUMNS, 0) {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private final JTextField keywordField = new JTextField(16);
    private final JLabel statusLabel = new JLabel("  请登录后使用课程管理");
    private final PageBarPanel pager;

    private final List<Course> allCourses = new ArrayList<Course>();
    private int sortColumn = CourseSorter.COL_CODE;
    private boolean ascending = true;

    /** 创建离线预览界面。 */
    public CourseAdminPanel() {
        this(null);
    }

    /**
     * 创建接入课程服务的界面；{@code api} 为 null 时仅离线预览。
     *
     * @param api 选课 API
     */
    public CourseAdminPanel(CourseService api) {
        this.api = api;
        this.pager = new PageBarPanel(new Runnable() {
            @Override
            public void run() {
                renderPage();
            }
        }, PAGE_SIZE);

        setLayout(new BorderLayout(0, 18));
        setBackground(UiTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(30, 34, 26, 34));
        add(heading(), BorderLayout.NORTH);
        add(center(), BorderLayout.CENTER);
        styleStatus();
        add(statusLabel, BorderLayout.SOUTH);

        table.getTableHeader().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                int column = table.columnAtPoint(event.getPoint());
                if (column < 0) {
                    return;
                }
                if (sortColumn == column) {
                    ascending = !ascending;
                } else {
                    sortColumn = column;
                    ascending = true;
                }
                pager.resetToFirstPage();
                renderPage();
            }
        });

        refresh();
    }

    private JPanel heading() {
        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        JPanel text = new JPanel(new BorderLayout(0, 5));
        text.setOpaque(false);
        JLabel title = new JLabel("课程管理");
        title.setForeground(UiTheme.TEXT);
        title.setFont(UiTheme.font(Font.BOLD, 28F));
        JLabel subtitle = new JLabel("维护课程目录：支持排序、分页、筛选与增删改");
        subtitle.setForeground(UiTheme.MUTED);
        subtitle.setFont(UiTheme.font(Font.PLAIN, 15F));
        text.add(title, BorderLayout.NORTH);
        text.add(subtitle, BorderLayout.SOUTH);
        heading.add(text, BorderLayout.WEST);
        return heading;
    }

    private JPanel center() {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setOpaque(false);
        panel.add(toolbar(), BorderLayout.NORTH);
        UiFactory.styleTable(table);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER));
        scroll.getViewport().setBackground(UiTheme.SURFACE);
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(pager, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel toolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        toolbar.setOpaque(false);
        toolbar.add(label("关键词"));
        keywordField.setPreferredSize(new java.awt.Dimension(180, 38));
        keywordField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiTheme.BORDER),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        toolbar.add(keywordField);
        JButton searchButton = UiFactory.primaryButton("搜索", "search");
        searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                pager.resetToFirstPage();
                renderPage();
            }
        });
        toolbar.add(searchButton);
        JButton addButton = UiFactory.primaryButton("添加课程", "user");
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                showAddDialog();
            }
        });
        toolbar.add(addButton);
        JButton editButton = UiFactory.secondaryButton("修改课程", "edit");
        editButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                Course selected = selectedCourse();
                if (selected == null) {
                    statusLabel.setText("  请先选择一门课程");
                    return;
                }
                showEditDialog(selected);
            }
        });
        toolbar.add(editButton);
        JButton deleteButton = UiFactory.secondaryButton("删除课程", "return");
        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                Course selected = selectedCourse();
                if (selected == null) {
                    statusLabel.setText("  请先选择一门课程");
                    return;
                }
                confirmDelete(selected);
            }
        });
        toolbar.add(deleteButton);
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
            statusLabel.setText("  请登录后使用课程管理");
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
                allCourses.clear();
                if (courses != null) {
                    allCourses.addAll(courses);
                }
                pager.resetToFirstPage();
                renderPage();
            }
        }, new UiTasks.Failure() {
            @Override
            public void accept(ApiException error) {
                statusLabel.setText("  " + error.getMessage());
            }
        });
    }

    private void renderPage() {
        List<Course> filtered = CourseFilters.filter(allCourses, keywordField.getText());
        List<Course> sorted = CourseSorter.sort(filtered, sortColumn, ascending);
        PageResponse<Course> page = CoursePaging.page(sorted, pager.getPageNumber(), PAGE_SIZE);
        render(page.getItems());
        pager.sync(page);
        statusLabel.setText("  共 " + page.getTotal() + " 门课程");
    }

    private void render(List<Course> courses) {
        model.setRowCount(0);
        if (courses == null) {
            return;
        }
        for (Course course : courses) {
            model.addRow(rowOf(course));
        }
    }

    private Object[] rowOf(Course course) {
        return new Object[] {
            course.getCode(),
            course.getName(),
            Integer.valueOf(course.getCredit()),
            course.getSemester() == null ? "--" : course.getSemester(),
            course.getTeacherUuid() == null ? "未认领" : course.getTeacherUuid(),
            Integer.valueOf(course.getCapacity()),
            Integer.valueOf(course.getEnrolled())
        };
    }

    private Course selectedCourse() {
        int row = table.getSelectedRow();
        if (row < 0) {
            return null;
        }
        String code = String.valueOf(model.getValueAt(row, 0));
        for (Course course : allCourses) {
            if (code.equals(course.getCode())) {
                return course;
            }
        }
        return null;
    }

    private void showAddDialog() {
        final JTextField code = new JTextField();
        final JTextField name = new JTextField();
        final JTextField credit = new JTextField("3");
        final JTextField capacity = new JTextField("40");
        final JTextField semester = new JTextField("2026-2027-1");
        final JTextField teacher = new JTextField();

        JPanel form = fieldForm(new String[] {"课程编号", "课程名称", "学分", "容量", "学期", "授课教师uuid（可空）"},
                new JComponent[] {code, name, credit, capacity, semester, teacher});
        int result = JOptionPane.showConfirmDialog(this, form, "添加课程",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }
        CourseSaveRequest request = new CourseSaveRequest();
        request.setCode(code.getText().trim());
        request.setName(name.getText().trim());
        request.setTeacherUuid(teacher.getText().trim());
        try {
            request.setCredit(Integer.valueOf(credit.getText().trim()));
            request.setCapacity(Integer.valueOf(capacity.getText().trim()));
        } catch (NumberFormatException exception) {
            statusLabel.setText("  学分和容量必须为整数");
            return;
        }
        request.setSemester(semester.getText().trim());
        submitAdd(request);
    }

    private void showEditDialog(Course course) {
        final JLabel codeLabel = new JLabel(course.getCode());
        final JTextField name = new JTextField(course.getName());
        final JTextField teacher = new JTextField(course.getTeacherUuid() == null
                ? "" : course.getTeacherUuid());
        final JTextField capacity = new JTextField(String.valueOf(course.getCapacity()));

        JPanel form = fieldForm(new String[] {"课程编号（不可改）", "课程名称", "授课教师uuid（留空=取消认领）", "容量（只增不减）"},
                new JComponent[] {codeLabel, name, teacher, capacity});
        int result = JOptionPane.showConfirmDialog(this, form, "修改课程",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }
        CourseSaveRequest request = new CourseSaveRequest();
        request.setCode(course.getCode());
        request.setName(name.getText().trim());
        request.setTeacherUuid(teacher.getText().trim());
        try {
            int newCapacity = Integer.parseInt(capacity.getText().trim());
            if (newCapacity < course.getCapacity()) {
                statusLabel.setText("  容量只增不减（当前 " + course.getCapacity() + "）");
                return;
            }
            request.setCapacity(Integer.valueOf(newCapacity));
        } catch (NumberFormatException exception) {
            statusLabel.setText("  容量必须为整数");
            return;
        }
        submitUpdate(request);
    }

    private void confirmDelete(final Course course) {
        int result = JOptionPane.showConfirmDialog(this,
                "确认删除课程「" + course.getCode() + " " + course.getName() + "」？",
                "删除课程", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }
        submitDelete(course.getCode());
    }

    private void submitAdd(final CourseSaveRequest request) {
        if (api == null) {
            statusLabel.setText("  服务器未连接，当前仅可预览界面");
            return;
        }
        UiTasks.run(new UiTasks.Task<Void>() {
            @Override
            public Void run() {
                api.addCourse(request);
                return null;
            }
        }, new UiTasks.Success<Void>() {
            @Override
            public void accept(Void result) {
                statusLabel.setText("  课程已添加");
                refresh();
            }
        }, new UiTasks.Failure() {
            @Override
            public void accept(ApiException error) {
                statusLabel.setText("  " + error.getMessage());
            }
        });
    }

    private void submitUpdate(final CourseSaveRequest request) {
        if (api == null) {
            statusLabel.setText("  服务器未连接，当前仅可预览界面");
            return;
        }
        UiTasks.run(new UiTasks.Task<Void>() {
            @Override
            public Void run() {
                api.updateCourse(request);
                return null;
            }
        }, new UiTasks.Success<Void>() {
            @Override
            public void accept(Void result) {
                statusLabel.setText("  课程已更新");
                refresh();
            }
        }, new UiTasks.Failure() {
            @Override
            public void accept(ApiException error) {
                statusLabel.setText("  " + error.getMessage());
            }
        });
    }

    private void submitDelete(final String courseCode) {
        if (api == null) {
            statusLabel.setText("  服务器未连接，当前仅可预览界面");
            return;
        }
        UiTasks.run(new UiTasks.Task<Void>() {
            @Override
            public Void run() {
                api.deleteCourse(courseCode);
                return null;
            }
        }, new UiTasks.Success<Void>() {
            @Override
            public void accept(Void result) {
                statusLabel.setText("  课程已删除");
                refresh();
            }
        }, new UiTasks.Failure() {
            @Override
            public void accept(ApiException error) {
                statusLabel.setText("  " + error.getMessage());
            }
        });
    }

    private JPanel fieldForm(String[] labels, JComponent[] fields) {
        JPanel panel = new JPanel(new GridLayout(0, 2, 8, 8));
        panel.setOpaque(false);
        for (int i = 0; i < labels.length; i++) {
            JLabel label = new JLabel(labels[i]);
            label.setForeground(UiTheme.MUTED);
            label.setFont(UiTheme.font(Font.BOLD, 13F));
            panel.add(label);
            panel.add(fields[i]);
        }
        return panel;
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
