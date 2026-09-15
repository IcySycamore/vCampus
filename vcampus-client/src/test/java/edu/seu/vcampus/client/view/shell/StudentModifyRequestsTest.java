package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.common.student.dto.StudentModifyRequest;
import edu.seu.vcampus.common.student.entity.CampusStatus;
import edu.seu.vcampus.common.student.entity.PersonCategory;
import edu.seu.vcampus.common.student.entity.StudentProfile;

import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 202 申请单组装规则测试：只提交真正改动过的字段。
 *
 * <p>
 * 这一层值得单测，是因为它决定「申请单里到底写了什么」——多写一项会造成一次无人申请过的变更，
 * 少写一项则审核通过后学籍根本没变。界面本身不测（ADR-0005）。
 */
class StudentModifyRequestsTest {

    @Test
    void noChangeSubmitsNothing() {
        Map<String, String> changes = StudentModifyRequests.changesOf(profile(),
                "软件工程", "2021", CampusStatus.ENROLLED.name());

        assertTrue(changes.isEmpty(), "原样提交不该产生任何变更");
    }

    @Test
    void paddedValueIsNotAChange() {
        Map<String, String> changes = StudentModifyRequests.changesOf(profile(),
                "  软件工程  ", "2021", CampusStatus.ENROLLED.name());

        assertTrue(changes.isEmpty(), "前后空格不算改动");
    }

    @Test
    void changedFieldIsSubmitted() {
        Map<String, String> changes = StudentModifyRequests.changesOf(profile(),
                "计算机科学与技术", "2021", CampusStatus.ENROLLED.name());

        assertEquals(1, changes.size());
        assertEquals("计算机科学与技术", changes.get(StudentModifyRequest.FIELD_FIELD));
    }

    @Test
    void changedYearIsSubmitted() {
        Map<String, String> changes = StudentModifyRequests.changesOf(profile(),
                "软件工程", "2022", CampusStatus.ENROLLED.name());

        assertEquals(1, changes.size());
        assertEquals("2022", changes.get(StudentModifyRequest.FIELD_JOIN_YEAR));
    }

    @Test
    void statusIsSentAsEnumName() {
        Map<String, String> changes = StudentModifyRequests.changesOf(profile(),
                "软件工程", "2021", CampusStatus.SUSPENDED.name());

        assertEquals(1, changes.size());
        assertEquals("SUSPENDED", changes.get(StudentModifyRequest.FIELD_STATUS));
    }

    @Test
    void allThreeChangesAreSubmitted() {
        Map<String, String> changes = StudentModifyRequests.changesOf(profile(),
                "计算机科学与技术", "2022", CampusStatus.SUSPENDED.name());

        assertEquals(3, changes.size());
        assertEquals("计算机科学与技术", changes.get(StudentModifyRequest.FIELD_FIELD));
        assertEquals("2022", changes.get(StudentModifyRequest.FIELD_JOIN_YEAR));
        assertEquals("SUSPENDED", changes.get(StudentModifyRequest.FIELD_STATUS));
    }

    @Test
    void emptyStatusMeansNotChosen() {
        assertTrue(StudentModifyRequests.changesOf(profile(), "软件工程", "2021", "")
                .isEmpty(), "未选状态时不该凭空写一个进去");
        assertTrue(StudentModifyRequests.changesOf(profile(), "软件工程", "2021", null)
                .isEmpty(), "null 状态同理");
    }

    @Test
    void nullProfileSubmitsNothing() {
        Map<String, String> changes = StudentModifyRequests.changesOf(null,
                "计算机科学与技术", "2022", CampusStatus.SUSPENDED.name());

        assertTrue(changes.isEmpty());
    }

    @Test
    void statusNamesCoverEveryEnumValue() {
        String[] names = StudentModifyRequests.statusNames();

        assertEquals(CampusStatus.values().length, names.length);
        assertEquals(CampusStatus.ENROLLED.getDisplayName(), names[0]);
    }

    @Test
    void statusOfMapsDisplayNameBackToEnum() {
        assertEquals(CampusStatus.SUSPENDED,
                StudentModifyRequests.statusOf(CampusStatus.SUSPENDED.getDisplayName()));
        assertNull(StudentModifyRequests.statusOf(null), "未选时给 null，不是随手挑一个");
        assertNull(StudentModifyRequests.statusOf("   "));
    }

    @Test
    void isIntegerAcceptsOnlyWholeNumbers() {
        assertTrue(StudentModifyRequests.isInteger("2021"));
        assertTrue(StudentModifyRequests.isInteger(" 2021 "));
        assertFalse(StudentModifyRequests.isInteger("20.21"), "年份不该接受小数");
        assertFalse(StudentModifyRequests.isInteger("二零二一"));
        assertFalse(StudentModifyRequests.isInteger(null));
    }

    @Test
    void checkRejectsProfileWithoutId() {
        StudentProfile noId = profile();
        noId.setId(null);

        assertEquals("这份档案还没有主键，无法提交申请",
                StudentModifyRequests.check(noId, "计算机科学与技术", "2021",
                        CampusStatus.ENROLLED.name(), "想改"));
    }

    @Test
    void checkRejectsEmptyReason() {
        assertEquals("请填申请理由——教务要据此判断批不批",
                StudentModifyRequests.check(profile(), "计算机科学与技术", "2021",
                        CampusStatus.ENROLLED.name(), "   "));
    }

    @Test
    void checkRejectsNoChange() {
        assertEquals("没有改动任何字段，不需要提交申请",
                StudentModifyRequests.check(profile(), "软件工程", "2021",
                        CampusStatus.ENROLLED.name(), "想改"));
    }

    @Test
    void checkRejectsNonNumericYear() {
        assertEquals("入学年份要填整数",
                StudentModifyRequests.check(profile(), "软件工程", "20.21",
                        CampusStatus.ENROLLED.name(), "想改"));
    }

    @Test
    void checkPassesOnARealChange() {
        assertNull(StudentModifyRequests.check(profile(), "计算机科学与技术", "2021",
                CampusStatus.ENROLLED.name(), "专业当初填错了"));
    }

    /**
     * 造一份当前档案：学生、专业「软件工程」、2021 年入学、在校。
     *
     * @return 档案
     */
    private static StudentProfile profile() {
        StudentProfile profile = new StudentProfile("u-1", PersonCategory.STUDENT, 2021,
                CampusStatus.ENROLLED);
        profile.setId(Long.valueOf(5L));
        profile.setField("软件工程");
        return profile;
    }
}
