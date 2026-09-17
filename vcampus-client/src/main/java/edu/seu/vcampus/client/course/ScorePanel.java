package edu.seu.vcampus.client.course;

import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.client.view.UiTasks;
import edu.seu.vcampus.client.view.theme.UiTheme;
import edu.seu.vcampus.common.course.Score;
import edu.seu.vcampus.common.user.entity.Role;

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
 * 成绩查询与成绩录入/修改的交互界面。
 *
 * <p>学生视角展示个人各科成绩及 GPA；教师/管理员视角额外提供成绩录入、
 * 更新与修改表单，并可按学号或课程编号搜索筛选。
 */
public class ScorePanel extends JPanel {

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
    private final CourseService api;

    /**
     * 创建默认学生视角的成绩界面（离线预览）。
     */
    public ScorePanel() {
        this(null, Role.STUDENT.getDisplayName());
    }

    /**
     * 按当前角色创建成绩界面（离线预览）。
     *
     * @param role 角色显示名（学生/教师/管理员）
     */
    public ScorePanel(String role) {
        this(null, role);
    }

    /**
     * 按当前角色创建接入成绩服务的界面；{@code api} 为 null 时仅离线预览。
     *
     * @param api 选课 API
     * @param role 角色显示名（学生/教师/管理员）
     */
    public ScorePanel(CourseService api, String role) {
        this.api = api;
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
        if (api == null) {
            statusLabel.setText("  请登录后使用成绩服务");
        } else {
            refreshScores();
        }
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
     * 发起成绩查询请求并回填界面。
     */
    public void refreshScores() {
        if (api == null) {
            statusLabel.setText("  服务器未连接，当前仅可预览界面");
            return;
        }
        UiTasks.run(new UiTasks.Task<List<Score>>() {
            @Override
            public List<Score> run() {
                return api.listMyScores();
            }
        }, new UiTasks.Success<List<Score>>() {
            @Override
            public void accept(List<Score> scores) {
                renderScores(toRecords(scores));
                statusLabel.setText("  成绩已更新，共 " + scoreModel.getRowCount() + " 条");
            }
        }, new UiTasks.Failure() {
            @Override
            public void accept(ApiException error) {
                statusLabel.setText("  " + error.getMessage());
            }
        });
    }

    /**
     * 提交成绩保存请求；成功后刷新列表。
     *
     * @param score 成绩记录
     */
    void saveScore(final Score score) {
        if (api == null) {
            statusLabel.setText("  服务器未连接，当前仅可预览界面");
            return;
        }
        UiTasks.run(new UiTasks.Task<Void>() {
            @Override
            public Void run() {
                api.saveScore(score);
                return null;
            }
        }, new UiTasks.Success<Void>() {
            @Override
            public void accept(Void result) {
                statusLabel.setText("  成绩已保存");
                refreshScores();
            }
        }, new UiTasks.Failure() {
            @Override
            public void accept(ApiException error) {
                statusLabel.setText("  " + error.getMessage());
            }
        });
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

    private static List<ScoreRecord> toRecords(List<Score> scores) {
        List<ScoreRecord> result = new ArrayList<ScoreRecord>();
        if (scores != null) {
            for (Score score : scores) {
                result.add(new ScoreRecord(score));
            }
        }
        return result;
    }
}

