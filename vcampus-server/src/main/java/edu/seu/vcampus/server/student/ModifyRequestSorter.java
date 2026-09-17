package edu.seu.vcampus.server.student;

import edu.seu.vcampus.common.student.dto.ModifyRequestQuery;
import edu.seu.vcampus.common.student.entity.RequestField;
import edu.seu.vcampus.common.student.entity.StudentModifyRequest;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 申请单排序：按查询条件指定的字段与方向，就地排好一批申请单。
 *
 * <p>
 * 与 {@link StudentSorter} 不同的一点是<b>默认方向</b>：这里默认按申请时间<b>倒序</b>（最新的在
 * 前）。原因是这个列表的日常用法是清待办——刚提交的申请排在最上面才不需要翻页去找；而学籍列表
 * 默认按主键升序，那是「先登记的在前」的自然顺序。
 *
 * <p>
 * 排序必须在切片之前（见 {@link StudentModifyFlow#list}）：只排当前这一页会得到页内有序、页间
 * 乱序的假序，翻页时一眼就能看出来。
 */
final class ModifyRequestSorter {

    /** 私有构造器，禁止实例化工具类。 */
    private ModifyRequestSorter() {
    }

    /**
     * 按 {@link ModifyRequestQuery} 指定的字段与方向排序。
     *
     * @param requests 待排序列表（就地排序）
     * @param query 查询条件；null 或未指定可排序字段时按申请时间倒序
     */
    static void sort(List<StudentModifyRequest> requests, ModifyRequestQuery query) {
        if (requests == null || requests.size() < 2) {
            return;
        }
        final RequestField key = resolveKey(query);
        Comparator<StudentModifyRequest> comparator = new Comparator<StudentModifyRequest>() {
            @Override
            public int compare(StudentModifyRequest left, StudentModifyRequest right) {
                return compareBy(left, right, key);
            }
        };
        if (resolveDescending(query, key)) {
            comparator = Collections.reverseOrder(comparator);
        }
        Collections.sort(requests, comparator);
    }

    /**
     * 取生效的排序字段。
     *
     * @param query 查询条件（可为 null）
     * @return 排序字段，永不为 null
     */
    private static RequestField resolveKey(ModifyRequestQuery query) {
        if (query == null || query.getSortBy() == null
                || !query.getSortBy().isSortable()) {
            return RequestField.APPLIED_AT;
        }
        return query.getSortBy();
    }

    /**
     * 取生效的排序方向。
     *
     * <p>
     * 没显式指定方向时：按申请时间排就取倒序（默认视图），其它字段取升序。
     *
     * @param query 查询条件（可为 null）
     * @param key 生效的排序字段
     * @return 是否倒序
     */
    private static boolean resolveDescending(ModifyRequestQuery query, RequestField key) {
        if (query == null || query.getSortBy() == null
                || !query.getSortBy().isSortable()) {
            return key == RequestField.APPLIED_AT;
        }
        return query.isDescending();
    }

    /**
     * 按字段比较两条申请单。
     *
     * @param left 左
     * @param right 右
     * @param key 排序字段
     * @return 比较结果
     */
    private static int compareBy(StudentModifyRequest left, StudentModifyRequest right,
            RequestField key) {
        if (key == RequestField.STATUS) {
            return compareOrdinals(left.getStatus(), right.getStatus());
        }
        if (key == RequestField.REQUEST_ID) {
            return compareNumbers(left.getRequestId(), right.getRequestId());
        }
        if (key == RequestField.PROFILE_ID) {
            return compareNumbers(left.getProfileId(), right.getProfileId());
        }
        if (key == RequestField.APPLICANT_UUID) {
            return compareText(left.getApplicantUuid(), right.getApplicantUuid());
        }
        return Long.compare(left.getAppliedAt(), right.getAppliedAt());
    }

    /**
     * 比较两个可空数值，空值排在后面。
     *
     * @param left 左
     * @param right 右
     * @return 比较结果
     */
    private static int compareNumbers(Long left, Long right) {
        if (left == null) {
            return right == null ? 0 : 1;
        }
        if (right == null) {
            return -1;
        }
        return left.compareTo(right);
    }

    /**
     * 比较两个可空文本（忽略大小写），空值排在后面。
     *
     * @param left 左
     * @param right 右
     * @return 比较结果
     */
    private static int compareText(String left, String right) {
        if (left == null) {
            return right == null ? 0 : 1;
        }
        if (right == null) {
            return -1;
        }
        return left.compareToIgnoreCase(right);
    }

    /**
     * 比较两个可空枚举，按声明序排列（待审核 → 已通过 → 已驳回），空值排在后面。
     *
     * @param left 左
     * @param right 右
     * @param &lt;T&gt; 枚举类型
     * @return 比较结果
     */
    private static <T extends Enum<T>> int compareOrdinals(T left, T right) {
        if (left == null) {
            return right == null ? 0 : 1;
        }
        if (right == null) {
            return -1;
        }
        return Integer.compare(left.ordinal(), right.ordinal());
    }
}
