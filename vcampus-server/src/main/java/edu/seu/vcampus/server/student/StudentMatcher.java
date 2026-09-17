package edu.seu.vcampus.server.student;

import edu.seu.vcampus.common.student.dto.StudentQuery;
import edu.seu.vcampus.common.student.entity.StudentField;
import edu.seu.vcampus.common.student.entity.StudentProfile;

import java.util.ArrayList;
import java.util.List;

/**
 * 档案过滤：判断一条档案是否满足查询条件。
 *
 * <p>
 * 单独放一处的第二个理由：这套判断将来接 JDBC 时要被逐条翻译成 SQL 的 WHERE 子句，放一起能一眼看全
 * 所有条件，不容易漏掉某个。
 *
 * <p>
 * <b>调用时机很重要</b>：本类只能作用在<b>已补齐姓名</b>的档案上（见
 * {@link StudentService#listStudents}）。学籍表只存账户 uuid，姓名是查出来时联查补上的，而
 * 「按姓名搜」正是最常用的搜法——过滤若跑在补姓名之前，匹配时 {@code realName} 还是 null，
 * 搜人就一条也搜不到。
 *
 * <p>
 * 各条件之间是「与」的关系；关键词内部（多字段或单字段）是「或」。
 */
final class StudentMatcher {

    /** 私有构造器，禁止实例化工具类。 */
    private StudentMatcher() {
    }

    /**
     * 筛出满足条件的档案。
     *
     * @param profiles 已补齐姓名的全量档案；null 视为空
     * @param query 过滤条件；null 表示不过滤
     * @return 匹配的档案（新列表）
     */
    static List<StudentProfile> filter(List<StudentProfile> profiles, StudentQuery query) {
        List<StudentProfile> matched = new ArrayList<StudentProfile>();
        if (profiles == null) {
            return matched;
        }
        int index = 0;
        while (index < profiles.size()) {
            StudentProfile profile = profiles.get(index);
            if (profile != null && !profile.isDeleted() && matches(profile, query)) {
                matched.add(profile);
            }
            index = index + 1;
        }
        return matched;
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
                && !contains(profile.getField(), query.getField())) {
            return false;
        }
        return matchesKeyword(profile, query);
    }

    /**
     * 按关键字匹配：指定了搜索字段就只比那一列，否则一次比多列。
     *
     * @param profile 档案
     * @param query 查询条件
     * @return 是否命中；关键字为空视为不过滤
     */
    private static boolean matchesKeyword(StudentProfile profile, StudentQuery query) {
        String keyword = query.getKeyword();
        if (keyword == null || keyword.trim().length() == 0) {
            return true;
        }
        String trimmed = keyword.trim();
        StudentField field = query.getSearchField();
        if (field == null || field == StudentField.ALL) {
            return matchesAnyField(profile, trimmed);
        }
        return matchesField(profile, field, trimmed);
    }

    /**
     * 关键字比对方括号内的若干字段，命中任何一个即算匹配。
     *
     * <p>
     * 比对范围包含姓名、学号与学术方向：界面上的搜索框默认是一个，用户不会先去区分「这是姓名
     * 还是专业」；学号同样必须能搜——列表里有「学号」这一列，搜不到会显得它只是个装饰。教师
     * 的研究方向与学生的专业共用 {@code field} 字段，所以一次就能把两类人都捞出来。
     *
     * @param profile 档案
     * @param keyword 已去空白的关键字
     * @return 是否命中
     */
    private static boolean matchesAnyField(StudentProfile profile, String keyword) {
        if (contains(profile.getUserUuid(), keyword)) {
            return true;
        }
        if (contains(profile.getRealName(), keyword)) {
            return true;
        }
        if (contains(profile.getField(), keyword)) {
            return true;
        }
        if (contains(profile.getStudentNo(), keyword)) {
            return true;
        }
        return contains(String.valueOf(profile.getJoinYear()), keyword);
    }

    /**
     * 关键字只比对指定字段。
     *
     * <p>
     * 文本字段一律「包含即命中」而不是相等：用户敲「张」时想要的是一串姓张的人，而不是一个名字
     * 恰好叫「张」的人。主键例外——它是精确编号，用子串比会把 11 也当成 1。
     *
     * <p>
     * 本方法不认识的字段（例如状态、类别——它们不在可搜索集合里）一律回退到多列比对，而不是直接
     * 返回 false：直接返回 false 会变成「搜不到但也不报错」，比放宽更坏。
     *
     * @param profile 档案
     * @param field 要比对的字段
     * @param keyword 已去空白的关键字
     * @return 是否命中
     */
    private static boolean matchesField(StudentProfile profile, StudentField field,
            String keyword) {
        if (field == StudentField.STUDENT_NO) {
            return contains(profile.getStudentNo(), keyword);
        }
        if (field == StudentField.REAL_NAME) {
            return contains(profile.getRealName(), keyword);
        }
        if (field == StudentField.USER_UUID) {
            return contains(profile.getUserUuid(), keyword);
        }
        if (field == StudentField.FIELD) {
            return contains(profile.getField(), keyword);
        }
        if (field == StudentField.JOIN_YEAR) {
            return contains(String.valueOf(profile.getJoinYear()), keyword);
        }
        if (field == StudentField.PROFILE_ID) {
            return profile.getId() != null && profile.getId().toString().equals(keyword);
        }
        return matchesAnyField(profile, keyword);
    }

    /**
     * 判断文本是否包含关键字（忽略大小写）。
     *
     * @param text 待查文本（可为 null）
     * @param keyword 关键字
     * @return 包含返回 true
     */
    private static boolean contains(String text, String keyword) {
        if (text == null || keyword == null) {
            return false;
        }
        return text.toLowerCase().contains(keyword.toLowerCase());
    }
}
