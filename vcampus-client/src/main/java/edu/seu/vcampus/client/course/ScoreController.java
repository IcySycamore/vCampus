package edu.seu.vcampus.client.course;

import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.course.Score;
import edu.seu.vcampus.common.message.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 成绩界面的业务动作与响应处理控制器。
 */
final class ScoreController {

    final ScorePanel panel;

    ScoreController(ScorePanel panel) {
        this.panel = panel;
    }

    void applyFilter() {
        String key = panel.keywordField.getText() == null
                ? "" : panel.keywordField.getText().trim().toLowerCase();
        List<ScoreRecord> filtered = filter(key);
        int size = ScoreTableModels.render(panel.scoreModel, filtered, panel.studentView);
        panel.gpaLabel.setText("GPA：" + formatGpa(GpaCalculator.weightedGpa(filtered)));
        panel.statusLabel.setText(size == 0 ? "  暂无成绩记录" : "  共 " + size + " 条成绩");
    }

    void saveSelected() {
        String studentId = panel.studentIdField.getText().trim();
        String courseCode = panel.courseCodeField.getText().trim();
        String scoreText = panel.scoreField.getText().trim();
        if (studentId.length() == 0 || courseCode.length() == 0 || scoreText.length() == 0) {
            panel.statusLabel.setText("  请填写学号、课程编号与成绩");
            return;
        }
        Double value;
        try {
            value = Double.valueOf(scoreText);
        } catch (NumberFormatException exception) {
            panel.statusLabel.setText("  成绩必须为数字");
            return;
        }
        // studentId 为用户登录 ID，最终由服务端解析为账户 uuid。
        Score score = new Score(studentId, courseCode, "");
        score.setScore(value);
        panel.send(CourseCommand.SCORE_SAVE, score);
    }

    void applyResponse(Message message) {
        if (!StatusCode.SUCCESS.equals(message.getStatusCode())) {
            panel.statusLabel.setText("  " + String.valueOf(message.getData()));
            return;
        }
        if (message.getCommand() == CourseCommand.SCORE_QUERY) {
            panel.renderScores(toRecords(message.getData()));
            panel.statusLabel.setText("  成绩已更新，共 " + panel.scoreModel.getRowCount() + " 条");
        } else {
            panel.statusLabel.setText("  " + String.valueOf(message.getData()));
        }
    }

    private List<ScoreRecord> filter(String key) {
        List<ScoreRecord> result = new ArrayList<ScoreRecord>();
        for (ScoreRecord record : panel.allRecords) {
            if (key.length() == 0 || matches(record, key)) {
                result.add(record);
            }
        }
        return result;
    }

    private boolean matches(ScoreRecord record, String key) {
        String student = record.getScore().getStudentUuid();
        String code = record.getScore().getCourseCode();
        return (student != null && student.toLowerCase().contains(key))
                || (code != null && code.toLowerCase().contains(key));
    }

    private List<ScoreRecord> toRecords(Object data) {
        List<ScoreRecord> result = new ArrayList<ScoreRecord>();
        if (data instanceof List) {
            List<?> values = (List<?>) data;
            for (Object value : values) {
                if (value instanceof ScoreRecord) {
                    result.add((ScoreRecord) value);
                }
            }
        }
        return result;
    }

    private String formatGpa(double gpa) {
        return String.format(Locale.ROOT, "%.2f", Double.valueOf(gpa));
    }
}
