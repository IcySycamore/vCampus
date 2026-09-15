package edu.seu.vcampus.server.student;

import edu.seu.vcampus.common.student.dto.StudentQuery;
import edu.seu.vcampus.common.student.entity.StudentProfile;

/**
 * 档案过滤：判断一条档案是否满足查询条件。
 *
 * <p>
 * 从 {@link StudentDaoMemory} 抽出来，一是让内存 DAO 待在 200 行以内，二是这套判断将来接 JDBC
 * 时要被逐条翻译成 SQL 的 WHERE 子句——单独放一处，翻译时能一眼看全所有条件，不容易漏掉某个
 * （漏掉过滤条件的后果是越权数据混进结果集，属于最难发现的一类缺陷）。
 *
 * <p>
 * 各条件之间是「与」的关系；单个条件内部（例如关键字比对多个字段）是「或」。
 */
final class StudentMatcher {

    /** 私有构造器，禁止实例化工具类。 */
    private StudentMatcher() {
    }

    /**
     * 判断档案是否满足查询条件。
     *
     * @param profile 档案
     * @param query 过滤条件；null 表示不过滤（全部通过）
     * @return 是否匹配
     */
    static boolean matches(StudentProfile profile, StudentQuery query) {
        if (query == null) {
            return true;
        }
        if (query.getProfileId() != null
                && !query.getProfileId().equals(profile.getId())) {
            return false;
        }
        if (query.getUserUuid() != null
                && !query.getUserUuid().equals(profile.getUserUuid())) {
            return false;
        }
        if (query.getPersonCategory() != null
                && query.getPersonCategory() != profile.getPersonCategory()) {
            return false;
        }
        if (query.getStatus() != null && query.getStatus() != profile.getStatus()) {
            return false;
        }
        if (query.getField() != null
                && !containsIgnoreCase(profile.getField(), query.getField())) {
            return false;
        }
        return matchesKeyword(profile, query.getKeyword());
    }

    /**
     * 关键字比对方括号内的若干字段，命中任何一个即算匹配。
     *
     * <p>
     * 比对范围包含姓名与学术方向：界面上的搜索框是一个，用户不会先去区分「这是姓名还是专业」，
     * 所以这里把教师的研究方向和学生的专业一并搜——正好也是「按方向找师生」这条需求的落点。
     *
     * @param profile 档案
     * @param keyword 关键字；null 或空白表示不过滤
     * @return 是否命中
     */
    private static boolean matchesKeyword(StudentProfile profile, String keyword) {
        if (keyword == null || keyword.trim().length() == 0) {
            return true;
        }
        String trimmed = keyword.trim();
        String uuid = profile.getUserUuid();
        if (uuid != null && uuid.contains(trimmed)) {
            return true;
        }
        if (containsIgnoreCase(profile.getRealName(), trimmed)) {
            return true;
        }
        if (containsIgnoreCase(profile.getField(), trimmed)) {
            return true;
        }
        return String.valueOf(profile.getJoinYear()).contains(trimmed);
    }

    /**
     * 判断文本是否包含关键字（忽略大小写）。
     *
     * @param text 待查文本（可为 null）
     * @param keyword 关键字
     * @return 包含返回 true
     */
    private static boolean containsIgnoreCase(String text, String keyword) {
        if (text == null || keyword == null) {
            return false;
        }
        return text.toLowerCase().contains(keyword.toLowerCase());
    }
}
