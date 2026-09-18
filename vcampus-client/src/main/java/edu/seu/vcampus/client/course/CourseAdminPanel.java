package edu.seu.vcampus.client.course;

import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.client.view.UiTasks;
import edu.seu.vcampus.client.view.component.PageBarPanel;
import edu.seu.vcampus.client.view.theme.UiFactory;
import edu.seu.vcampus.client.view.theme.UiTheme;
import edu.seu.vcampus.common.course.College;
import edu.seu.vcampus.common.course.Course;
import edu.seu.vcampus.common.course.Teacher;
import edu.seu.vcampus.common.course.dto.CourseSaveRequest;
import edu.seu.vcampus.common.message.PageResponse;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

/**
 * 管理员「课程目录」界面：课程表格 + 表头排序 + 分页 + 关键字筛选，支持新增 / 修改 / 删除课程。
 *
 * <p>
 * 这是服务端 310（新增）/ 311（修改）/ 312（删除）三个命令的正式入口——此前这三个命令没有任何界面调用， 本页也从未被挂到侧栏，等于课程只能靠直接写库维护。
 *
 * <p>
 * 表格列顺序必须与 {@link CourseSorter} 的列索引一致（编号 / 名称 / 学分 / 学期 / 教师 / 容量 / 已选）。
 */
public class CourseAdminPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final String[] COLUMNS = {
            "课程编号", "课程名称", "学分", "学期", "授课教师", "容量", "已选"
    };
    private static final int PAGE_SIZE = 8;

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
    private final JLabel statusLabel = new JLabel("  请登录后使用课程目录");
    private final PageBarPanel pager;
    private final CourseCatalogForm form = new CourseCatalogForm();

    private final List<Course> allCourses = new ArrayList<Course>();
    private final List<Teacher> teachers = new ArrayList<Teacher>();
    private final List<College> colleges = new ArrayList<College>();
    private int sortColumn = CourseSorter.COL_CODE;
    private boolean ascending = true;

    /** 教师/学院下拉取不到时的提示（辅助数据缺失不影响课程表）。 */
    private String optionsHint;

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
        add(CourseViewBuilder.catalogHeading(), BorderLayout.NORTH);
        add(center(), BorderLayout.CENTER);
        CourseViewBuilder.styleStatus(statusLabel);
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
        JLabel label = new JLabel("关键词");
        label.setForeground(UiTheme.MUTED);
        label.setFont(UiTheme.font(Font.BOLD, 13F));
        toolbar.add(label);
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

        JButton addButton = UiFactory.primaryButton("新增课程", "user");
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                openForm(null);
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
                openForm(selected);
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
            statusLabel.setText("  请登录后使用课程目录");
            return;
        }
        UiTasks.run(new UiTasks.Task<Void>() {
            @Override
            public Void run() {
                // 课程表是关键内容，先取；教师/学院下拉只是辅助，取不到也不能让整页刷不出来
                allCourses.clear();
                List<Course> courses = api.listCourses();
                if (courses != null) {
                    allCourses.addAll(courses);
                }
                optionsHint = null;
                try {
                    teachers.clear();
                    List<Teacher> loadedTeachers = api.listTeachers();
                    if (loadedTeachers != null) {
                        teachers.addAll(loadedTeachers);
                    }
                } catch (ApiException exception) {
                    optionsHint = "（教师列表取不到，认领教师时只能保持原值）";
                }
                try {
                    colleges.clear();
                    List<College> loadedColleges = api.listColleges();
                    if (loadedColleges != null) {
                        colleges.addAll(loadedColleges);
                    }
                } catch (ApiException exception) {
                    optionsHint = (optionsHint == null ? "" : optionsHint + " ")
                            + "（学院列表取不到，学院只能保持原值）";
                }
                return null;
            }
        }, new UiTasks.Success<Void>() {
            @Override
            public void accept(Void result) {
                pager.resetToFirstPage();
                renderPage();
            }
        }, UiTasks.failureWithDialog(this, "课程目录操作失败", statusLabel));
    }

    private void renderPage() {
        List<Course> filtered = CourseFilters.filter(allCourses, keywordField.getText());
        List<Course> sorted = CourseSorter.sort(filtered, sortColumn, ascending);
        PageResponse<Course> page = CoursePaging.page(sorted, pager.getPageNumber(), PAGE_SIZE);
        model.setRowCount(0);
        for (Course course : page.getItems()) {
            model.addRow(new Object[] {
                    course.getCode(), course.getName(), Integer.valueOf(course.getCredit()),
                    course.getSemester() == null ? "--" : course.getSemester(),
                    CourseDisplay.teacherOf(course), Integer.valueOf(course.getCapacity()),
                    Integer.valueOf(course.getEnrolled())
            });
        }
        pager.sync(page);
        statusLabel.setText("  共 " + page.getTotal() + " 门课程（点击表头排序，修改/删除前先选中一行）"
                + (optionsHint == null ? "" : optionsHint));
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

    private void openForm(final Course course) {
        if (api == null) {
            statusLabel.setText("  服务器未连接，当前仅可预览界面");
            return;
        }
        form.setOptions(teachers, colleges);
        form.render(course);
        final JDialog dialog = new JDialog();
        dialog.setTitle(course == null ? "新增课程" : "修改课程：" + course.getCode());
        dialog.setModal(true);
        JPanel wrapper = new JPanel(new BorderLayout(0, 8));
        wrapper.setBackground(UiTheme.SURFACE);
        wrapper.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        wrapper.add(form, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        buttons.setOpaque(false);
        JButton cancel = UiFactory.secondaryButton("取消", "return");
        cancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                dialog.dispose();
            }
        });
        buttons.add(cancel);
        JButton save = UiFactory.primaryButton("保存", "user");
        save.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                String error = form.errorMessage();
                if (error != null) {
                    JOptionPane.showMessageDialog(dialog, error, "无法保存",
                            JOptionPane.WARNING_MESSAGE);
                    return;
                }
                CourseSaveRequest request = form.request();
                dialog.dispose();
                submit(request, course == null);
            }
        });
        buttons.add(save);
        wrapper.add(buttons, BorderLayout.SOUTH);
        dialog.setContentPane(wrapper);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void submit(final CourseSaveRequest request, final boolean creating) {
        UiTasks.run(new UiTasks.Task<Void>() {
            @Override
            public Void run() {
                if (creating) {
                    api.addCourse(request);
                } else {
                    api.updateCourse(request);
                }
                return null;
            }
        }, new UiTasks.Success<Void>() {
            @Override
            public void accept(Void result) {
                statusLabel.setText(creating ? "  课程已新增" : "  课程已修改");
                refresh();
            }
        }, UiTasks.failureWithDialog(this, "课程目录操作失败", statusLabel));
    }

    private void confirmDelete(final Course course) {
        int result = JOptionPane.showConfirmDialog(this,
                "确认删除课程「" + course.getCode() + " " + course.getName() + "」？\n"
                        + "已有学生选修的课程不允许删除。",
                "删除课程", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }
        UiTasks.run(new UiTasks.Task<Void>() {
            @Override
            public Void run() {
                api.deleteCourse(course.getCode());
                return null;
            }
        }, new UiTasks.Success<Void>() {
            @Override
            public void accept(Void result) {
                statusLabel.setText("  课程已删除");
                refresh();
            }
        }, UiTasks.failureWithDialog(this, "课程目录操作失败", statusLabel));
    }
}
