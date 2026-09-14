package edu.seu.vcampus.client.course;

import java.util.List;

import javax.swing.table.DefaultTableModel;

/**
 * 成绩表格模型的创建与数据渲染工具。
 */
final class ScoreTableModels {

    /** 学生视角成绩表列名。 */
    static final String[] STUDENT_COLUMNS = {"课程编号", "课程名称", "学分", "学期", "成绩"};

    /** 教师/管理员视角成绩表列名。 */
    static final String[] TEACHER_COLUMNS = {"学号", "课程编号", "课程名称", "学分", "学期", "成绩"};

    private ScoreTableModels() {
    }

    /**
     * 创建不可编辑的成绩表格模型。
     *
     * @param columns 列名
     * @return 表格模型
     */
    static DefaultTableModel create(String[] columns) {
        return new DefaultTableModel(columns, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    /**
     * 将成绩记录渲染进表格，并返回记录条数。
     *
     * @param model 表格模型
     * @param records 成绩展示记录
     * @param studentView 是否学生视角（隐藏学号列）
     * @return 渲染的记录条数
     */
    static int render(DefaultTableModel model, List<ScoreRecord> records,
            boolean studentView) {
        model.setRowCount(0);
        if (records == null) {
            return 0;
        }
        for (ScoreRecord record : records) {
            model.addRow(toRow(record, studentView));
        }
        return records.size();
    }

    private static Object[] toRow(ScoreRecord record, boolean studentView) {
        String name = record.getCourseName() == null ? "--" : record.getCourseName();
        Double value = record.getScore().getScore();
        String score = value == null ? "--" : String.valueOf(value.doubleValue());
        String credit = record.getCredit() <= 0
                ? "--" : String.valueOf(record.getCredit());
        if (studentView) {
            return new Object[] {
                record.getScore().getCourseCode(), name, credit,
                record.getScore().getSemester(), score
            };
        }
        return new Object[] {
            record.getScore().getStudentUuid(), record.getScore().getCourseCode(), name,
            credit, record.getScore().getSemester(), score
        };
    }
}
