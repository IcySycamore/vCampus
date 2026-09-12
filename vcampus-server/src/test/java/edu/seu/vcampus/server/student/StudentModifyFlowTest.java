package edu.seu.vcampus.server.student;

import edu.seu.vcampus.common.message.PageResponse;
import edu.seu.vcampus.common.student.dto.ModifyRequestQuery;
import edu.seu.vcampus.common.student.entity.CampusStatus;
import edu.seu.vcampus.common.student.entity.ModifyRequestStatus;
import edu.seu.vcampus.common.student.entity.StudentModifyRequest;
import edu.seu.vcampus.common.student.entity.StudentProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 审核流测试：申请只落单不改学籍、通过才生效、驳回不动、不能重复审核。
 *
 * <p>
 * 这几条断言就是设计文档的验收标准「学生提交修改申请 → 教务审核通过 → 学籍字段真的变了」
 * 在单元层面的落点。
 */
class StudentModifyFlowTest {

    /** 学籍存储。 */
    private StudentDaoMemory dao;

    /** 申请单存储。 */
    private StudentModifyRequestDaoMemory requests;

    /** 被测审核流。 */
    private StudentModifyFlow flow;

    /** 预置的学籍记录。 */
    private StudentProfile profile;

    /**
     * 每个测试前重建存储并预置一条学籍。
     */
    @BeforeEach
    void setUp() {
        dao = new StudentDaoMemory();
        requests = new StudentModifyRequestDaoMemory();
        flow = new StudentModifyFlow(requests, dao);
        profile = new StudentProfile("uuid-stu", 2026, CampusStatus.ENROLLED);
        dao.insert(profile);
    }

    /**
     * 提交申请后只多出一条待审单，学籍原封不动。
     */
    @Test
    void applyCreatesPendingRequestWithoutChangingProfile() {
        assertTrue(flow.apply(profile.getId(), "uuid-stu", statusChange(), "状态填错了"));

        assertEquals(CampusStatus.ENROLLED,
                dao.findById(profile.getId()).getStatus());
        PageResponse<StudentModifyRequest> page = flow.list(null);
        assertEquals(1L, page.getTotal());
        assertEquals(ModifyRequestStatus.PENDING, page.getItems().get(0).getStatus());
    }

    /**
     * 目标学籍不存在时拒绝受理。
     */
    @Test
    void applyRejectsMissingProfile() {
        assertFalse(flow.apply(9999L, "uuid-stu", statusChange(), "无此人"));
        assertEquals(0L, requests.count(null));
    }

    /**
     * 空变更集没有意义，拒绝受理。
     */
    @Test
    void applyRejectsEmptyChanges() {
        assertFalse(flow.apply(profile.getId(), "uuid-stu",
                new LinkedHashMap<String, String>(), "什么都没改"));
    }

    /**
     * 白名单之外的字段（比如账户 uuid）会被丢弃；全是非法字段则整单拒绝。
     */
    @Test
    void applyDropsDisallowedFields() {
        Map<String, String> changes = new LinkedHashMap<String, String>();
        changes.put("userUuid", "uuid-hacked");
        assertFalse(flow.apply(profile.getId(), "uuid-stu", changes, "想改账号"));

        changes.put("status", "SUSPENDED");
        assertTrue(flow.apply(profile.getId(), "uuid-stu", changes, "合法字段一起提"));
        String encoded = requests.findById(1L).getChangesJson();
        assertFalse(encoded.contains("userUuid"));
        assertTrue(encoded.contains("status"));
    }

    /**
     * 审核通过时，申请里的入学年份与状态都写回学籍。
     */
    @Test
    void approveAppliesAllFields() {
        Map<String, String> changes = new LinkedHashMap<String, String>();
        changes.put("joinYear", "2024");
        changes.put("status", "SUSPENDED");
        flow.apply(profile.getId(), "uuid-stu", changes, "入学年份录错");

        assertTrue(flow.audit(1L, true, "情况属实", "uuid-tea"));

        StudentProfile updated = dao.findById(profile.getId());
        assertEquals(2024, updated.getJoinYear());
        assertEquals(CampusStatus.SUSPENDED, updated.getStatus());
        StudentModifyRequest request = requests.findById(1L);
        assertEquals(ModifyRequestStatus.APPROVED, request.getStatus());
        assertEquals("uuid-tea", request.getAuditedBy());
        assertNotNull(request.getAuditedAt());
    }

    /**
     * 驳回时学籍保持原样，但申请单状态与审核人仍要落库（留痕）。
     */
    @Test
    void rejectKeepsProfileUntouched() {
        flow.apply(profile.getId(), "uuid-stu", statusChange(), "试试看");

        assertTrue(flow.audit(1L, false, "材料不足", "uuid-tea"));

        assertEquals(CampusStatus.ENROLLED,
                dao.findById(profile.getId()).getStatus());
        assertEquals(ModifyRequestStatus.REJECTED, requests.findById(1L).getStatus());
        assertEquals("材料不足", requests.findById(1L).getComment());
    }

    /**
     * 同一条申请不能审核两次（否则变更会被反复应用）。
     */
    @Test
    void auditTwiceIsRejected() {
        flow.apply(profile.getId(), "uuid-stu", statusChange(), "第一次");

        assertTrue(flow.audit(1L, true, "通过", "uuid-tea"));
        assertFalse(flow.audit(1L, true, "再通过一次", "uuid-tea"));
    }

    /**
     * 按状态过滤申请单。
     */
    @Test
    void listFiltersByStatus() {
        flow.apply(profile.getId(), "uuid-stu", statusChange(), "第一条");
        flow.apply(profile.getId(), "uuid-stu", statusChange(), "第二条");
        flow.audit(1L, true, "通过", "uuid-tea");

        ModifyRequestQuery pending = new ModifyRequestQuery();
        pending.setStatus(ModifyRequestStatus.PENDING);
        assertEquals(1L, flow.list(pending).getTotal());

        ModifyRequestQuery approved = new ModifyRequestQuery();
        approved.setStatus(ModifyRequestStatus.APPROVED);
        assertEquals(1L, flow.list(approved).getTotal());

        assertEquals(2L, flow.list(null).getTotal());
    }

    /**
     * 列表按提交时间倒序：最新的申请排在最前，教务先看到它。
     */
    @Test
    void listSortsByAppliedAtDescending() {
        flow.apply(profile.getId(), "uuid-stu", statusChange(), "较早");
        flow.apply(profile.getId(), "uuid-stu", statusChange(), "较晚");
        StudentModifyRequest first = requests.findById(1L);
        first.setAppliedAt(1000L);
        requests.update(first);
        StudentModifyRequest second = requests.findById(2L);
        second.setAppliedAt(2000L);
        requests.update(second);

        assertEquals(2L, flow.list(null).getItems().get(0).getRequestId().longValue());
    }

    /**
     * 构造一份只改状态的变更集。
     *
     * @return 变更集
     */
    private static Map<String, String> statusChange() {
        Map<String, String> changes = new LinkedHashMap<String, String>();
        changes.put("status", "SUSPENDED");
        return changes;
    }
}
