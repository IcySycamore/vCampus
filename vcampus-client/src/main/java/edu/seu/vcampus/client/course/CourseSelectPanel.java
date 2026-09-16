package edu.seu.vcampus.client.course;

import edu.seu.vcampus.client.handler.UIUpdateHandler;
import edu.seu.vcampus.client.network.ClientSocketListener;
import edu.seu.vcampus.client.view.theme.UiTheme;
import edu.seu.vcampus.common.course.Course;
import edu.seu.vcampus.common.message.Message;

import java.awt.BorderLayout;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

/**
 * 学生选课与退课的交互界面。
 *
 * <p>渲染可选课程列表，支持按课程编号或名称搜索筛选；选中课程后发起选课或
 * 退课请求，并在底部状态栏展示服务端返回的响应提示。未连接服务器时可离线预览界面。
 */
public class CourseSelectPanel extends JPanel implements UIUpdateHandler {

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
    private ClientSocketListener client;
    private String userId;

    /**
     * 创建离线选课界面。
     */
    public CourseSelectPanel() {
        setLayout(new BorderLayout(0, 18));
        setBackground(UiTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(30, 34, 26, 34));
        add(CourseViewBuilder.heading(), BorderLayout.NORTH);
        add(CourseViewBuilder.content(courseTable,
                CourseViewBuilder.toolbar(keywordField, controller)), BorderLayout.CENTER);
        CourseViewBuilder.styleStatus(statusLabel);
        add(statusLabel, BorderLayout.SOUTH);
    }

    /**
     * 绑定已建立的客户端连接。
     *
     * @param client 客户端连接
     * @param userId 当前用户 ID
     */
    public void attach(ClientSocketListener client, String userId) {
        this.client = client;
        this.userId = userId;
        statusLabel.setText(client != null && client.isConnected()
                ? "  已连接选课服务" : "  连接尚未建立");
    }

    /**
     * 处理服务端返回的消息并刷新界面。
     *
     * @param message 返回消息
     */
    @Override
    public void handleMessage(final Message message) {
        runOnUi(new Runnable() {
            @Override
            public void run() {
                controller.applyResponse(message);
            }
        });
    }

    /**
     * 处理连接关闭事件。
     *
     * @param cause 关闭原因；正常关闭时为 null
     */
    @Override
    public void connectionClosed(final Exception cause) {
        runOnUi(new Runnable() {
            @Override
            public void run() {
                statusLabel.setText(cause == null ? "  连接已关闭" : "  连接中断，请稍后重试");
            }
        });
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
    public void selectCourse(String courseCode) {
        send(CourseCommand.COURSE_SELECT, courseCode);
    }

    /**
     * 发起退课请求。
     *
     * @param courseCode 课程编号
     */
    public void dropCourse(String courseCode) {
        send(CourseCommand.COURSE_DROP, courseCode);
    }

    /**
     * 发起查询可选课程列表请求。
     */
    public void refreshCourses() {
        send(CourseCommand.COURSE_LIST, null);
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

    void send(int command, Object data) {
        if (client == null || !client.isConnected()) {
            statusLabel.setText("  服务器未连接，当前仅可预览界面");
            return;
        }
        Message request = new Message(command, data);
        request.setSender(userId);
        // 业务命令需携带会话 token，登录后由客户端会话层统一注入。
        try {
            client.send(request);
            statusLabel.setText("  请求已发送，请稍候…");
        } catch (IOException exception) {
            statusLabel.setText("  发送失败：" + exception.getMessage());
        }
    }

    private void runOnUi(Runnable action) {
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
        } else {
            SwingUtilities.invokeLater(action);
        }
    }
}