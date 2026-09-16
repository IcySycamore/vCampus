package edu.seu.vcampus.server.student;

import edu.seu.vcampus.common.student.dto.ModifyRequestQuery;
import edu.seu.vcampus.common.student.entity.StudentModifyRequest;

/**
 * 申请单过滤：判断一条申请单是否满足查询条件（207 命令）。
 *
 * <p>
 * 与 {@link StudentMatcher} 同一套做法：内存实现与文件实现共用这一份判断，既避免两边边界
 * 处理出现分歧，也保证将来接 JDBC 时只需要把这里翻译成一条 WHERE 子句。
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
     * @param query 过滤条件；null 表示不过滤（全部通过）
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
        return matchesKeyword(request, query.getKeyword());
    }

    /**
     * 关键词比对单号、学籍主键、申请人、变更内容与理由，命中任何一个即算匹配。
     *
     * <p>
     * 变更内容与理由也纳入比对，是因为审核人常常记得「那条申请休学的」而非单号：变更内容里
     * 存的是 {@code status=SUSPENDED} 这样的原文，理由则是学生自己写的自由文本。
     *
     * @param request 申请单
     * @param keyword 关键词；null 或空白表示不过滤
     * @return 是否命中
     */
    private static boolean matchesKeyword(StudentModifyRequest request, String keyword) {
        if (keyword == null || keyword.trim().length() == 0) {
            return true;
        }
        String trimmed = keyword.trim();
        if (contains(text(request.getRequestId()), trimmed)) {
            return true;
        }
        if (contains(text(request.getProfileId()), trimmed)) {
            return true;
        }
        if (contains(request.getApplicantUuid(), trimmed)) {
            return true;
        }
        if (contains(request.getChangesJson(), trimmed)) {
            return true;
        }
        return contains(request.getReason(), trimmed);
    }

    /**
     * 判断文本是否包含关键词（忽略大小写）。
     *
     * @param value 待查文本（可为 null）
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
