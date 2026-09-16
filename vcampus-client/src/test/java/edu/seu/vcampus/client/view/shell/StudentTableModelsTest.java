package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.common.student.entity.CampusStatus;
import edu.seu.vcampus.common.student.entity.PersonCategory;
import edu.seu.vcampus.common.student.entity.StudentField;
import edu.seu.vcampus.common.student.entity.StudentProfile;

import java.util.ArrayList;
import java.util.List;
import javax.swing.table.DefaultTableModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

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
     * 列下标 → 排序字段的映射逐列对上。
     *
     * <p>
     * 这个映射不能单看：它必须与表头（{@link StudentTableModels#COLUMNS}）的次序严格一致，错一格
     * 就会变成「点姓名按学号排」——界面上完全看不出问题，只会在用户心里默默折损信任。所以逐列断言，
     * 而不是抽查两列。
     */
    @Test
    void mapsEveryColumnToItsSortField() {
        assertEquals(StudentField.PROFILE_ID, StudentTableModels.sortFieldOf(0));
        assertEquals(StudentField.STUDENT_NO, StudentTableModels.sortFieldOf(1));
        assertEquals(StudentField.CATEGORY, StudentTableModels.sortFieldOf(2));
        assertEquals(StudentField.REAL_NAME, StudentTableModels.sortFieldOf(3));
        assertEquals(StudentField.FIELD, StudentTableModels.sortFieldOf(4));
        assertEquals(StudentField.JOIN_YEAR, StudentTableModels.sortFieldOf(5));
        assertEquals(StudentField.STATUS, StudentTableModels.sortFieldOf(6));
    }

    /**
     * 表头文字数必须与映射的表宽一致，且越界列不参与排序。
     */
    @Test
    void sortMappingHasNoExtraColumns() {
        DefaultTableModel model = StudentTableModels.create();
        int width = model.getColumnCount();

        assertNull(StudentTableModels.sortFieldOf(width), "越界列应不可排序");
        assertNull(StudentTableModels.sortFieldOf(-1));
        assertNotNull(StudentTableModels.sortFieldOf(width - 1), "最后一列也必须能排序");
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
