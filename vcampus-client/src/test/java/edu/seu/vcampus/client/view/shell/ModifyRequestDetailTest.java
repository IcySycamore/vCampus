package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.common.student.entity.CampusStatus;
import edu.seu.vcampus.common.student.entity.ModifyRequestStatus;
import edu.seu.vcampus.common.student.entity.StudentModifyRequest;
import edu.seu.vcampus.common.student.entity.StudentProfile;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 申请单详情测试：变更串解码与详情全文都是纯函数，直接单测（ADR-0005：不写 GUI 自动化）。
 *
 * <p>
 * 这一块值得单独钉住，是因为「改了啥、为什么改」正是教务批不批的依据：解码错一项，
 * 审批人看到的就是另一条申请。
 */
class ModifyRequestDetailTest {

    /**
     * 三个白名单字段都要翻成人话：状态存的是枚举名，展示要用显示名。
     */
    @Test
    void describesKnownFieldsInPlainWords() {
        List<String> lines = StudentModifyRequests.describeChanges(
                "field=软件工程;joinYear=2024;status=SUSPENDED");

        assertEquals(3, lines.size());
        assertEquals("  专业 / 研究方向：软件工程", lines.get(0));
        assertEquals("  入学年份：2024", lines.get(1));
        assertEquals("  在校状态：" + CampusStatus.SUSPENDED.getDisplayName(), lines.get(2),
                "状态不该把 SUSPENDED 这种枚举名丢给教务看");
    }

    /**
     * 认不出的字段名照原样露出，不静默吞掉——宁可多一行陌生键，也不能少一条真实变更。
     */
    @Test
    void keepsUnknownFieldsInsteadOfDroppingThem() {
        List<String> lines = StudentModifyRequests.describeChanges("mystery=x;field=软件工程");

        assertEquals(2, lines.size());
        assertTrue(lines.get(0).contains("mystery"), "陌生字段名要照原样露出");
        assertTrue(lines.get(1).contains("软件工程"));
    }

    /**
     * 枚举名非法（脏数据）时原样展示，详情页不该因此打不开。
     */
    @Test
    void toleratesBadEnumValue() {
        List<String> lines = StudentModifyRequests.describeChanges("status=NOT_A_STATUS");

        assertEquals(1, lines.size());
        assertTrue(lines.get(0).contains("NOT_A_STATUS"));
    }

    /**
     * 空变更串给空列表，不抛异常。
     */
    @Test
    void emptyChangesGiveEmptyList() {
        assertEquals(0, StudentModifyRequests.describeChanges(null).size());
        assertEquals(0, StudentModifyRequests.describeChanges("   ").size());
    }

    /**
     * 详情全文：该有的行都在，学号与姓名查得到就带上。
     */
    @Test
    void detailLinesCarryStudentNoAndName() {
        StudentModifyRequest request = request();
        StudentProfile profile = new StudentProfile("uuid-stu", 2026, CampusStatus.ENROLLED);
        profile.setStudentNo("20260001");
        profile.setRealName("演示学生");

        List<String> lines = ModifyRequestDetailDialog.linesOf(request, profile);
        String all = join(lines);

        assertTrue(all.contains("申请单号：7"));
        assertTrue(all.contains("学号 20260001"), "教务要能认人，光有账户 uuid 不够");
        assertTrue(all.contains("姓名 演示学生"));
        assertTrue(all.contains("状态：" + ModifyRequestStatus.PENDING.getDisplayName()));
        assertTrue(all.contains("变更内容："));
        assertTrue(all.contains("  专业 / 研究方向：软件工程"));
        assertTrue(all.contains("申请理由：录错了"));
    }

    /**
     * 查不到目标档案（可能已注销）时也要能看全文，只是少学号姓名并注明原因。
     */
    @Test
    void detailLinesSurviveMissingProfile() {
        List<String> lines = ModifyRequestDetailDialog.linesOf(request(), null);
        String all = join(lines);

        assertTrue(all.contains("未查到档案"));
        assertFalse(all.contains("学号"), "查不到档案就不该编一个学号出来");
        assertTrue(all.contains("申请理由：录错了"));
    }

    /**
     * 造一条待审申请。
     *
     * @return 申请单
     */
    private static StudentModifyRequest request() {
        StudentModifyRequest request = new StudentModifyRequest(Long.valueOf(1L), "uuid-stu",
                "field=软件工程", "录错了");
        request.setRequestId(Long.valueOf(7L));
        request.setStatus(ModifyRequestStatus.PENDING);
        request.setAppliedAt(1700000000000L);
        return request;
    }

    /**
     * 拼成一整段文本便于断言。
     *
     * @param lines 行
     * @return 合并文本
     */
    private static String join(List<String> lines) {
        StringBuilder text = new StringBuilder();
        int index = 0;
        while (index < lines.size()) {
            text.append(lines.get(index)).append('\n');
            index = index + 1;
        }
        return text.toString();
    }
}
