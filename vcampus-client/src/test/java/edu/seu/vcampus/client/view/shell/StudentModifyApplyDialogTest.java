package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.common.student.dto.StudentModifyRequest;
import edu.seu.vcampus.common.student.entity.CampusStatus;
import edu.seu.vcampus.common.student.entity.PersonCategory;
import edu.seu.vcampus.common.student.entity.StudentProfile;

import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 修改申请的内容组装测试：只提交真正改动过的字段。
 *
 * <p>
 * 这一层值得单测是因为它决定「申请单里到底写了什么」——多写一项会造成一次无人申请过的变更，
 * 少写一项则审核通过后学籍根本没变。界面本身不测（ADR-0005）。
 */
class StudentModifyApplyDialogTest {

    @Test
    void noChangeSubmitsNothing() {
        Map<String, String> changes = StudentModifyApplyDialog.changesOf(profile(),
                "软件工程", "2021", CampusStatus.ENROLLED.name());

        assertTrue(changes.isEmpty(), "原样提交不该产生任何变更");
    }

    @Test
    void paddedValueIsNotAChange() {
        Map<String, String> changes = StudentModifyApplyDialog.changesOf(profile(),
                "  软件工程  ", "2021", CampusStatus.ENROLLED.name());

        assertTrue(changes.isEmpty(), "前后空格不算改动");
    }

    @Test
    void changedFieldIsSubmitted() {
        Map<String, String> changes = StudentModifyApplyDialog.changesOf(profile(),
                "计算机科学与技术", "2021", CampusStatus.ENROLLED.name());

        assertEquals(1, changes.size());
        assertEquals("计算机科学与技术", changes.get(StudentModifyRequest.FIELD_FIELD));
    }

    @Test
    void changedYearIsSubmitted() {
        Map<String, String> changes = StudentModifyApplyDialog.changesOf(profile(),
                "软件工程", "2022", CampusStatus.ENROLLED.name());

        assertEquals(1, changes.size());
        assertEquals("2022", changes.get(StudentModifyRequest.FIELD_JOIN_YEAR));
    }

    @Test
    void statusIsSentAsEnumName() {
        Map<String, String> changes = StudentModifyApplyDialog.changesOf(profile(),
                "软件工程", "2021", CampusStatus.SUSPENDED.name());

        assertEquals(1, changes.size());
        assertEquals("SUSPENDED", changes.get(StudentModifyRequest.FIELD_STATUS));
    }

    @Test
    void allThreeChangesAreSubmitted() {
        Map<String, String> changes = StudentModifyApplyDialog.changesOf(profile(),
                "计算机科学与技术", "2022", CampusStatus.SUSPENDED.name());

        assertEquals(3, changes.size());
        assertEquals("计算机科学与技术", changes.get(StudentModifyRequest.FIELD_FIELD));
        assertEquals("2022", changes.get(StudentModifyRequest.FIELD_JOIN_YEAR));
        assertEquals("SUSPENDED", changes.get(StudentModifyRequest.FIELD_STATUS));
    }

    @Test
    void emptyStatusMeansNotChosen() {
        Map<String, String> changes = StudentModifyApplyDialog.changesOf(profile(),
                "软件工程", "2021", "");

        assertTrue(changes.isEmpty(), "未选状态时不应凭空写一个状态进去");
    }

    @Test
    void nullProfileSubmitsNothing() {
        Map<String, String> changes = StudentModifyApplyDialog.changesOf(null,
                "计算机科学与技术", "2022", CampusStatus.SUSPENDED.name());

        assertTrue(changes.isEmpty());
    }

    /**
     * 造一份当前档案。
     *
     * @return 学生档案（专业「软件工程」、2021 年入学、在校）
     */
    private static StudentProfile profile() {
        StudentProfile profile = new StudentProfile("u-1", PersonCategory.STUDENT, 2021,
                CampusStatus.ENROLLED);
        profile.setId(Long.valueOf(5L));
        profile.setField("软件工程");
        return profile;
    }
}
