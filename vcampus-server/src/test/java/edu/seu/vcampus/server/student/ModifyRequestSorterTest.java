package edu.seu.vcampus.server.student;

import edu.seu.vcampus.common.student.dto.ModifyRequestQuery;
import edu.seu.vcampus.common.student.entity.ModifyRequestStatus;
import edu.seu.vcampus.common.student.entity.RequestField;
import edu.seu.vcampus.common.student.entity.StudentModifyRequest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 申请单排序测试：默认按申请时间倒序，指定字段后按指定字段。
 *
 * <p>
 * 默认方向与学籍列表相反（这里倒序、那边升序）是有意的：审核列表的日常用法是清待办，刚提交的
 * 排在最上面才不用翻页去找；学籍列表则是「先登记的在前」更自然。这条差异容易被后来者「统一」
 * 掉，所以两个方向各钉一条用例。
 */
class ModifyRequestSorterTest {

    /**
     * 不给条件时按申请时间倒序——最新的待办在最前面。
     */
    @Test
    void defaultsToAppliedAtDescending() {
        List<StudentModifyRequest> list = threeRequests();

        ModifyRequestSorter.sort(list, new ModifyRequestQuery());

        assertEquals(3000L, list.get(0).getAppliedAt());
        assertEquals(1000L, list.get(2).getAppliedAt());
    }

    /**
     * 没指定排序字段时（null 条件）也保持同样的默认序。
     */
    @Test
    void nullQueryKeepsDefaultOrder() {
        List<StudentModifyRequest> list = threeRequests();

        ModifyRequestSorter.sort(list, null);

        assertEquals(3000L, list.get(0).getAppliedAt());
    }

    /**
     * 点了表头就按那一列排；同列换方向由 descending 决定。
     */
    @Test
    void sortsByRequestIdBothDirections() {
        List<StudentModifyRequest> list = threeRequests();

        ModifyRequestSorter.sort(list, queryOf(RequestField.REQUEST_ID, false));
        assertEquals(1L, list.get(0).getRequestId().longValue());

        ModifyRequestSorter.sort(list, queryOf(RequestField.REQUEST_ID, true));
        assertEquals(3L, list.get(0).getRequestId().longValue());
    }

    /**
     * 按状态排时用声明序（待审核 → 已通过 → 已驳回），待办自然聚在最前。
     */
    @Test
    void sortsByStatusByDeclarationOrder() {
        List<StudentModifyRequest> list = threeRequests();

        ModifyRequestSorter.sort(list, queryOf(RequestField.STATUS, false));

        assertEquals(ModifyRequestStatus.PENDING, list.get(0).getStatus());
        assertEquals(ModifyRequestStatus.APPROVED, list.get(1).getStatus());
        assertEquals(ModifyRequestStatus.REJECTED, list.get(2).getStatus());
    }

    /**
     * 不可排序的字段（自由文本）传进来应回退到默认序，而不是「不排」。
     */
    @Test
    void unsortableFieldFallsBackToDefault() {
        List<StudentModifyRequest> list = threeRequests();

        ModifyRequestSorter.sort(list, queryOf(RequestField.REASON, false));

        assertEquals(3000L, list.get(0).getAppliedAt(), "理由不可排序，应回退到申请时间倒序");
    }

    /**
     * 造三条申请单：单号 1/2/3，申请时间 1000/2000/3000，状态 已驳回/已通过/待审核。
     *
     * @return 申请单列表
     */
    private static List<StudentModifyRequest> threeRequests() {
        List<StudentModifyRequest> list = new ArrayList<StudentModifyRequest>();
        list.add(request(2L, 2000L, ModifyRequestStatus.APPROVED));
        list.add(request(3L, 3000L, ModifyRequestStatus.PENDING));
        list.add(request(1L, 1000L, ModifyRequestStatus.REJECTED));
        return list;
    }

    /**
     * 造一条申请单。
     *
     * @param id 申请单号
     * @param appliedAt 申请时间
     * @param status 状态
     * @return 申请单
     */
    private static StudentModifyRequest request(long id, long appliedAt,
            ModifyRequestStatus status) {
        StudentModifyRequest request = new StudentModifyRequest(Long.valueOf(42L), "uuid-stu",
                "status=SUSPENDED", "申请休学");
        request.setRequestId(Long.valueOf(id));
        request.setAppliedAt(appliedAt);
        request.setStatus(status);
        return request;
    }

    /**
     * 造一份排序条件。
     *
     * @param field 排序字段
     * @param descending 是否降序
     * @return 查询条件
     */
    private static ModifyRequestQuery queryOf(RequestField field, boolean descending) {
        ModifyRequestQuery query = new ModifyRequestQuery();
        query.setSortBy(field);
        query.setDescending(descending);
        return query;
    }
}
