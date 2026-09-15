package edu.seu.vcampus.client.course;

import edu.seu.vcampus.client.handler.UIUpdateHandler;
import edu.seu.vcampus.client.network.ClientSocketListener;
import edu.seu.vcampus.client.view.theme.UiTheme;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.user.entity.Role;

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
 * 成绩查询与成绩录入/修改的交互界面。
 *
 * <p>学生视角展示个人各科成绩及 GPA；教师/管理员视角额外提供成绩录入、
 * 更新与修改表单，并可按学号或课程编号搜索筛选。
 */
public class ScorePanel extends JPanel implements UIUpdateHandler {

    private static final long serialVersionUID = 1L;

    final boolean studentView;
    final DefaultTableModel scoreModel;
    final JTextField keywordField = new JTextField(16);
    final JTextField studentIdField = new JTextField(12);
    final JTextField courseCodeField = new JTextField(10);
    final JTextField scoreField = new JTextField(6);
    final JLabel statusLabel = new JLabel("  当前为界面预览，连接服务器后即可操作");
    final JLabel gpaLabel = new JLabel("GPA：--");
    final List<ScoreRecord> allRecords = new ArrayList<ScoreRecord>();
    final ScoreController controller = new ScoreController(this);
    private ClientSocketListener client;
    private String userId;

    /**
     * 创建默认学生视角的成绩界面。
     */
    public ScorePanel() {
        this(Role.STUDENT.getDisplayName());
    }

    /**
     * 按当前角色创建成绩界面。
     *
     * @param role 角色显示名（学生/教师/管理员）
     */
    public ScorePanel(String role) {
        studentView = Role.STUDENT.getDisplayName().equals(role);
        scoreModel = ScoreTableModels.create(studentView
                ? ScoreTableModels.STUDENT_COLUMNS : ScoreTableModels.TEACHER_COLUMNS);
        JTable scoreTable = new JTable(scoreModel);
        setLayout(new BorderLayout(0, 18));
        setBackground(UiTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(30, 34, 26, 34));
        add(ScoreViewBuilder.heading(studentView), BorderLayout.NORTH);
        add(ScoreViewBuilder.content(scoreTable, toolbar()), BorderLayout.CENTER);
        add(ScoreViewBuilder.footer(statusLabel, gpaLabel), BorderLayout.SOUTH);
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
                ? "  已连接成绩服务" : "  连接尚未建立");
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
     * 渲染成绩列表并刷新 GPA。
     *
     * @param records 成绩展示记录
     */
    public void renderScores(List<ScoreRecord> records) {
        allRecords.clear();
        if (records != null) {
            allRecords.addAll(records);
        }
        controller.applyFilter();
    }

    /**
     * 发起成绩查询请求。
     */
    public void refreshScores() {
        send(CourseCommand.SCORE_QUERY, null);
    }

    /**
     * 返回底部状态栏文本。
     *
     * @return 状态文本
     */
    public String getStatusText() {
        return statusLabel.getText();
    }

    /**
     * 返回当前 GPA 文本。
     *
     * @return GPA 文本
     */
    public String getGpaText() {
        return gpaLabel.getText();
    }

    /**
     * 返回当前成绩表格行数。
     *
     * @return 行数
     */
    public int getScoreCount() {
        return scoreModel.getRowCount();
    }

    private JPanel toolbar() {
        return ScoreViewBuilder.toolbar(studentView, keywordField, studentIdField,
                courseCodeField, scoreField, controller);
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

