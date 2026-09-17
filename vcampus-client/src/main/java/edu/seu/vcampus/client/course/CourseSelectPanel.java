package edu.seu.vcampus.client.course;

import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.client.view.UiTasks;
import edu.seu.vcampus.client.view.theme.UiTheme;
import edu.seu.vcampus.common.course.Course;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

/**
 * 学生选课与退课的交互界面。
 *
 * <p>渲染可选课程列表，支持按课程编号或名称搜索筛选；选中课程后发起选课或
 * 退课请求，并在底部状态栏展示服务端返回的响应提示。未连接服务器时可离线预览界面。
 */
public class CourseSelectPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final String[] COLUMNS = {"课程编号", "课程名称", "学分", "授课教师", "容量", "已选"};

    final DefaultTableModel courseModel = new DefaultTableModel(COLUMNS, 0) {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    final JTable courseTable = new JTable(courseModel);
    final JTextField keywordField = new JTextField(18);
    final JLabel statusLabel = new JLabel("  当前为界面预览，连接服务器后即可操作");
    final List<Course> allCourses = new ArrayList<Course>();
    final CourseController controller = new CourseController(this);
    private final CourseService api;

    /**
     * 创建离线选课界面。
     */
    public CourseSelectPanel() {
        this(null);
    }

    /**
     * 创建接入选课服务的界面；{@code api} 为 null 时仅离线预览。
     *
     * @param api 选课 API
     */
    public CourseSelectPanel(CourseService api) {
        this.api = api;
        setLayout(new BorderLayout(0, 18));
        setBackground(UiTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(30, 34, 26, 34));
        add(CourseViewBuilder.heading(), BorderLayout.NORTH);
        add(CourseViewBuilder.content(courseTable,
                CourseViewBuilder.toolbar(keywordField, controller)), BorderLayout.CENTER);
        CourseViewBuilder.styleStatus(statusLabel);
        add(statusLabel, BorderLayout.SOUTH);
        if (api == null) {
            statusLabel.setText("  请登录后使用选课服务");
        } else {
            refreshCourses();
        }
    }

    /**
     * 渲染可选课程列表，并应用当前搜索关键词。
     *
     * @param courses 课程列表
     */
    public void renderCourses(List<Course> courses) {
        allCourses.clear();
        if (courses != null) {
            allCourses.addAll(courses);
        }
        controller.applyFilter();
    }

    /**
     * 发起选课请求。
     *
     * @param courseCode 课程编号
     */
    public void selectCourse(final String courseCode) {
        if (api == null) {
            statusLabel.setText("  服务器未连接，当前仅可预览界面");
            return;
        }
        UiTasks.run(new UiTasks.Task<Void>() {
            @Override
            public Void run() {
                api.selectCourse(courseCode);
                return null;
            }
        }, new UiTasks.Success<Void>() {
            @Override
            public void accept(Void result) {
                statusLabel.setText("  选课成功");
                refreshCourses();
            }
        }, new UiTasks.Failure() {
            @Override
            public void accept(ApiException error) {
                statusLabel.setText("  " + error.getMessage());
            }
        });
    }

    /**
     * 发起退课请求。
     *
     * @param courseCode 课程编号
     */
    public void dropCourse(final String courseCode) {
        if (api == null) {
            statusLabel.setText("  服务器未连接，当前仅可预览界面");
            return;
        }
        UiTasks.run(new UiTasks.Task<Void>() {
            @Override
            public Void run() {
                api.dropCourse(courseCode);
                return null;
            }
        }, new UiTasks.Success<Void>() {
            @Override
            public void accept(Void result) {
                statusLabel.setText("  退课成功");
                refreshCourses();
            }
        }, new UiTasks.Failure() {
            @Override
            public void accept(ApiException error) {
                statusLabel.setText("  " + error.getMessage());
            }
        });
    }

    /**
     * 发起查询可选课程列表请求。
     */
    public void refreshCourses() {
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
                renderCourses(courses);
                statusLabel.setText("  可选课程已更新，共 " + courseModel.getRowCount() + " 门");
            }
        }, new UiTasks.Failure() {
            @Override
            public void accept(ApiException error) {
                statusLabel.setText("  " + error.getMessage());
            }
        });
    }

    /**
     * 按关键词筛选课程；空关键词返回全部，null 列表返回空列表。
     *
     * @param courses 待筛选课程
     * @param keyword 搜索关键词
     * @return 匹配的课程列表
     */
    public static List<Course> filterCourses(List<Course> courses, String keyword) {
        return CourseFilters.filter(courses, keyword);
    }

    /** @return 当前课程表格行数 */
    public int getCourseCount() {
        return courseModel.getRowCount();
    }

    /** @return 底部状态栏文本 */
    public String getStatusText() {
        return statusLabel.getText();
    }

}