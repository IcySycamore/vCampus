package edu.seu.vcampus.client.course;

import edu.seu.vcampus.common.course.Score;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.DefaultTableModel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * 成绩表格模型渲染测试。
 */
public class ScoreTableModelsTest {

    /**
     * 学生视角应有五列。
     */
    @Test
    void studentColumnsHaveFiveEntries() {
        assertEquals(5, ScoreTableModels.STUDENT_COLUMNS.length);
    }

    /**
     * 教师/管理员视角应有六列。
     */
    @Test
    void teacherColumnsHaveSixEntries() {
        assertEquals(6, ScoreTableModels.TEACHER_COLUMNS.length);
    }

    /**
     * 创建的表格模型应不可编辑。
     */
    @Test
    void createReturnsNonEditableModel() {
        DefaultTableModel model = ScoreTableModels.create(ScoreTableModels.STUDENT_COLUMNS);

        assertFalse(model.isCellEditable(0, 0));
    }

    /**
     * 学生视角应按课程编号、名称、学分、学期、成绩顺序渲染。
     */
    @Test
    void renderPopulatesStudentRow() {
        DefaultTableModel model = ScoreTableModels.create(ScoreTableModels.STUDENT_COLUMNS);
        List<ScoreRecord> records = new ArrayList<ScoreRecord>();
        Score score = new Score("s1", "CS101", "2026-2027-1");
        score.setScore(Double.valueOf(88.5));
        records.add(new ScoreRecord(score, "数据结构", 3));

        int size = ScoreTableModels.render(model, records, true);

        assertEquals(1, size);
        assertEquals(1, model.getRowCount());
        assertEquals("CS101", model.getValueAt(0, 0));
        assertEquals("数据结构", model.getValueAt(0, 1));
        assertEquals("3", model.getValueAt(0, 2));
        assertEquals("2026-2027-1", model.getValueAt(0, 3));
        assertEquals("88.5", model.getValueAt(0, 4));
    }

    /**
     * 教师视角应额外在第一列展示学号。
     */
    @Test
    void renderTeacherRowIncludesStudentUuid() {
        DefaultTableModel model = ScoreTableModels.create(ScoreTableModels.TEACHER_COLUMNS);
        List<ScoreRecord> records = new ArrayList<ScoreRecord>();
        Score score = new Score("s1", "CS101", "2026-2027-1");
        score.setScore(Double.valueOf(88.5));
        records.add(new ScoreRecord(score, "数据结构", 3));

        ScoreTableModels.render(model, records, false);

        assertEquals(6, model.getColumnCount());
        assertEquals("s1", model.getValueAt(0, 0));
        assertEquals("CS101", model.getValueAt(0, 1));
        assertEquals("数据结构", model.getValueAt(0, 2));
    }
}

