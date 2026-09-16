package edu.seu.vcampus.server.student;

import edu.seu.vcampus.common.student.dto.ModifyRequestQuery;
import edu.seu.vcampus.common.student.entity.ModifyRequestStatus;
import edu.seu.vcampus.common.student.entity.StudentModifyRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 申请单过滤测试：逐条验证「哪种条件该筛掉谁」。
 *
 * <p>
 * 过滤条件是查询的正确性核心——漏掉一条的后果是审核人看不到该看的申请，多放一条的后果是把别人的
 * 申请摊出来，两种都很难在使用中察觉，所以在这里定死。
 */
class ModifyRequestMatcherTest {

    /** 申请单主键 7、目标学籍 42、申请人 uuid-stu、变更内容含 SUSPENDED、理由「申请休学」。 */
    private static final StudentModifyRequest REQUEST = request();

    /**
     * null 条件表示不过滤，全部通过。
     */
    @Test
    void nullQueryPasses() {
        assertTrue(ModifyRequestMatcher.matches(REQUEST, null));
    }

    /**
     * 为 null 的申请单不匹配（不抛异常）。
     */
    @Test
    void nullRequestNeverMatches() {
        assertFalse(ModifyRequestMatcher.matches(null, new ModifyRequestQuery()));
    }

    /**
     * 状态条件只放行状态相同的那一条。
     */
    @Test
    void statusFilters() {
        ModifyRequestQuery query = new ModifyRequestQuery();
        query.setStatus(ModifyRequestStatus.PENDING);

        assertTrue(ModifyRequestMatcher.matches(REQUEST, query));
        assertFalse(ModifyRequestMatcher.matches(REQUEST,
                new ModifyRequestQuery(ModifyRequestStatus.APPROVED)));
    }

    /**
     * 学籍主键条件只放行目标学籍相同的那一条。
     */
    @Test
    void profileIdFilters() {
        ModifyRequestQuery query = new ModifyRequestQuery();
        query.setProfileId(Long.valueOf(42L));

        assertTrue(ModifyRequestMatcher.matches(REQUEST, query));

        query.setProfileId(Long.valueOf(43L));
        assertFalse(ModifyRequestMatcher.matches(REQUEST, query));
    }

    /**
     * 申请人条件只放行本人提交的那一条。
     */
    @Test
    void applicantFilters() {
        ModifyRequestQuery query = new ModifyRequestQuery();
        query.setApplicantUuid("uuid-stu");

        assertTrue(ModifyRequestMatcher.matches(REQUEST, query));

        query.setApplicantUuid("uuid-other");
        assertFalse(ModifyRequestMatcher.matches(REQUEST, query));
    }

    /**
     * 关键词命中单号 / 学籍主键 / 申请人 / 变更内容 / 理由中的任何一个。
     */
    @Test
    void keywordMatchesAnyField() {
        assertTrue(hit("7"), "单号应命中");
        assertTrue(hit("42"), "目标学籍主键应命中");
        assertTrue(hit("uuid-stu"), "申请人应命中");
        assertTrue(hit("SUSPENDED"), "变更内容应命中");
        assertTrue(hit("休学"), "理由应命中");
        assertTrue(hit("sus"), "关键词应忽略大小写");
    }

    /**
     * 关键词命中不到任何字段时被筛掉；空白关键词视为不过滤。
     */
    @Test
    void keywordMissesAndBlanks() {
        assertFalse(hit("不存在的词"));
        assertTrue(hit("   "), "空白关键词应视为不过滤");
        assertTrue(hit(null), "null 关键词应视为不过滤");
    }

    /**
     * 对给定关键词做一次匹配。
     *
     * @param keyword 关键词
     * @return 是否命中
     */
    private static boolean hit(String keyword) {
        ModifyRequestQuery query = new ModifyRequestQuery();
        query.setKeyword(keyword);
        return ModifyRequestMatcher.matches(REQUEST, query);
    }

    /**
     * 造一条用于比对的申请单。
     *
     * @return 申请单
     */
    private static StudentModifyRequest request() {
        StudentModifyRequest request = new StudentModifyRequest(Long.valueOf(42L), "uuid-stu",
                "status=SUSPENDED", "申请休学");
        request.setRequestId(Long.valueOf(7L));
        request.setAppliedAt(1000L);
        return request;
    }
}
