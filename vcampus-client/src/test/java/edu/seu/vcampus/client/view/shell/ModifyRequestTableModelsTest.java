package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.common.student.entity.ModifyRequestStatus;
import edu.seu.vcampus.common.student.entity.RequestField;
import edu.seu.vcampus.common.student.entity.StudentModifyRequest;

import java.util.ArrayList;
import java.util.List;
import javax.swing.table.DefaultTableModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 修改申请表格回填测试。时间列只断言格式不断言具体时刻——时区一变就会飘。
 */
class ModifyRequestTableModelsTest {

    @Test
    void fillsRequestRow() {
        DefaultTableModel model = ModifyRequestTableModels.create();
        StudentModifyRequest request = new StudentModifyRequest(Long.valueOf(3L), "u-9",
                "field=软件工程", "换研究方向");
        request.setRequestId(Long.valueOf(11L));
        request.setStatus(ModifyRequestStatus.PENDING);
        request.setAppliedAt(1700000000000L);

        ModifyRequestTableModels.fill(model, one(request));

        assertEquals(9, model.getColumnCount());
        assertEquals(1, model.getRowCount());
        assertEquals("11", model.getValueAt(0, 0));
        assertEquals("3", model.getValueAt(0, 1));
        assertEquals("u-9", model.getValueAt(0, 2));
        assertEquals("field=软件工程", model.getValueAt(0, 3));
        assertEquals("换研究方向", model.getValueAt(0, 4));
        assertEquals("待审核", model.getValueAt(0, 5));
        String appliedAt = String.valueOf(model.getValueAt(0, 6));
        assertTrue(appliedAt.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}"));
    }

    @Test
    void statusDisplayNamesFollowTheEnum() {
        DefaultTableModel model = ModifyRequestTableModels.create();
        List<StudentModifyRequest> requests = new ArrayList<StudentModifyRequest>();
        requests.add(requestWithStatus(ModifyRequestStatus.APPROVED));
        requests.add(requestWithStatus(ModifyRequestStatus.REJECTED));

        ModifyRequestTableModels.fill(model, requests);

        assertEquals("已通过", model.getValueAt(0, 5));
        assertEquals("已驳回", model.getValueAt(1, 5));
    }

    @Test
    void unrecordedValuesBecomeDash() {
        DefaultTableModel model = ModifyRequestTableModels.create();
        StudentModifyRequest request = new StudentModifyRequest();

        ModifyRequestTableModels.fill(model, one(request));

        assertEquals("-", model.getValueAt(0, 0));
        assertEquals("-", model.getValueAt(0, 1));
        assertEquals("-", model.getValueAt(0, 2));
        assertEquals("-", model.getValueAt(0, 3));
        assertEquals("-", model.getValueAt(0, 4));
        assertEquals("-", model.getValueAt(0, 5));
        assertEquals("-", model.getValueAt(0, 6));
        assertEquals("-", model.getValueAt(0, 7));
        assertEquals("-", model.getValueAt(0, 8));
    }

    /**
     * 审核意见与审核时间也得落到表里：学生就是靠这两列知道为什么被驳回、什么时候批的。
     */
    @Test
    void showsAuditResult() {
        DefaultTableModel model = ModifyRequestTableModels.create();
        StudentModifyRequest request = requestWithStatus(ModifyRequestStatus.REJECTED);
        request.setComment("材料不足");
        request.setAuditedAt(1700000060000L);

        ModifyRequestTableModels.fill(model, one(request));

        assertEquals("材料不足", model.getValueAt(0, 7));
        String auditedAt = String.valueOf(model.getValueAt(0, 8));
        assertTrue(auditedAt.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}"));
    }

    @Test
    void fillReplacesPreviousRowsAndToleratesNull() {
        DefaultTableModel model = ModifyRequestTableModels.create();
        ModifyRequestTableModels.fill(model, one(requestWithStatus(ModifyRequestStatus.PENDING)));
        assertEquals(1, model.getRowCount());

        ModifyRequestTableModels.fill(model, null);

        assertEquals(0, model.getRowCount());
    }

    @Test
    void cellsAreNotEditable() {
        DefaultTableModel model = ModifyRequestTableModels.create();

        assertFalse(model.isCellEditable(0, 0));
    }

    /**
     * 列下标 → 排序字段的映射：能排的列逐列对上，自由文本列一律点不动。
     *
     * <p>
     * 「点不动」不是偷懒：变更内容与理由是可以很长的自由文本，拿它排序没有意义；审核意见与审核
     * 时间则大量为空，排出来只会让人困惑。点上去没反应，比给出一个无法解释的顺序更好。
     */
    @Test
    void mapsSortableColumnsAndRejectsFreeTextOnes() {
        assertEquals(RequestField.REQUEST_ID, ModifyRequestTableModels.sortFieldOf(0));
        assertEquals(RequestField.PROFILE_ID, ModifyRequestTableModels.sortFieldOf(1));
        assertEquals(RequestField.APPLICANT_UUID, ModifyRequestTableModels.sortFieldOf(2));
        assertNull(ModifyRequestTableModels.sortFieldOf(3), "变更内容不可排序");
        assertNull(ModifyRequestTableModels.sortFieldOf(4), "理由不可排序");
        assertEquals(RequestField.STATUS, ModifyRequestTableModels.sortFieldOf(5));
        assertEquals(RequestField.APPLIED_AT, ModifyRequestTableModels.sortFieldOf(6));
        assertNull(ModifyRequestTableModels.sortFieldOf(7), "审核意见不可排序");
        assertNull(ModifyRequestTableModels.sortFieldOf(8), "审核时间不可排序");
        assertNull(ModifyRequestTableModels.sortFieldOf(9), "越界列不可排序");
    }

    /**
     * 造一条带状态的申请单。
     *
     * @param status 状态
     * @return 申请单
     */
    private static StudentModifyRequest requestWithStatus(ModifyRequestStatus status) {
        StudentModifyRequest request = new StudentModifyRequest(Long.valueOf(1L), "u-1", "a=b", "r");
        request.setStatus(status);
        return request;
    }

    /**
     * 包一层单元素列表。
     *
     * @param request 申请单
     * @return 只含该申请单的列表
     */
    private static List<StudentModifyRequest> one(StudentModifyRequest request) {
        List<StudentModifyRequest> requests = new ArrayList<StudentModifyRequest>();
        requests.add(request);
        return requests;
    }
}
