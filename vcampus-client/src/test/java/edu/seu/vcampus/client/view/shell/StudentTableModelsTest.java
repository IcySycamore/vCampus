package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.common.student.entity.CampusStatus;
import edu.seu.vcampus.common.student.entity.PersonCategory;
import edu.seu.vcampus.common.student.entity.StudentProfile;

import java.util.ArrayList;
import java.util.List;
import javax.swing.table.DefaultTableModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * 学籍表格回填测试：「实体字段 → 单元格文本」这一段是纯函数，界面本身不测（ADR-0005）。
 */
class StudentTableModelsTest {

    @Test
    void fillsEveryColumnInOrder() {
        DefaultTableModel model = StudentTableModels.create();
        StudentProfile teacher = new StudentProfile("u-teacher", PersonCategory.TEACHER, 2019,
                CampusStatus.RETIRED);
        teacher.setId(Long.valueOf(7L));
        teacher.setStudentNo("20190042");
        teacher.setRealName("演示教师");
        teacher.setField("分布式系统");

        StudentTableModels.fill(model, one(teacher));

        assertEquals(7, model.getColumnCount());
        assertEquals(1, model.getRowCount());
        assertEquals("7", model.getValueAt(0, 0));
        assertEquals("20190042", model.getValueAt(0, 1));
        assertEquals("教师", model.getValueAt(0, 2));
        assertEquals("演示教师", model.getValueAt(0, 3));
        assertEquals("分布式系统", model.getValueAt(0, 4));
        assertEquals("2019", model.getValueAt(0, 5));
        assertEquals("退休", model.getValueAt(0, 6));
    }

    @Test
    void blankTextBecomesDash() {
        DefaultTableModel model = StudentTableModels.create();
        StudentProfile student = new StudentProfile("u-1", PersonCategory.STUDENT, 2023,
                CampusStatus.ENROLLED);
        student.setRealName("   ");
        student.setField(null);

        StudentTableModels.fill(model, one(student));

        assertEquals("-", model.getValueAt(0, 1));
        assertEquals("-", model.getValueAt(0, 3));
        assertEquals("-", model.getValueAt(0, 4));
        assertEquals("学生", model.getValueAt(0, 2));
    }

    @Test
    void missingIdBecomesDashAndMissingStatusStaysDash() {
        DefaultTableModel model = StudentTableModels.create();
        StudentProfile student = new StudentProfile("u-2", PersonCategory.STUDENT, 2021,
                CampusStatus.ENROLLED);
        student.setStatus(null);

        StudentTableModels.fill(model, one(student));

        assertEquals("-", model.getValueAt(0, 0));
        assertEquals("-", model.getValueAt(0, 1));
        assertEquals("-", model.getValueAt(0, 6));
    }

    @Test
    void missingCategoryReadsAsStudent() {
        DefaultTableModel model = StudentTableModels.create();
        StudentProfile student = new StudentProfile("u-4", 2021, CampusStatus.ENROLLED);
        student.setPersonCategory(null);

        StudentTableModels.fill(model, one(student));

        // 实体把「未标记」按学生归一（getPersonCategory 不返回 null），界面跟随同一语义。
        assertEquals("学生", model.getValueAt(0, 2));
    }

    @Test
    void fillReplacesPreviousRowsAndToleratesNull() {
        DefaultTableModel model = StudentTableModels.create();
        StudentTableModels.fill(model, one(new StudentProfile("u-3", 2022, CampusStatus.ENROLLED)));
        assertEquals(1, model.getRowCount());

        StudentTableModels.fill(model, null);

        assertEquals(0, model.getRowCount());
    }

    @Test
    void cellsAreNotEditable() {
        DefaultTableModel model = StudentTableModels.create();

        assertFalse(model.isCellEditable(0, 0));
    }

    /**
     * 包一层单元素列表。
     *
     * @param profile 学籍
     * @return 只含该学籍的列表
     */
    private static List<StudentProfile> one(StudentProfile profile) {
        List<StudentProfile> profiles = new ArrayList<StudentProfile>();
        profiles.add(profile);
        return profiles;
    }
}
