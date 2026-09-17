package edu.seu.vcampus.server.student;

import edu.seu.vcampus.common.student.dto.ModifyRequestQuery;
import edu.seu.vcampus.common.student.entity.RequestField;
import edu.seu.vcampus.common.student.entity.StudentModifyRequest;

/**
 * 申请单过滤：判断一条申请单是否满足查询条件（207 命令）。
 *
 * <p>
 * 与 {@link StudentMatcher} 同一套做法：判断集中在这一处，null、空关键词、多字段「或」这些 边界只写一遍。查询条件若与 SQL 写法不一致（比如这边不区分大小写、SQL
 * 那边区分），同一条 查询在两个入口就会得到不同结果。
 *
 * <p>
 * 各条件之间是「与」的关系；关键词内部（多个字段）是「或」。
 */
final class ModifyRequestMatcher {

    /** 私有构造器，禁止实例化工具类。 */
    private ModifyRequestMatcher() {
    }

    /**
     * 判断申请单是否满足查询条件。
     *
     * @param request 申请单
     * @param query   过滤条件；null 表示不过滤（全部通过）
     * @return 是否匹配
     */
    static boolean matches(StudentModifyRequest request, ModifyRequestQuery query) {
        if (request == null) {
            return false;
        }
        if (query == null) {
            return true;
        }
        if (query.getStatus() != null && query.getStatus() != request.getStatus()) {
            return false;
        }
        Long profileId = query.getProfileId();
        if (profileId != null && !profileId.equals(request.getProfileId())) {
            return false;
        }
        String applicant = query.getApplicantUuid();
        if (applicant != null && !applicant.equals(request.getApplicantUuid())) {
            return false;
        }
        return matchesKeyword(request, query);
    }

    /**
     * 关键词比对单号、学籍主键、申请人、变更内容与理由，命中任何一个即算匹配。
     *
     * <p>
     * 变更内容与理由也纳入比对，是因为审核人常常记得「那条申请休学的」而非单号：变更内容里 存的是 {@code status=SUSPENDED} 这样的原文，理由则是学生自己写的自由文本。
     *
     * <p>
     * 指定了搜索字段时只比那一列（例如「按理由搜」能排除掉变更内容里碰巧含同一串字的干扰）。
     *
     * @param request 申请单
     * @param query   过滤条件
     * @return 是否命中；关键词为空视为不过滤
     */
    private static boolean matchesKeyword(StudentModifyRequest request, ModifyRequestQuery query) {
        String keyword = query.getKeyword();
        if (keyword == null || keyword.trim().length() == 0) {
            return true;
        }
        String trimmed = keyword.trim();
        RequestField field = query.getSearchField();
        if (field == null || field == RequestField.ALL) {
            return matchesAnyField(request, trimmed);
        }
        return matchesField(request, field, trimmed);
    }

    /**
     * 关键词比对多条字段。
     *
     * @param request 申请单
     * @param keyword 已去空白的关键词
     * @return 是否命中
     */
    private static boolean matchesAnyField(StudentModifyRequest request, String keyword) {
        if (contains(text(request.getRequestId()), keyword)) {
            return true;
        }
        if (contains(text(request.getProfileId()), keyword)) {
            return true;
        }
        if (contains(request.getApplicantUuid(), keyword)) {
            return true;
        }
        if (contains(request.getChangesJson(), keyword)) {
            return true;
        }
        return contains(request.getReason(), keyword);
    }

    /**
     * 关键词只比对指定字段。
     *
     * <p>
     * 申请单号与学籍主键是编号，比对方式取「包含」而不是相等：审批人常常只记得单号的尾几位。 这两个编号都很短（自增），「包含」不会像长编号那样误伤一大片。
     *
     * @param request 申请单
     * @param field   要比对的字段
     * @param keyword 已去空白的关键词
     * @return 是否命中
     */
    private static boolean matchesField(StudentModifyRequest request, RequestField field,
            String keyword) {
        if (field == RequestField.REQUEST_ID) {
            return contains(text(request.getRequestId()), keyword);
        }
        if (field == RequestField.PROFILE_ID) {
            return contains(text(request.getProfileId()), keyword);
        }
        if (field == RequestField.APPLICANT_UUID) {
            return contains(request.getApplicantUuid(), keyword);
        }
        if (field == RequestField.CHANGES) {
            return contains(request.getChangesJson(), keyword);
        }
        if (field == RequestField.REASON) {
            return contains(request.getReason(), keyword);
        }
        return matchesAnyField(request, keyword);
    }

    /**
     * 判断文本是否包含关键词（忽略大小写）。
     *
     * @param value   待查文本（可为 null）
     * @param keyword 关键词
     * @return 包含返回 true
     */
    private static boolean contains(String value, String keyword) {
        if (value == null || keyword == null) {
            return false;
        }
        return value.toLowerCase().contains(keyword.toLowerCase());
    }

    /**
     * 主键的文本形式。
     *
     * @param id 主键；可为 null
     * @return 文本；null 返回 null（由 {@link #contains} 判否）
     */
    private static String text(Long id) {
        return id == null ? null : id.toString();
    }
}
